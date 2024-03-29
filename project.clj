(defproject com.upgradingdave/javabrowser "0.0.3"
  :description "Web app to browse java code"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/tools.logging "0.1.2"]
                 [org.clojure/data.json "0.1.0"]
                 [ring/ring-jetty-adapter "0.3.11"
                  :exclusions [org.clojure/clojure
                               org.clojure/clojure-contrib]]
                 [ring-json-params "0.1.3"]
                 [clj-json "0.4.0"]
                 [compojure "0.6.5" :exclusions [org.clojure/clojure]]
                 [hiccup "0.3.6" :exclusions [org.clojure/clojure]]
                 [org.clojure.contrib/zip-filter "1.3.0-SNAPSHOT"]
                 [log4j "1.2.16" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]]
  :dev-dependencies [[lein-ring "0.4.5"]]
  :source-path "src/main/clj"
  :test-path "src/test/clj"
  :extra-classpath-dirs ["src/main/cljs" "/Users/dparoulek/src/clojure/clojurescript/src/clj" "/Users/dparoulek/src/clojure/clojurescript/src/cljs" "/Users/dparoulek/src/clojure/clojurescript/lib/goog.jar" "/Users/dparoulek/src/clojure/clojurescript/lib/compiler.jar"]
  :resources-path "src/main/resources"
  :war-resources-path "resources/public"
  :dev-resources-path "src/test/resources"
  :ring {:handler javabrowser.web/app})
