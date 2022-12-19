(defproject clj-htmx-playground "0.1.0-SNAPSHOT"
  :description "A simple set of examples demonstrating htmx"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[clojure-csv/clojure-csv "2.0.2"]
                 [com.github.seancorfield/honeysql "2.4.947"]
                 [com.github.seancorfield/next.jdbc "1.3.847"]
                 [commons-io/commons-io "2.4"]
                 [datascript-transit/datascript-transit "0.3.0"]
                 [datascript/datascript "1.3.15"]
                 [funcool/cuerdas "2.2.1"]
                 [hiccup/hiccup "1.0.5"]
                 [info.sunng/ring-jetty9-adapter "0.18.1"]
                 [integrant/integrant "0.8.0"]
                 [metosin/jsonista "0.3.6"]
                 [metosin/reitit "0.5.18"]
                 [metosin/ring-http-response "0.9.3"]
                 [org.clojure/clojure "1.11.1"]
                 [org.clojure/tools.logging "1.2.4"]
                 [ch.qos.logback/logback-classic "1.4.5"]
                 [org.xerial/sqlite-jdbc "3.40.0.0"]
                 [ring/ring "1.9.6"]
                 [ring/ring-defaults "0.3.4"]
                 [ring/ring-json "0.5.1"]]
  :repl-options {:init-ns clj-htmx-playground.system})
