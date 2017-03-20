(defproject vadyalex/yads "1.0.0"

  :description "Dynamic DNS micro service connected to Yandex DNS API"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [compojure "1.5.0"]
                 [ring/ring-defaults "0.2.0"]
                 [ring-server "0.4.0"]
                 [clj-http "3.1.0"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/tools.logging "0.3.1"]
                 [environ "1.1.0"]]

  :profiles {:dev {:dependencies [[alembic "0.3.2"]
                                  [ch.qos.logback/logback-classic "1.1.7"]]}}

  :repl-options {:nrepl-middleware [lighttable.nrepl.handler/lighttable-ops]}

  :plugins [[lein-ring "0.8.10"]
            [lein-environ "1.1.0"]]

  :ring {:handler yads.core/service
         :init    yads.core/service-init
         :destroy yads.core/service-destroy}


  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "--no-sign"] ; disable signing
                  ["ring" "uberwar"]
                  ;["deploy"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]])
