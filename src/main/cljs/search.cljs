;; Reusable helper code for displaying and managing an autocomplete
;; search box
(ns javabrowser.search
  (:require
   [javabrowser.util :as util]
   [goog.events :as events]
   [goog.dom :as dom]
   [goog.async.Delay :as Delay]))

(defn addSearchListener
    "Given the ELEMID of an input box, setup a autocomplete style
  search. HANDLER should be a function that accepts the text of the
  text box as a param"
    [elemid handler]
  (let [searchbox (util/get-element "#classes input")
        delay (goog.async.Delay.
               (fn [] (handler (. searchbox value))) 500)]
    (events/listen searchbox
                   (. events/EventType KEYUP)
                   (fn [] (. delay (start))))))
