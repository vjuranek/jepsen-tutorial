(defproject counter "0.1.0-SNAPSHOT"
  :description "jgroups raft counter over http"
  :main counter.http
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.jgroups/jgroups-raft "0.5.0.Final"]
                 [ring/ring-core "1.8.0"]
                 [ring/ring-jetty-adapter "1.8.0"]])
