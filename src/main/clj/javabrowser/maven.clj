(ns javabrowser.maven
  (:use [javabrowser.filesystem])
  (:require [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.contrib.zip-filter :as zf]
            [clojure.contrib.zip-filter.xml :as zfxml]
            [clojure.string])
  (:import (java.io File)))

(defn local-repo-path
  "Return path to local repo"
  []
  (str (. System (getProperty "user.home")) "/.m2/repository"))

(defn find-pom
  "Search up the file system directory path for a pom.xml. Returns
  full path to pom.xml"
  ([] (find-pom "."))
  ([dirpath]
     (find-file dirpath "pom.xml")))

(defn mavenized?
  "returns true if we can find a pom.xml"
  ([] (mavenized? "."))
  ([dirpath]
      (if (find-pom dirpath) true false)))

(defn replace-token
  "Replace all instances of ${key} with value from token-map in target string"
  [token-map target]
  (loop [coll (keys token-map)
         target target]
    (if (not (empty? coll))
      (recur
       (rest coll)
       (clojure.string/replace
        target (str "${" (name (first coll)) "}") ((first coll) token-map)))
      target)))

(defn get-properties
  "Parse pom.xml and returns map of maven properties that are defined"
  ([] (get-properties (find-pom)))
  ([pom-path]
     (let [pom (zip/xml-zip (xml/parse pom-path))
           children (zfxml/xml-> pom :properties zf/children)]
       (zipmap 
        (map (fn [next-zip] (:tag (zip/node next-zip))) children)
        (map (fn [next-zip] (zfxml/text next-zip)) children)))))

(defn get-dependencies
  "Parse pom.xml and build list of jar dependencies"
  ([] (get-dependencies (find-pom)))
  ([pom-path]
     (let [pom (zip/xml-zip (xml/parse pom-path))
           token-map (get-properties pom-path)]
       (map (fn [group artifact version]
              (replace-token
               token-map
               (str (local-repo-path) "/"
                    (.. group (replace "." java.io.File/separator)) "/" artifact
                    "/" version "/" artifact "-" version ".jar")))
            (zfxml/xml-> pom :dependencies :dependency :groupId zfxml/text)
            (zfxml/xml-> pom :dependencies :dependency :artifactId zfxml/text)
            (zfxml/xml-> pom :dependencies :dependency :version zfxml/text)))))


