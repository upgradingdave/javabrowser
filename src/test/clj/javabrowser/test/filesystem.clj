(ns javabrowser.test.filesystem
  (:use [javabrowser.filesystem]
        [clojure.test]
        [clojure.java.io :only (file)]))

(deftest test-filename
  (is (= "clj-json-0.4.0.jar"
         (get-file-name "javabrowser/lib/clj-json-0.4.0.jar"))
      "Get filename from path")
  (is (= "" (get-file-name "")) "Empty filename from path")
  (is (= nil (get-file-name nil)) "nil filename from path"))

(deftest test-find-file
  (is (file-in-dir? "src/test/resources/dir1" "a.xml") "a.xml is inside dir1")
  (is (not (file-in-dir? "src/test/resources/dir1/dir2" "a.xml")) "a.xml is not inside dir2")
  (is (find-file "src/test/resources/dir1/dir2/dir3" "a.xml") "a.xml is two folders up from dir3")
  (is (not (find-file "src/test/resources/dir1/dir2" "c.xml")) "c.xml is in dir3 so it won't be found"))

(deftest test-sort
  (let [files ["/b/a.jar" "/a/c.jar" "/c/b.jar"]]
    (is (= "/a/c.jar" (first (sort files))) "Normal sort")
    (is (= "/b/a.jar" (first (sort file-name-comparator files))) "File name sort")))





