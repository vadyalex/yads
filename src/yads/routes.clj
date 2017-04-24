(ns yads.routes
  (:require
    [compojure.core :refer :all]
    [yads.handler :as handler]))


(defroutes app-routes
  (context "/record/:domain" [domain]
           (GET "/"
                []
                (handler/record-status domain))
           (GET "/update"
                [ip key :as {headers :headers client-ip :remote-addr}]
                (handler/record-update domain ip (or (get headers "x-forwarded-for") client-ip) key)))
  (ANY "*"
       []
       handler/not-found))

