(ns javabrowser.test.web
  (:use [javabrowser.web]
        [javabrowser.reflect]
        [javabrowser.filesystem]
        [clojure.test]
        [clojure.string :only (split)]))

(deftest test-build-html
  (let [classname "org.apache.log4j.varia.HUPNode"
        expected1 [:ul {:id "methods"} [:li "public void run()"] [:li "public final void wait()"] [:li "public final native void wait(long)"] [:li "public final void wait(long, int)"] [:li "public boolean equals(class java.lang.Object)"] [:li "public java.lang.String toString()"] [:li "public native int hashCode()"] [:li "public final native java.lang.Class getClass()"] [:li "public final native void notify()"] [:li "public final native void notifyAll()"]]]
    (is (= expected1 (java-methods-html (get-java-methods classname))))))

(deftest page-results
  (let [junit-jar-name (get-full-dir-path "src/test/resources/junit-4.0.jar")
        log4j-jar-name (get-full-dir-path "src/test/resources/log4j-1.2.16.jar")
        compojure-jar-name (get-full-dir-path "src/test/resources/compojure-0.6.5.jar")]
    (is (=
         (count (get-class-results
                 (apply str
                        (interpose
                         ","
                         [junit-jar-name log4j-jar-name compojure-jar-name]))
                 ))
         20) "Get first 20 results by default")))
