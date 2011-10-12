(ns javabrowser.test.reflect
  (:use [javabrowser.reflect]
        [javabrowser.filesystem]
        [clojure.test]
        [clojure.java.io :only (file)]))

(deftest test-finding-jars
  (is (> (count (get-jars)) 0) "Find some jars")
  (is (> (count (get-jars-on-classpath)) 0) "Find jars on classpath")
  (is (> (count (search-jars "clojure")) 0) "Do jar search"))

(deftest test-looking-inside-jar
  (let [test-jar-name (get-full-dir-path "src/test/resources/junit-4.0.jar")
        test-jar (file test-jar-name)]
    (is (. test-jar (exists)))
    (is (> (count (get-entries-in-zip test-jar)) 0) "Able to find some entries inside a jar")
    (is (> (count (get-classes-in-zip test-jar)) 0) "Find some class files in jar")))


