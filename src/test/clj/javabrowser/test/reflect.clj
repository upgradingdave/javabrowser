(ns javabrowser.test.reflect
  (:use [javabrowser.reflect]
        [javabrowser.filesystem]
        [clojure.test]
        [clojure.java.io :only (file)]
        [clojure.string :only (split)]))

(deftest test-finding-jars
  (is (> (count (get-jars)) 0) "Find some jars")
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

(deftest test-constructors
  (let [classname "org.apache.log4j.varia.HUPNode"]
    (is (> (count (get-java-constructors classname)) 0))))

(deftest test-class-meta
  (let [classname "java.util.HashMap"
        short-name (fqn-to-short-class-name classname)
        package (fqn-to-package-name classname)
        modifiers (get-class-modifiers classname)
        type (get-class-type-params classname)
        interfaces (get-class-interfaces classname)]
    (is (= "public HashMap implements java.util.Map<K, V>, interface java.lang.Cloneable, interface java.io.Serializable"
           (str package modifiers " " short-name " implements "
                (apply str (interpose ", " (map #(format "%s" %)
                                                interfaces))))) "Basic Class info")))

