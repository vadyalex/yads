(ns yads.yandex
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
    [yads.config :as cfg]))


(defn yandex-get-records
  "Get DNS records from Yandex DNS service"
  []
  (if (and cfg/yandex-domain cfg/yandex-token)
    (let [response-xml (-> "https://pddimp.yandex.ru/nsapi/get_domain_records.xml?token=%s&domain=%s"
                           (format cfg/yandex-token cfg/yandex-domain)
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
           (into {})))
    {}))


(defn yandex-update-record
  "Update DNS record in the Yandex DNS service"
  [subdomain id ip]
  (let
    [template "https://pddimp.yandex.ru/nsapi/edit_a_record.xml?token=%S&domain=%s&subdomain=%s&record_id=%s&content=%s&ttl=300"
     url (format template cfg/yandex-token cfg/yandex-domain subdomain id ip)]
    (try
      (client/get url)
      (catch Exception e (log/error e "Error requesting Yandex API")))))
