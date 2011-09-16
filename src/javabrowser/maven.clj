(ns javabrowser.maven
  (:use [javabrowser.filesystem])
  (:require [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.contrib.zip-filter.xml :as zf])
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


(defn get-dependencies
  "Parse pom.xml and build list of jar dependencies"
  []
  (let [zipper (zip/xml-zip (xml/parse (find-pom)))]
    (map (fn [group artifact version]
           (str (local-repo-path) "/"
                (.. group (replace "." java.io.File/separator)) "/" artifact
                "/" version "/" artifact "-" version ".jar"))
         (zf/xml-> zipper :dependencies :dependency :groupId zf/text)
         (zf/xml-> zipper :dependencies :dependency :artifactId zf/text)
         (zf/xml-> zipper :dependencies :dependency :version zf/text))))





