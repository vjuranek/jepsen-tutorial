(ns counter.http
  (:gen-class)
  (:require
    [counter.core :as counter]
    [ring.adapter.jetty :as jetty]))

(defn provide-port-name
  []
  (throw (Exception. "You have to provice port number via -port argument and name via -name argument.")))

(def cnt (atom nil))

(defn cnt-val [method]
  (if (= :post method)
    (.incrementAndGet @cnt)
    (.get @cnt)))

(defn handler [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (str "JGroups RAFT counter: cnt = " (cnt-val (:request-method request)))})

(defn start-server [port, name]
  (println "Starting JGroups counter with name")
  (reset! cnt (counter/start-counter counter/raft-config name))
  (println "Starting Jetty on port " port)
  (jetty/run-jetty handler {:port port}))

(defn -main
  [& args]
  (if-not (empty? args)
    (let [[port-arg, port, name-arg, name] args]
      (if (and (= "-port" port-arg) (= "-name" name-arg))
        (start-server (Integer/parseInt port) name)
        (provide-port-name)))
    (provide-port-name))
  (System/exit 0))