(defproject jepsen.jgroups.raft "0.1.0-SNAPSHOT"
  :description "Jepsen test for JGroups Raft"
  :url "http://example.com/FIXME"
  :license {:name "MIT"}
  :main jepsen.jgroups.raft
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [jepsen "0.1.15"]
                 [org.jgroups/jgroups-raft "0.5.0.Final"]])
