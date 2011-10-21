(ns javabrowser.core
  (:require
   [javabrowser.util :as util]
   [cljs.reader :as reader]
   [goog.dom :as dom]
   [goog.json :as json]
   [goog.string :as string]
   [goog.array :as array]
   [goog.events :as events]
   [goog.async.Delay :as gDelay]
   [goog.net.XhrIo :as xhr]
   [goog.debug.Logger :as Logger]
   [goog.debug.Logger.Level :as Level]
   [goog.debug.Console :as Console]
   [goog.dom.query :as query]
   [goog.ui.CustomButton :as CustomButton]
   [goog.ui.Menu :as Menu]
   [goog.ui.MenuButton :as MenuButton]
   [goog.ui.Separator :as Separator]
   [goog.ui.decorate :as decorator]))

(def DELAY 500)
(def HOST "/rest/jars?search=.*")
(def DEFAULT_SEARCH_VAL "Search for Packages")
(def DEFAULT_MAX_LENGTH 10)

(def jar-menu (goog.ui.Menu.))

(def initial-state {:selected-jars #{}})

(def state (atom initial-state))

(defn update-state
  "Call this from swap! to change state"
  [old-state new-jar]
     (let [jar-set (:selected-jars old-state)
           selected (get jar-set new-jar)]
       (if selected
         (assoc old-state :selected-jars (disj jar-set new-jar))
         (assoc old-state :selected-jars (conj jar-set new-jar)))))

(defn clear-selected-jars
  []
  (swap! state (fn [old-state] (assoc old-state :selected-jars {}))))

(defn toggle-selected-jar
  [jar-path]
  (swap! state update-state jar-path))

;; Manage menu of jars
(defn create-jar-menu-items
  "Create menu of jars"
  [coll]
  (do
    (. jar-menu (removeChildren true))
    (doseq [x coll]
      (. jar-menu (addItem (create-jar-menu-item x))))))

(defn create-jar-menu-button
  []
  (let [button (goog.ui.MenuButton. "Add Jars" jar-menu)
        jars (util/get-element "#jars .menu")]
    (. button (setDispatchTransitionEvents goog.ui.Component.State.ALL true))
    (. button (setId "jar-button"))
    (. button (render jars))
    (. button (setTooltip "Jar Menu"))
    (. jar-menu (setId "jar-menu"))
    (events/listen jar-menu (. goog.ui.Component.EventType ACTION)
                   handle-jar-menu-change)))

(defn create-jar-menu-item
  [jar-path]
  (let [display (util/trunc-string (util/get-file-name jar-path))
        item (goog.ui.MenuItem. display jar-path)]
    (. item (setId jar-path))
    (. item (setDispatchTransitionEvents goog.ui.Component.State.ALL true))
    item))

(defn handle-jar-menu-change
  [e]
  (let [item (. e target)
        jar-path (. item (getValue))]
    (util/log (str "Clicked Menu Item: " jar-path))
    (toggle-selected-jar jar-path)
    (update-list-of-jars (:selected-jars @state))
    (. jar-menu (removeChild jar-path true))
    (get-list-of-classes-in-jar jar-path)))

;; manage list of selected jars
(defn get-list-of-jars
  [search]
  (let [search (if (empty? search) ".*" search)]
    (util/request
     (str "http://localhost:3000/rest/jars?search=" search)
     (fn [response] (create-jar-menu-items (util/parse-response response))))))

(defn update-list-of-jars
  "COLL is a list of absolute file paths to jar files"
  [coll]
  (let [package-list (util/get-element "#jars ul")]
    (dom/removeChildren package-list true)
    (doseq [item coll]
      (. package-list (appendChild (create-jar-li item))))))

(defn create-jar-li
  [jar-path]
  (let [anchor (dom/createDom "a" (js* "{href:'#'}")
                              (util/trunc-string (util/get-file-name jar-path)))]
    (events/listen anchor
                   (. events/EventType CLICK)
                   (fn [] (do
                           (toggle-selected-jar jar-path)
                           (update-list-of-jars (:selected-jars @state))
                           )))
    (dom/createDom "li" nil anchor)))

;; manage list of classes 
(defn get-list-of-classes-in-jar
  [path-to-jar]
  (util/request
   (str "http://localhost:3000/rest/jars?jar=" path-to-jar)
   (fn [response] (update-list-of-classes (util/parse-response response)))))

(defn update-list-of-classes
  "COLL is a list of fully qualified classnames"
  [coll]
  (let [class-list (util/get-element "#classes ul")]
    (dom/removeChildren class-list true)
    (loop [coll coll]
      (if (not (empty? coll))
        (. class-list (appendChild (create-class-li (first coll)))))
      (if (not (empty? (next coll)))
        (recur (next coll))))))

(defn path-to-fqdn
  [path]
  (do
    (. (. path (replace (js/RegExp. "/" "g") ".")) (replace ".class" ""))))

(defn create-class-li
  [class-name] 
  (let [anchor (util/element :a {:href "#"} (util/trunc-string (util/get-file-name class-name) 25))]
    (events/listen anchor
                   (. events/EventType CLICK)
                   (fn [] (get-class-details (path-to-fqdn class-name))))
    (dom/createDom "li" nil anchor)))

;; Manage Class details
(defn get-class-details
  [classname]
  (util/request
   (str "http://localhost:3000/rest/classdetail?classname=" classname)
   (fn [response] (update-class-detail (util/parse-response response)))))

;; (defn update-class-detail
;;   [response]
;;   (let [classdetail (util/get-element "#col3")
;;         new (util/build response)]
;;     (dom/removeChildren classdetail)
;;     (. classdetail (appendChild new))))

(defn update-class-detail
  [response]
  (let [classdetail (util/get-element "#col3")
        new (util/build response)]
    (dom/removeChildren classdetail)
    (. classdetail (appendChild new))))

(defn addJarSearchListener
  []
  (let [searchbox (util/get-element "#jars input")
        delay (goog.async.Delay.
               (fn [] (get-list-of-jars (. searchbox value))) 500)]
    (events/listen searchbox
                   (. events/EventType KEYUP)
                   (fn [] (. delay (start))))))

(defn ^:export init
  []
  (create-jar-menu-button)
  (get-list-of-jars ".*"))

(init)

