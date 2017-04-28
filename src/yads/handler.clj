(ns yads.handler
  (:require
    [environ.core :refer [env]]
    [clojure.string :as strings]
    [clojure.data.xml :as xml]
    [clojure.data.json :as json]
    [clojure.tools.logging :as log]
    [clojure.core.async :refer [>! <! >!! <!! go chan buffer close! thread alts! alts!! timeout]]
    [clojure.java.io :as io]
    [compojure.core :refer :all]
    [ring.adapter.jetty :as jetty]
    [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
    [clj-http.client :as client]
    [overtone.at-at :as at-at]
    [yads.config :as cfg]
    [yads.yandex :as yndx]))

(defn bad-request
  "Return 400 response with optional body"
  ([]
   bad-request "")
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

(def accepted
  {:status  202
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

(def nothing-to-do-here
  (-> "nothing-to-do-here.txt"
      (io/resource)
      (slurp)))

(def not-found
  {:status  404
   :headers {"Content-Type" "text/plain; charset=utf-8"}
   :body nothing-to-do-here})


;; records map
(defonce records (atom {}))

;; thread pool to execute periodic records fetch
(defonce tasks (at-at/mk-pool))


(defn app-init
  "Initializing.."
  []
  (log/info "Initializing YADS..")
  (log/info "YANDEX_DOMAIN" cfg/yandex-domain)
  (log/info "YANDEX_TOKEN" cfg/yandex-token)
  (log/info "Schedule periodicaly get records from Yandex..")
  (at-at/interspaced cfg/updates-period #(swap! records merge (yndx/yandex-get-records)) tasks))

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
          (do
            (go
              (if (yndx/yandex-update-record subdomain ysubdomainid new-ip)
                (swap! records assoc-in [subdomain :ip] new-ip)))
            accepted))))))

(defn records-status
  "Return status of all subdomain records"
  []
  (let
    [result (->> @records
                 (seq)
                 (map #(hash-map :domain (first %) :ip (:ip (second %))))
                 (sort #(compare (:domain %1) (:domain %2)))
                 (reduce conj []))]
    (if (empty? result)
      no-content
      (json-response (json/write-str result)))))
