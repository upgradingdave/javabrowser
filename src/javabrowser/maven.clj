(ns javabrowser.maven
  (:use [ring.middleware.json-params])
  (:require [clojure.string :as str])
  (:import (org.apache.maven.artifact.repository DefaultArtifactRepository)
           (org.codehaus.classworlds ClassWorld)
           (java.io File)
           (org.codehaus.plexus.embed Embedder)))

(defn pom-in-dir?
  [filepath]
  (not (empty? (filter #(re-find #"pom.xml" %) (.. (clojure.java.io/file filepath) list)))))

(defn get-dependencies
  "Gets a list of maven dependencies for the current project"
  [pom-file]
  ;; create plexus container
  (let [classworld (ClassWorld. "plexus.core"
                                (.. Thread (currentThread) (getContextClassLoader)))
        emedder (Embedder.)
        pomFile (File. "pom.xml")])
    (import 'org.apache.maven.project.DefaultProjectBuilderConfiguration)
    (def pomConfig (DefaultProjectBuilderConfiguration.))
    (import 'org.apache.maven.project.DefaultMavenProjectBuilder)
    (def projBuilder (DefaultMavenProjectBuilder.))
    (.. projBuilder (initialize))
    set local artifact repository
    then build

  
)

