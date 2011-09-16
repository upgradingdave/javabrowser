(ns javabrowser.filesystem
  (:import (java.io File)))

(defn file-in-dir?
  "Determine whether filename exists in dirpath (relative or absolute)"
  [dirpath filename]
  (not (empty? (filter #(re-find (re-pattern (str "(?i)" filename)) %)
                       (.. (clojure.java.io/file dirpath) list)))))

(defn get-full-dir-path
  "Returns full directory path to dir or file. Returns nil if file
  Doesn't exist"
  [dirpath]
  (let [file (clojure.java.io/file dirpath)]
    (if (. file (exists))
      (. file (getCanonicalPath))
      nil)))
  
(defn parent-dir
  "Get the parent directory of dirpath (relative or absolute)"
  ([]
     (parent-dir "."))
  ([dirpath]
     (let [abs-path (or (get-full-dir-path dirpath) "")
           last-sep (. abs-path (lastIndexOf (File/separator)))]
       (if (> last-sep 0)
         (. abs-path (substring 0 last-sep))
         nil))))

(defn find-file
  "Start in dirpath and look for file. Look in parent folder
  until file is found or until you reach root"
  ([filename]
     (find-file "." filename))
  ([dirpath filename]
     (let [full-path (or (get-full-dir-path dirpath) "")
           parent (parent-dir full-path)]
       (if (empty? parent)
         nil
         (if (file-in-dir? full-path filename)
           (str full-path File/separator filename)
           (recur parent filename))))))
