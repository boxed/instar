(defproject instar "1.0.10"
  :description "Simpler and more powerful assoc/dissoc/update-in"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2371"]]
  :jar-exclusions [#"\.cljx|\.swp|\.swo|\.DS_Store"]
  :profiles {:dev {:dependencies [[midje "1.6.3"]]}}
  :plugins [[com.keminglabs/cljx "0.4.0" :exclusions [org.clojure/clojure]]
            [lein-cljsbuild "1.0.4-SNAPSHOT"]
            [lein-midje "3.0.0"]
            [midje-readme "1.0.5"]
            [lein-pdo "0.1.1"]
            [com.cemerick/clojurescript.test "0.3.1"]]
  :uberjar-name "instar.jar"

  :license {:name "MIT"
            :url "http://opensource.org/licenses/MIT"}
  :url "https://github.com/boxed/instar"
  :scm {:name "git"
        :url "https://github.com/boxed/instar"}
  :deploy-repositories [["clojars" {:creds :gpg}] ["releases" :clojars]]

  :hooks [cljx.hooks]

  :source-paths ["src/clj" "src/cljs" "target/classes"]

  ; - clsj config -
  :cljsbuild
  {:builds [{:id "client"
             :source-paths ["target/classes"]
             :compiler {:output-dir "target/client"
                        :output-to "target/client.js"
                        ;;:source-map "target/client.js.map"
                        :pretty-print true}}
            {:id "test"
             :source-paths ["target/classes" "test"]
             :notify-command ["phantomjs" :cljs.test/runner "target/testable.js"]
             :compiler {:output-to "target/testable.js"
                        :output-dir "target/out-test"
                        :optimizations :whitespace
                        :pretty-print true}}]}

  ; - cljx config -
  :cljx {:builds [{:source-paths ["src/cljx" "src/clj"]
                   :output-path "target/classes"
                   :rules :clj}
                  {:source-paths ["src/cljx"]
                   :output-path "target/classes"
                   :rules :cljs}]})
