(ns jepsen.jgroups.raft
  (:require [clojure.tools.logging :refer :all]
            [clojure.string :as str]
            [jepsen [cli :as cli]
             [control :as c]
             [db :as db]
             [tests :as tests]]
            [jepsen.control.util :as cu]
            [jepsen.os.debian :as debian]))

(defn counter
  []
  (reify db/DB
         (setup! [_ test node]
                 (info node "installing jgroups counter"))

         (teardown! [_ test node]
                    (info node "tearing down jgroups counter"))))

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