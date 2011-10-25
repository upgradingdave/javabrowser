;; Reusable helper code for displaying and managing an unordered list of items
(ns javabrowser.ul
  (:require
   [javabrowser.util :as util]
   [goog.events :as events]
   [goog.dom :as dom]
   [goog.dom.classes :as classes]
   [goog.dom.query :as query]))

(defn remove-all-items
  "Remove all list items out of list with id ELEMID"
  [elemid]
   (let [ul (util/get-element elemid)]
    (dom/removeChildren ul true)))

(defn add-li
  "Add LI element to list identified by elemid"
  [elemid li]
  (. (util/get-element elemid) (appendChild li)))

(defn create-and-add-li
  [elemid content opts]
  (let [li (create-li content opts)]
    (. (util/get-element elemid) (appendChild li))))

(defn create-li
  "CONTENT is hiccup style structure that should be displayed as child
  of li element. ON-CLICK (optional) is a function that will be called
  when user clicks on li. DATA (optional) is stuff to send to on-click
  function. CLASSES (optional) is string of css classes"
  [content opts]
  (let [{onclick :onclick data :data classes :classes} opts
        anchor (util/build [:a {:href "#" :class classes} content])
        li (util/build [:li])]
    (. li (appendChild anchor))
    (if onclick
      (do
        (set! (.data anchor) data)
        (events/listen anchor (. events/EventType CLICK)
                       onclick)))
    li))

(defn un-select-all
  "Unselect all items in list with id ELEMID by removing 'selected' class"
  [elemid]
  (let [items (dom/query (str elemid "a"))]
    (doseq [item items]
      (util/log "attempting to remove")
      (classes/remove item "selected"))))


