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
  (let [junit-jar-name (get-full-dir-path "src/test/resources/junit-4.0.jar")
        junit-jar (file junit-jar-name)
        log4j-jar-name (get-full-dir-path "src/test/resources/log4j-1.2.16.jar")
        log4j-jar (file log4j-jar-name)]
    (is (. junit-jar (exists)))
    (is (> (count (get-entries-in-zip junit-jar)) 0) "Able to find some entries inside a jar")
    (is (= (count (get-classes-in-zip junit-jar)) 92) "Find some class files in jar")
    (is (. log4j-jar (exists)))
    (is (= (count (get-classes-in-zip log4j-jar)) 308) "Find some class files in jar")
    (is (= (count (get-classes-in-zips [junit-jar log4j-jar])) (+ 308 92)))))

(deftest test-methods
  (let [classname "org.apache.log4j.varia.HUPNode"]
    (is (> (count (get-java-methods classname)) 0))))




