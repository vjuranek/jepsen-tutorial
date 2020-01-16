(ns jepsen.jgroups.raft
  (:require [clojure.tools.logging :refer :all]
            [clojure.string :as str]
            [clojure.java.shell :refer [sh]]
            [clojure.java.io :as io]
            [jepsen
             [cli :as cli]
             [control :as c]
             [core :as core]
             [db :as db]
             [tests :as tests]]
            [jepsen.control.util :as cu]
            [jepsen.os.debian :as debian]))

(def counter-src "counter")
(def counter-jar (str counter-src "/target/counter-0.1.0-SNAPSHOT-standalone.jar"))
(def test-dir "/opt/counter")
(def test-jar (str test-dir "counter.jar"))

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

(defn counter
  []
  (reify
    db/DB
    (setup! [_ test node]
            (build-counter! test node)
            (debian/install-jdk11!)
            (upload!)
            (jepsen/synchronize test) ;; ensure build is on all nodes
            (info node "Starting JGroups counter")
            ;(start-counter raft-config node))

    (teardown! [_ test node]
               ;; TODO: stop jch here
               (info node "Stopping JGroups counter - no-op for now"))))

(defn counter-test
  [opts]
  (merge tests/noop-test
         opts
         {:name "jgroups counter"
          :os   debian/os
          :db   (counter)}))

(defn -main
  "Handles command line arguments. Can either run a test, or a web server for
  browsing results."
  [& args]
  (cli/run!
    (merge (cli/single-test-cmd {:test-fn counter-test})
           (cli/serve-cmd))
    args))