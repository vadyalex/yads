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
