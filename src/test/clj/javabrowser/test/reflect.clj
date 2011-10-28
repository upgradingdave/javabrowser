(ns javabrowser.test.reflect
  (:use [javabrowser.reflect]
        [javabrowser.filesystem]
        [clojure.test]
        [clojure.java.io :only (file)]
        [clojure.string :only (split)]))

(deftest test-finding-jars
  (is (> (count (get-jars)) 0) "Find some jars")
  (is (> (count (get-jars-on-classpath)) 0) "Find jars on classpath")
  (is (> (count (search-jars "clojure")) 0) "Do jar search"))

(deftest test-looking-inside-jar
  (let [junit-jar-name (get-full-dir-path "src/test/resources/junit-4.0.jar")
        junit-jar (file junit-jar-name)
        log4j-jar-name (get-full-dir-path "src/test/resources/log4j-1.2.16.jar")
        log4j-jar (file log4j-jar-name)
        compojure-jar-name (get-full-dir-path "src/test/resources/compojure-0.6.5.jar")
        compojure-jar (file compojure-jar-name)]
    (is (. junit-jar (exists)))
    (is (> (count (get-entries-in-zip junit-jar)) 0) "Able to find some entries inside a jar")
    (is (= (count (get-classes-in-zip junit-jar)) 92) "Find some class files in jar")
    (is (. log4j-jar (exists)))
    (is (= (count (get-classes-in-zip log4j-jar)) 308) "Find some class files in jar")
    (is (= (count (get-classes-in-zips (map file (split log4j-jar-name #",")))) 308) "Find classes files in list with one jar")
    (is (= (count (get-classes-in-zips [junit-jar log4j-jar])) (+ 308 92)) "Find classes in multiple jars")
    (is (= (count (get-classes-in-zips [junit-jar log4j-jar compojure-jar])) (+ 308 92)) "Find classes in multiple jars")
    (is (= (count (search-classes "log4j" (get-classes-in-zips [junit-jar log4j-jar compojure-jar]))) 308) "Filter results by search term")))

(deftest test-methods
  (let [classname "org.apache.log4j.varia.HUPNode"]
    (is (> (count (get-java-methods classname)) 0))))

