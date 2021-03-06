(defproject vadyalex/yads "2.1.1"

  :description "Dynamic DNS micro service connected to Yandex DNS API"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/core.async "0.3.442"]
                 [compojure "1.5.0"]
                 [ring-server "0.4.0"]
                 [ring/ring-defaults "0.2.0"]
                 [http-kit "2.2.0"]
                 [clj-http "3.1.0"]
                 [environ "1.1.0"]
                 [ch.qos.logback/logback-classic "1.1.7"]
                 [overtone/at-at "1.2.0"]]

  :uberjar-name "yads.jar"
  :main yads.core

  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[alembic "0.3.2"]]}
             :production {:env {:production true}}}

  :repl-options {:nrepl-middleware [lighttable.nrepl.handler/lighttable-ops]
                 :port 7000}

  :plugins [[lein-environ "1.1.0"]]

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "--no-sign"] ; disable signing
                  ["uberjar"]
                  ;["deploy"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]])
