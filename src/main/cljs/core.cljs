(ns javabrowser.core
  (:require
   [cljs.reader :as reader]
   [goog.dom :as dom]
   [goog.json :as json]
   [goog.string :as string]
   [goog.array :as array]
   [goog.events :as events]
   [goog.async.Delay :as Delay]
   [goog.net.XhrIo :as xhr]
   [goog.debug.Logger :as Logger]
   [goog.debug.Logger.Level :as Level]
   [goog.debug.Console :as Console]
   [goog.dom.query :as query]))

(def DELAY 500)
(def HOST "http://localhost:3000/rest/jars?search=.*")
(def DEFAULT_SEARCH_VAL "Search for Packages")

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

(defn get-list-of-jars
  [search]
  (request
   (str "http://localhost:3000/rest/jars?search=" search)
   list-of-jars-callback))

(defn list-of-jars-callback
  [response]
  (update-list-of-jars (parse-response response)))

(defn update-list-of-jars
  [coll]
  (let [package-list  (aget (dom/query "#jars ul") 0)]
    (dom/removeChildren  package-list)
    (loop [coll coll]
      (. package-list (appendChild (create-jar-li (first coll))))
      (if (not (empty? coll))
        (recur (next coll))))))

(defn create-jar-li
  [jar-name]
  (dom/createDom "li" nil (dom/createDom "a" (js* "{href:'#'}") jar-name)))


