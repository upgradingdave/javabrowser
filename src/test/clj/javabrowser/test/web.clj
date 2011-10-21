(ns javabrowser.test.web
  (:use [javabrowser.web]
        [javabrowser.reflect]
        [clojure.test]))

(deftest test-build-html
  (let [classname "org.apache.log4j.varia.HUPNode"
        expected1 [:ul {:id "methods"} [:li "public void run()"] [:li "public final void wait()"] [:li "public final native void wait(long)"] [:li "public final void wait(long, int)"] [:li "public boolean equals(class java.lang.Object)"] [:li "public java.lang.String toString()"] [:li "public native int hashCode()"] [:li "public final native java.lang.Class getClass()"] [:li "public final native void notify()"] [:li "public final native void notifyAll()"]]]
    (is (= expected1 (java-methods-html (get-java-methods classname))))))
