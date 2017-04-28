(ns yads.config
  (:require
    [environ.core :refer [env]]))

(def yandex-domain
  (env :yandex-domain))

(def yandex-token
  (env :yandex-token))

(def yads-api-key
  (env :yads-api-key))

(def port
  (Integer. (or (env :port) 5000)))

;; 900 000 milliseconds = 15 minutes
(def updates-period 900000)
