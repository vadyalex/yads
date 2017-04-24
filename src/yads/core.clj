(ns yads.core
  (:require
    [ring.adapter.jetty :as jetty]
    [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
    [clojure.tools.logging :as log]
    [yads.config :as cfg]
    [yads.routes :as routes]
    [yads.handler :as handler])
  (:gen-class))


(defn -main [& args]
  (handler/app-init)
  (log/info "Starting YADS on port" cfg/port)
  (jetty/run-jetty
    (wrap-defaults routes/app-routes api-defaults)
    {:port cfg/port
     :join? false}))
