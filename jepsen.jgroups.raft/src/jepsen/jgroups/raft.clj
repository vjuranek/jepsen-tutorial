(ns jepsen.jgroups.raft
  (:require [clojure.tools.logging :refer :all]
            [clj-http.client :as http]
            [clojure.string :as str]
            [clojure.java.shell :refer [sh]]
            [clojure.java.io :as io]
            [jepsen
             [checker :as checker]
             [cli :as cli]
             [client :as client]
             [control :as c]
             [core :as core]
             [db :as db]
             [generator :as gen]
             [tests :as tests]]
            [jepsen.checker.timeline :as timeline]
            [jepsen.control.util :as cu]
            [jepsen.os.debian :as debian]))

(defn parse-long
  "Parses a string to a Long. Passes through `nil`."
  [s]
  (when s (Long/parseLong s)))

(def counter-src "counter")
(def counter-jar (str counter-src "/target/counter-0.1.0-SNAPSHOT-standalone.jar"))
(def test-dir "/opt/counter")
(def test-jar (str test-dir "/counter.jar"))
(def pid-file (str test-dir "/counter.pid"))
(def log-file (str test-dir "/counter.log"))
(def persistent-log (str test-dir "/persistent.log"))


(defn upload!
  "Upload counter jar on nodes."
  []
  (c/exec :mkdir :-p test-dir)
  (c/cd test-dir
        (c/upload (.getCanonicalPath (io/file counter-jar)) test-jar)))

(defn build-counter!
  "Ensures the server jar is ready"
  [test node]
  (when (= node (core/primary test))
    (when-not (.exists (io/file counter-jar))
      (info "Building counter")
      (let [{:keys [exit out err]} (sh "lein" "uberjar" :dir counter-src)]
        (info out)
        (info err)
        (info exit)
        (assert (zero? exit))))))

(defn start-counter!
  [node]
  (c/cd test-dir
        (cu/start-daemon!
         {:chdir   test-dir
          :logfile log-file
          :pidfile pid-file}
         "/usr/bin/java"
         :-jar   test-jar
         :-port 3000
         :-name node)))

(defn stop-counter!
  []
  (c/cd test-dir
        (c/su
         (cu/stop-daemon! pid-file))))

(defn counter
  []
  (reify
   db/DB
   (setup! [_ test node]
           (build-counter! test node)
           (core/synchronize test) ;; ensure build is finished
           (debian/install-jdk11!)
           (upload!)
           (core/synchronize test) ;; ensure build is on all nodes
           (info node "Starting JGroups counter")
           (start-counter! node)
           (Thread/sleep 10000) ;; TODO: figure out better way how to ensure cluster is formed.
           (core/synchronize test))

   (teardown! [counter test node]
              ;; TODO: stop jch here
              (info node "Stopping JGroups counter - no-op for now")
              (stop-counter!)
              (c/su
               (c/exec :rm :-rf log-file pid-file persistent-log)))))

(defn client-get   [_ _] {:type :invoke, :f :read})
(defn client-increment   [_ _] {:type :invoke, :f :add})

(defn client-read [node]
  (parse-long(:body (http/get (str "http://" node ":3000")))))

(defn client-add [node]
  (http/post (str "http://" node ":3000"))
  1) ;; Increment amount, always 1.

(defn counter-client
  [node]
  (reify client/Client
        (open! [client test node]
               (counter-client node))

        (setup! [client test])

        (teardown! [client test])

        (close! [client test])

        (invoke! [client test op]
                 (case (:f op)
                   :read (assoc op :type :ok, :value (client-read node))
                   :add (assoc op :type :ok, :value (client-add node))))))

(defn counter-test
  [opts]
  (merge tests/noop-test
         opts
         {:name "jgroups counter"
          :os debian/os
          :db (counter)
          :client (counter-client nil)
          :checker (checker/compose
                    {:counter (checker/counter)
                     :timeline (timeline/html)})
          :generator (->> (gen/mix [client-get client-increment])
                          (gen/stagger 1)
                          (gen/nemesis nil)
                          (gen/time-limit 15))}))

(defn -main
  "Handles command line arguments. Can either run a test, or a web server for
  browsing results."
  [& args]
  (cli/run!
   (merge (cli/single-test-cmd {:test-fn counter-test})
          (cli/serve-cmd))
   args))