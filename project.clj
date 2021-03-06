(defproject instar "1.0.11-SNAPSHOT"
  :description "Simpler and more powerful assoc/dissoc/update-in"
  :dependencies [[org.clojure/clojure "1.7.0" :scope "provided"]]
  :jar-exclusions [#"\.cljx|\.swp|\.swo|\.DS_Store"]
  :profiles {:dev {:dependencies [[midje "1.7.0"]
                                  [org.clojure/clojurescript "1.7.28"]]}}
  :plugins [[com.keminglabs/cljx "0.6.0" :exclusions [org.clojure/clojure]]
            [lein-cljsbuild "1.0.6"]
            [lein-midje "3.1.3"]
            [midje-readme "1.0.8"]
            [lein-pdo "0.1.1"]
            [com.cemerick/clojurescript.test "0.3.3"]]
  :uberjar-name "instar.jar"

  :license {:name "MIT"
            :url "http://opensource.org/licenses/MIT"}
  :url "https://github.com/boxed/instar"
  :scm {:name "git"
        :url "https://github.com/boxed/instar"}
  :deploy-repositories [["clojars" {:creds :gpg}] ["releases" :clojars]]

  :prep-tasks [["cljx" "once"]]

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
