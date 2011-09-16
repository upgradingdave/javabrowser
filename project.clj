(defproject com.upgradingdave/javabrowser "0.0.1"
  :description "Web app to browse java classes avialable on classpath"
  :dependencies [[org.clojure/clojure "1.3.0-beta1"]
                 [org.clojure/tools.logging "0.1.2"]
                 [org.clojure/data.json "0.1.0"]
                 [ring/ring-jetty-adapter "0.3.11"
                  :exclusions [org.clojure/clojure
                               org.clojure/clojure-contrib]]
                 [ring-json-params "0.1.3"]
                 [clj-json "0.4.0"]
                 [compojure "0.6.5" :exclusions [org.clojure/clojure]]
                 [hiccup "0.3.6" :exclusions [org.clojure/clojure]]
                 [log4j "1.2.16" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]
                 [swank-clojure "1.3.0" :exclusions [org.clojure/clojure]]
                 [jline "0.9.94"]]
  :dev-dependencies [[lein-ring "0.4.5"]]
  :ring {:handler javabrowser.core/app}
  )
