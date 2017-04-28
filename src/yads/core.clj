(ns yads.core
  (:require
    [org.httpkit.server :as server]
    [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
    [ring.middleware.reload :refer [wrap-reload]]
    [clojure.tools.logging :as log]
    [yads.config :as cfg]
    [yads.routes :as routes]
    [yads.handler :as handler])
  (:gen-class))


(defn -main [& args]
  (handler/app-init)
  (log/info "Starting YADS on port" cfg/port)
  (server/run-server
    (if cfg/is-production?
      (wrap-defaults routes/app-routes api-defaults)
      (wrap-reload (wrap-defaults #'routes/app-routes api-defaults)))
    {:port cfg/port}))
