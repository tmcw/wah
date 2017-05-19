(defproject wah "0.1.0-SNAPSHOT"
  :description "Convenience syntax for WAST"
  :url "http://example.com/FIXME"
  :test-paths ["src"]
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojure/tools.reader "1.0.0-beta4"]
                 [org.clojure/clojure "1.8.0"]]
  :plugins [[lein-cljfmt "0.5.6"]]
  :main ^:skip-aot wah.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
