(defproject instar "1.0.0"
  :description "transform nested data structures easily"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2173"]]
  :jar-exclusions [#"\.cljx|\.swp|\.swo|\.DS_Store"]
  :source-paths ["src/cljx" "src/clj" "src/cljs"]
  :test-paths ["target/test-classes"]
  :profiles {:dev {:dependencies [[midje "1.5.0"]]}}
  :plugins [[com.keminglabs/cljx "0.4.0"]
            [lein-cljsbuild "1.0.2"]]
  :uberjar-name "instar.jar"

  :license "MIT"
  :url "https://github.com/boxed/instar"

  :hooks [cljx.hooks
          leiningen.cljsbuild]

  ; - clsj config -
  :cljsbuild {:builds {:client {:source-paths ["target/classes"]
                                :compiler {:output-dir "target/client"
                                           :output-to "target/client.js"
                                           ;:source-map "target/client.js.map"
                                           :pretty-print true}}}}

  ; - cljx config -
  :hooks []

  :cljx {:builds [{:source-paths ["src/cljx" "src/clj"]
                   :output-path "target/classes"
                   :rules :clj}
                  {:source-paths ["src/cljx" "src/cljs"]
                   :output-path "target/classes"
                   :rules :cljs}

                  {:source-paths ["test/cljx" "src/clj"]
                   :output-path "target/test-classes"
                   :rules :clj}
                  {:source-paths ["test/cljx" "src/cljs"]
                   :output-path "target/test-classes"
                   :rules :cljs}]})
