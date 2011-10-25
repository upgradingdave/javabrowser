(ns javabrowser.util
  (:require
   [cljs.reader :as reader]
   [goog.dom :as dom]
   [goog.json :as json]
   [clojure.string :as string]
   [goog.array :as array]
   [goog.events :as events]
   [goog.async.Delay :as gDelay]
   [goog.net.XhrIo :as xhr]
   [goog.debug.Logger :as Logger]
   [goog.debug.Logger.Level :as Level]
   [goog.debug.Console :as Console]
   [goog.dom.query :as query]
   [goog.ui.ComboBox :as Select]
   [goog.ui.ComboBoxItem :as ComboItem]
   [goog.dom.classes :as classes]))

(def logger
  (let [logger (. goog.debug.Logger (getLogger "javabrowser"))
        console (goog.debug.Console.)]
    (. logger (setLevel goog.debug.Logger.Level.ALL))
    (. console (setCapturing true))
    logger))

(defn log
  [msg]
  (. logger (info msg)))

(defn request
  [url callback]
  (xhr/send url callback))

(defn parse-response
  [message]
  (reader/read-string
   (. message/target (getResponseText))))

(defn parse-response
  [message]
  (reader/read-string
   (. message/target (getResponseText))))

(defn get-element
  "Helper for finding dom elements using jquery like syntax. Returns
  first node found."
  [selector]
  (aget (dom/query selector) 0))

(defn trunc-string
  "Truncate STRING so that it is no longer than MAX-LENGTH. TODO:
  figure out how to make max-length optional and default"
  ([string]
     (trunc-string string 30))
  ([string max-length]
     (let [length (. string length)]
       (if (> length max-length)
         (str (. string (substring 0 (- max-length 3))) "...")
         string))))

(defn get-file-name
  [path]
  (let [length (. path length)
        idx (. path (lastIndexOf "/"))]
    (. path (slice (+ 1 idx) length))))

(defn path-to-fqdn
  "Convert file system path of class file to dot notation"
  [path]
  (do
    (. (. path (replace (js/RegExp. "/" "g") ".")) (replace ".class" ""))))

(defn hide-or-show
  "Toggle hidden css class of element identified by csspath"
  [csspath]
  (classes/toggle (get-element csspath) "hidden"))

(defn unselect-all
  "Unselect all items targeted by SELECTOR by removing 'selected' class"
  [selector]
  (let [items (dom/query selector)]
    (loop [idx 0]
      (if (< idx (alength items)) (do (classes/remove (aget items idx) "selected")
                                      (recur (inc idx)))))))

(defn select-all
  "Select all items targeted by SELECTOR by removing 'selected' class"
  [selector]
  (let [items (dom/query selector)]
    (loop [idx 0]
      (if (< idx (alength items)) (do (classes/add (aget items idx) "selected")
                                      (recur (inc idx)))))))

;; I stole these methods from twitterbuzz sample app
(defn append
  "Append all children to parent."
  [parent & children]
  (do (doseq [child children]
        (dom/appendChild parent child))
      parent))

(defn set-text
  "Set the text content for the passed element returning the
  element. If a keyword is passed in the place of e, the element with
  that id will be used and returned."
  [e s]
  (let [e (if (keyword? e) (get-element e) e)]
    (doto e (dom/setTextContent s))))

(defn normalize-args [tag args]
  (let [parts (string/split tag #"(\.|#)")
        [tag attrs] [(first parts)
                     (apply hash-map (map #(cond (= % ".") :class
                                                 (= % "#") :id
                                                 :else %)
                                          (rest parts)))]]
    (if (map? (first args))
      [tag (merge attrs (first args)) (rest args)]
      [tag attrs args])))

;; TODO: replace call to .strobj with whatever we come up with for
;; creating js objects from Clojure maps.

(defn element
  "Create a dom element using a keyword for the element name and a map
  for the attributes. Append all children to parent. If the first
  child is a string then the string will be set as the text content of
  the parent and all remaining children will be appended."
  [tag & args]
  (let [[tag attrs children] (normalize-args tag args)
        parent (dom/createDom (name tag)
                              (.strobj (reduce (fn [m [k v]]
                                                 (assoc m k v))
                                               {}
                                               (map #(vector (name %1) %2)
                                                    (keys attrs)
                                                    (vals attrs)))))
        [parent children] (if (string? (first children))
                            [(set-text (element tag attrs) (first children))
                             (rest children)]
                            [parent children])]
    (apply append parent children)))

(defn- element-arg? [x]
  (or (keyword? x)
      (map? x)
      (string? x)))

(defn build
  "Build up a dom element from nested vectors."
  [x]
  (if (vector? x)
    (let [[parent children] (if (keyword? (first x))
                              [(apply element (take-while element-arg? x))
                               (drop-while element-arg? x)]
                              [(first x) (rest x)])
          children (map build children)]
      (apply append parent children))
    x))
