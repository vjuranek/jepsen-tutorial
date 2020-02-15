(ns counter.core
  (:gen-class)
  (:import
    (org.jgroups JChannel)
    (org.jgroups.blocks.atomic Counter)
    (org.jgroups.raft.blocks CounterService)))

(def raft-config "raft.xml")

(defn jchannel
  [props, name]
  (-> (JChannel. props) (.name name)))

(defn counter
  [jch]
  (-> (CounterService. jch) (.raftId (.name jch)) (.replTimeout 5000) (.getOrCreateCounter "counter" 0)))

(defn join-channel
  [jch]
  (.connect jch "counters"))

(defn start-counter
  [props, name]
  (let [jch (jchannel props name),
        cnt (counter jch)]
    (join-channel jch)
    cnt))
