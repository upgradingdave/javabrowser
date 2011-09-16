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

(defn path-to-jar-name
  "Convert path like
  '/Users/dparoulek/code/clojure/javabrowser/lib/clj-json-0.4.0.jar'
  to filename"
  [filepath]
  (apply str
   (drop (+ 1 (.. filepath (lastIndexOf
                            (. java.io.File separator)))) filepath)))

(defn path-to-class-name
  [filepath]
  "Convert path like 'com/apple/java/AppleSystemLog.class' into fully
qualified java class name"
  (str/replace (nth (re-matches #"(.*)\.class" filepath) 1) "/" "."))

(defn find-classes
  "TODO: WIP. Attempts to find names of all classes on classpath. First, find names of classes in all the jar files on the classpath"
  []
  (let [class-files (mapcat #(get-classes-in-zip %) (get-jars))]
    (map #(path-to-class-name %) class-files)
    ))

(defstruct search-results :total :offset :max :results)

(defn search-classes
  "Returns list of search results for a fully qualified class name on the classpath."
  [search-term & [offset max]] 
  (let [offset (or offset 0)
        max (or max 20)
        all-classes (filter #(re-seq (re-pattern (str "(?i)" search-term)) %) (find-classes))
        total (count all-classes)]
    (take max (drop offset all-classes))))

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

(defn get-class-modifiers
  "Given a Class return string of Modifiers (like public static etc)"
  [aclass]
  (.. Modifier (toString (.getModifiers aclass))))

(defn get-class-type-params
  "Given a class return list of TypeParameters"
  [aclass]
  (seq (.. aclass (getTypeParameters))))

(defn format-class-type-params
  "Given list of TypeParams, produce nicely formatted string"
  [coll]
  (apply str (interpose "," (map #(format "%s" %) coll))))

(defn get-class-interfaces
  "Given a class return list of implemented Interfaces"
  [aclass]
  (seq (.. aclass (getGenericInterfaces))))

(defn format-class-interfaces
  "Given a list of Type interfaces, produce nicely formatted string"
  [coll]
  (apply str (interpose ", " (map
                              #(html
                                [:a {:href (format "methods?classname=%s" %)}
                                 (format "%s" %)]) coll))))

(defn build-class-html
  "Generate html to display metadata about a class"
  [class-name]
  (let [aclass (string-to-class class-name)]
      (html [:div
             [:div {:class "class-name"}
              [:h1 (str (get-class-modifiers aclass) " " (.getName aclass))]]
             (if (not (empty? (get-class-interfaces aclass)))
               [:div {:class "class-interfaces"}
                (str
                 "implements "
                 (format-class-interfaces (get-class-interfaces aclass))) ]
               )
             ])))

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


