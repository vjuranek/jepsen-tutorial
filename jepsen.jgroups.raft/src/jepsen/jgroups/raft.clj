(ns jepsen.jgroups.raft
  (:require [clojure.tools.logging :refer :all]
            [clojure.string :as str]
            [jepsen [cli :as cli]
             [control :as c]
             [db :as db]
             [tests :as tests]]
            [jepsen.control.util :as cu]
            [jepsen.os.debian :as debian])
  (:import
    (org.jgroups JChannel)
    (org.jgroups.blocks.atomic Counter)
    (org.jgroups.raft.blocks CounterService)))


(defn provide-name
  []
  (throw (Exception. "You have to provice node name via -name argument.")))

(def raft-config "raft.xml")

(defn jchannel
  [props, name]
  (-> (JChannel. props) (.name name)))

(defn jcnt
  [jch]
  (-> (CounterService. jch) (.raftId (.name jch)) (.replTimeout 5000) (.getOrCreateCounter "counter" 0)))

(defn join-channel
  [jch]
  (.connect jch "counters"))

(defn start-counter
  [props, name]
  (let [jch (jchannel props name),
        cnt (jcnt jch)]
    (join-channel jch)
    cnt))

(defn counter
  []
  (reify db/DB
         (setup! [_ test node]
                 (info node "Starting JGroups counter")
                 (start-counter raft-config node))

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
  (cli/run! (merge (cli/single-test-cmd {:test-fn counter-test})
                   (cli/serve-cmd))
            args))