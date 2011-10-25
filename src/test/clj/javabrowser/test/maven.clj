(ns javabrowser.test.maven
  (:use [javabrowser.maven]
        [clojure.test]
        [javabrowser.filesystem])
  (:require [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.contrib.zip-filter.xml :as zf])
  (:import (java.io File)))

;; (deftest test-get-dependencies
;;   (let [pom-path (find-pom "src/test/resources/.")
;;         pom-file (File. pom-path)]
;;     (is (= "" (get-dependencies pom-path)))))

(deftest test-replace-token
  (let [pom-path (find-pom "src/test/resources/.")
        pom-file (File. pom-path)
        ;; TODO: remove hard coded path
        expected '("/Users/dparoulek/.m2/repository/org/springframework/ldap/spring-ldap-core/1.3.1.RELEASE/spring-ldap-core-1.3.1.RELEASE.jar" "/Users/dparoulek/.m2/repository/org/clojure/clojure/1.3.0-beta1/clojure-1.3.0-beta1.jar")]
    (is (. pom-file exists) "Found Pom")
    (is (= 2 (count (get-dependencies pom-path))) "Found some dependency paths in pom")

    (is (= "maven-2.0-RELEASE"
           (replace-token {:maven.version "2.0" :maven.release "RELEASE"}
                          "maven-${maven.version}-${maven.release}"))
        "Able to replace single token")
    (is (= {:spring.version "3.0.5.RELEASE" :spring.ldap.version "1.3.1.RELEASE"}
           (get-properties pom-path)) "Extract properties out of pom")
    (is (= expected (get-dependencies pom-path)) "Replaced tokens correctly")))

