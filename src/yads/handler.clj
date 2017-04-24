(ns yads.handler
  (:require
    [environ.core :refer [env]]
    [clojure.string :as strings]
    [clojure.data.xml :as xml]
    [compojure.core :refer :all]
    [ring.adapter.jetty :as jetty]
    [ring.util.response :as responses]
    [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
    [clj-http.client :as client]
    [clojure.data.json :as json]
    [clojure.tools.logging :as log]
    [yads.config :as cfg]
    [yads.yandex :as yndx]))

(defn bad-request
  "Return 400 response with optional body"
  ([]
   {:status  400
    :headers {"Content-Type" "text/plain; charset=utf-8"}})
  ([body]
   {:status  400
    :body    body
    :headers {"Content-Type" "text/plain; charset=utf-8"}}))

(def internal-server-error
  {:status  500
   :headers {"Content-Type" "text/plain; charset=utf-8"}})

(def ok
  {:status  200
   :headers {"Content-Type" "text/plain; charset=utf-8"}})

(defn json-response
  "Return 200 response with 'application/json' and supplied body"
  [body]
  {:status  200
   :body    body
   :headers {"Content-Type" "application/json; charset=utf-8"}})

(def not-modified
  {:status  304
   :headers {"Content-Type" "text/plain; charset=utf-8"}})

(def no-content
  {:status  204
   :headers {"Content-Type" "text/plain; charset=utf-8"}})

(def not-found
  {:status  404
   :headers {"Content-Type" "text/plain; charset=utf-8"}
   :body ""})

(defonce records (atom {}))

(defn app-init
  "Initializing.."
  []
  (log/info "Initializing YADS..")
  (log/info "YANDEX_DOMAIN" cfg/yandex-domain)
  (log/info "YANDEX_TOKEN" cfg/yandex-token)
  (if (and cfg/yandex-domain cfg/yandex-token)
    (do
      (log/info "Get records from Yandex..")
      (swap! records merge (yndx/yandex-get-records))))
  (log/info "RECORDS" "->" @records)
  (log/info "YADS_API_KEY" cfg/yads-api-key))


(defn record-status
  "Return status of the subdomain record"
  [subdomain]
  (let
    [record (get @records subdomain)
     result (dissoc record :ysubdomainid)]
    (if record
      (json-response (json/write-str result))
      no-content)))

(defn record-update
  "Update status for subdomain record :id"
  [subdomain ip client-ip key]
  (let [new-ip (or ip client-ip)]
    (if (or (some strings/blank? [cfg/yads-api-key subdomain new-ip key]) (not (.equalsIgnoreCase cfg/yads-api-key key)))
      (bad-request)
      (let
        [record (get @records subdomain)
         ysubdomainid (get record :ysubdomainid)
         current-ip (get record :ip)
         same-ip? (.equalsIgnoreCase current-ip new-ip)]
        (if same-ip?
          not-modified
          (if (yndx/yandex-update-record subdomain ysubdomainid new-ip)
            (do
              (swap! records assoc-in [subdomain :ip] new-ip)
              ok)
            internal-server-error))))))
