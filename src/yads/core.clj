(ns yads.core
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
    [clojure.tools.logging :as log])
  (:gen-class))

(def yandex-domain
  (env :yandex-domain))

(def yandex-token
  (env :yandex-token))

(def yads-api-key
  (env :yads-api-key))

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


(defn yandex-get-records
  "Get DNS records from Yandex DNS service"
  []
  (let
  [response-xml (-> "https://pddimp.yandex.ru/nsapi/get_domain_records.xml?token=%s&domain=%s"
                    (format yandex-token yandex-domain)
                    (client/get)
                    :body
                    xml/parse-str)]
    (->> response-xml
         xml-seq
         (filter #(= :record (:tag %)))
         (filter #(.equalsIgnoreCase "A" (get-in % [:attrs :type])))
         (map #(let [record-tag  %
                     domain  (get-in record-tag [:attrs :domain])
                     id      (get-in record-tag [:attrs :id])
                     content (get-in record-tag [:content] "0.0.0.0")
                     ip      (-> (apply str content)
                                 (.replaceAll "\\)" "")
                                 (.replaceAll "\\(" ""))]
                 {domain {:ysubdomainid id
                          :ip ip}}))
         (into {}))))


(defn yandex-update-record
  "Update DNS record in the Yandex DNS service"
  [subdomain id ip]
  (let
    [template "https://pddimp.yandex.ru/nsapi/edit_a_record.xml?token=%S&domain=%s&subdomain=%s&record_id=%s&content=%s&ttl=300"
     url (format template yandex-token yandex-domain subdomain id ip)]
    (try
      (client/get url)
      (catch Exception e (log/error e "Error requesting Yandex API"))))
  )

(defonce records (atom {}))

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
    (if (or (some strings/blank? [yads-api-key subdomain new-ip key]) (not (.equalsIgnoreCase yads-api-key key)))
      (bad-request)
      (let
        [record (get @records subdomain)
         ysubdomainid (get record :ysubdomainid)
         current-ip (get record :ip)
         same-ip? (.equalsIgnoreCase current-ip new-ip)]
        (if same-ip?
          not-modified
          (if (yandex-update-record subdomain ysubdomainid new-ip)
            (do
              (swap! records assoc-in [subdomain :ip] new-ip)
              ok)
            internal-server-error))))))

(defroutes service-routes
  (context "/record/:subdomain" [subdomain]
           (GET "/" []
                (record-status subdomain))
           (GET "/update" [ip key :as {headers :headers client-ip :remote-addr}]
                (record-update subdomain ip (or (get headers "x-forwarded-for") client-ip) key)))
  (ANY "*" [] not-found))

(def port
  (Integer. (or (env :port) 5000)))

(defn service-init
  "Initializing.."
  []
  (log/info "Initializing YADS..")
  (log/info "YANDEX_DOMAIN" yandex-domain)
  (log/info "YANDEX_TOKEN" yandex-token)
  (if (and yandex-domain yandex-token)
    (do
      (log/info "Get records from Yandex..")
      (swap! records merge (yandex-get-records))))
  (log/info "RECORDS" "->" @records)
  (log/info "YADS_API_KEY" yads-api-key)
  (log/info "Starting YADS on port" port))

(defn -main [& args]
  (service-init)
  (jetty/run-jetty (wrap-defaults service-routes api-defaults) {:port port :join? false}))
