(ns javabrowser.reflect
  (:use [hiccup.core :only (html)]
        [hiccup.page-helpers :only (include-css include-js)]
        [javabrowser.maven])
  (:require [clojure.string :as str])
  (:import (java.lang.reflect Modifier)
           (java.lang.reflect TypeVariable)))

(defn get-jars-on-classpath
  "Gets a list of abs paths to jars on the classpath"
  []
  (filter 
   #(re-matches #".*\.jar" %)
   (concat (str/split (. System (getProperty "sun.boot.class.path")) #":")
           (str/split (. System (getProperty "java.class.path")) #":"))))

(defn get-jars
  "Tries to find all jars in this project"
  []
  (if (mavenized?)
    (get-dependencies)
    (get-jars-on-classpath)))


(defn get-entries-in-zip
  [fileName]
  "Get list of all entries inside jar file. TODO: when I tried to use
map, it complains that z is closed"
  (try
    (with-open [z (java.util.zip.ZipFile. fileName)]
      (loop [e (enumeration-seq (.entries z))
             result '()]
        (if (empty? e)
          result
          (recur (rest e) (conj result (.getName (first e)))))))
    (catch Exception e ())))

(defn get-classes-in-zip
  "Get list of class files inside jar file."
  [fileName]
  (filter #(re-matches #".*\.class" %) (get-entries-in-zip fileName)))

(defn get-classes-in-zips
  "Returns a list of all classes in collection of zip files"
  [coll]
  (reduce into (map get-classes-in-zip coll)))

(defn search-classes
   "Filters a list COLL of fully qualified class names by search-term"
   [search-term coll]
   (filter #(re-seq (re-pattern (str "(?i)" search-term)) %) coll))

(defn path-to-class-name
  [filepath]
  "Convert path like 'com/apple/java/AppleSystemLog.class' into fully
qualified java class name"
  (str/replace (nth (re-matches #"(.*)\.class" filepath) 1) "/" "."))

(defn fqn-to-short-class-name
  "Grab class name off of fqn"
  [fqn]
  (apply str (drop (+ 1 (. fqn (lastIndexOf "."))) fqn)))

(defn fqn-to-package-name
  "Grab package from fqn"
  [fqn]
  (apply str (take (. fqn (lastIndexOf ".")) fqn)))

;; Deprecated in favor of using maven pom.xml
;; (defn find-classes
;;   "TODO: WIP. Attempts to find names of all classes on classpath. First, find names of classes in all the jar files on the classpath"
;;   []
;;   (let [class-files (mapcat #(get-classes-in-zip %) (get-jars))]
;;     (map #(path-to-class-name %) class-files)
;;     ))

;; Now handled in web.clj
;;(defstruct search-results :total :offset :max :results)

(defn search-jars
  [search-term & [offset max]]
  (let [offset (or offset 0)
        max (or max 20)
        all-jars (filter #(re-seq (re-pattern (str "(?i)" search-term)) %) (get-jars))
        total (count all-jars)]
    (take max (drop offset all-jars))))

;; Get metadata about a Java Class
(defn string-to-class
  "Given a string, return corresponding class, or throw exception"
  [classname]
  (try
    (.. Class (forName classname))
    (catch ClassNotFoundException _ nil)))

;; TODO: I'm sure there's a way to clean this up, maybe with protocols?
(defmulti get-class-modifiers
  "Get list of class modifiers such as 'public static'" class)
(defmethod get-class-modifiers :default [aclass]
  (.. Modifier (toString (.getModifiers aclass))))
(defmethod get-class-modifiers String [s]
  (get-class-modifiers (.. Class (forName s))))

(defmulti get-class-interfaces
  "Given a class return list of implemented Interfaces"
  class)
(defmethod get-class-interfaces :default [aclass]
    (seq (.. aclass (getGenericInterfaces))))
(defmethod get-class-interfaces String [s]
  (get-class-interfaces (.. Class (forName s))))

;; TODO: need to revisit
(defmulti get-class-type-params class)
(defmethod get-class-type-params :default [aclass]
  (seq (.. aclass (getTypeParameters))))
(defmethod get-class-type-params String [s]
  (get-class-type-params (.. Class (forName s))))

;; Get metadata about Java Methods
(defn get-java-methods
  "Get a collection of all the methods of string CLASSNAME"
  [classname]
  (try
    (seq (.. Class (forName classname) (getMethods)))
    (catch ClassNotFoundException _ nil)))

(defn get-param-types
  "Take a Method, and return param types as nicely formatted string"
  [method]
  (apply str
         (interpose ", "
                    (map #(format "%s" %)
                         (seq (.getParameterTypes method))))))

(defn get-java-constructors
  "Get a collection of all the constructors of CLASSNAME (string)"
  [classname]
  (try
    (seq (.. Class (forName classname) (getConstructors)))
    (catch ClassNotFoundException _ nil)))

(defn get-java-declared-constructors
  "Get a collection of all the constructors of CLASSNAME (string)"
  [classname]
  (try
    (seq (.. Class (forName classname) (getDeclaredConstructors)))
    (catch ClassNotFoundException _ nil)))


