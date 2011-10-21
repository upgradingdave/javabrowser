(ns javabrowser.web
  (:use [ring.adapter.jetty :only (run-jetty)]
        [ring.util.response :only (redirect)]
        [compojure.core]
        [hiccup.core :only (html)]
        [hiccup.page-helpers :only (include-css include-js)]
        [ring.middleware.json-params]
        [javabrowser.reflect])
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]
            [clojure.string :as str]
            [clj-json.core :as json])
  (:import (java.net URLEncoder)
           (java.lang.reflect Modifier)
           (java.lang.reflect TypeVariable)))

(def default-stylesheets
  ["/stylesheets/application.css"])

(def default-javascripts
  ["/javascripts/jquery.js" "/javascripts/application.js"])

(defn split-query-string-into-pairs
  "Split query string into a coll of pairs of key=value pairs"
  [query-string]
  (clojure.string/split 
   (clojure.string/replace
    (clojure.string/replace query-string "&amp;" "&") "&" "&amp;") #"&amp;"))

(defn split-pairs-into-map
  "Convert COLL like [\"param1=val1\" \"param2=val2\"] into map like
  {:param1 \"val1\" :param2 \"val2\"} "
  [coll]
  (let [params (map #(let [pair (clojure.string/split % #"=")]
                       {(keyword (first pair)) (second pair)})
                    coll)]
    (if (= (count params) 1)
      (first params)
      (apply conj params))))

(defn parse-query-string
  "Convert QUERY-STRING string into map of key value pairs"
  [query-string]
  (if (empty? query-string)
    ""
    (split-pairs-into-map (split-query-string-into-pairs query-string))))

(defn json-response
  [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string data)})

(defn text-response
  [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "text/plain"}
   :body data})

(defn layout
  [& [sidebar body]]
  (html
   [:head
    [:meta {:charset "utf-8"}]
    [:title "Java Browser"]
    (apply include-css default-stylesheets)
    (apply include-js default-javascripts)]
   [:body
    [:div {:class "two-col"}
     [:div {:id "sidebar"}
      sidebar]
     [:div {:id "content"}
      body]]
    [:script {:type "text/javascript"
              :src "/javascripts/closure/closure/goog/base.js"}]
    [:script {:type "text/javascript"
              :src "/javascripts/javabrowser.js"}]
    [:script {:type "text/javascript"}
     "goog.require('javabrowser.repl');\ngoog.require('javabrowser.core');"]]))

(defn sidebar-html
  [& search]
  (html
   [:div {:id "sidebar"}
    [:div {:id "jars"}
     [:div {:class "searchbox"}
      [:input {:type "text"}]]
     [:ul {:id "list"}
      [:li {:id "jar-name"}
       [:a {:href "#"} "TODO" ]]]]
    [:div {:id "classes"}
     [:div {:class "searchbox"}
      [:input {:type "text"}]]
     [:ul {:id "list"}
      [:li {:id "class-name"}
       [:a {:href "#"} "TODO" ]]]]]))

(defn java-methods-html
  "Build a structure that represents methods of a class"
  [coll]
  (vec
   (cons :ul
         (cons {:id "methods"}
               (map #(vector :li (str (.. Modifier (toString (.getModifiers %)))
                                      " " (.. % getReturnType getName)
                                      " " (.. % getName)
                                      "(" (get-param-types %) ")"))
                    coll)))))

(defn build-class-html
  "Generate html to display metadata about a class"
  [class-name]
  (let [aclass (string-to-class class-name)]
      (html [:div
             [:div {:class "class-name"}
              [:h1 (str (get-class-modifiers aclass) " " (.getName aclass))]]
             (if (not (empty? (get-class-interfaces aclass)))
               [:div {:class "class-interfaces"}
                (str
                 "implements "
                 (format-class-interfaces (get-class-interfaces aclass)))]
               )
             ])))

(defn content-html
  "Render structure that represents details of a class"
  [& [classname]]
  (str (java-methods-html (get-java-methods classname))))

(defroutes application-routes
  ;; (GET "/" [] (redirect "/methods?classname=java.lang.Object"))
  (GET "/methods" request
       (let [classname (:classname (parse-query-string (:query-string request)))]
         (layout (sidebar-html) (html (content-html classname)))))
  ;; Build a html fragment that describes all details about a class
  ;; and return it
  (GET "/rest/classdetail" request
       (let [classname (:classname (parse-query-string (:query-string request)))]
         (text-response (list (content-html classname)))))
  (GET "/rest/search" request
       (let [search-term (:search (parse-query-string (:query-string request)))]
         (json-response (search-classes search-term))))
  (GET "/rest/jars" request
       (let [search-term (:search (parse-query-string (:query-string request)))
             jar-path (:jar (parse-query-string (:query-string request)))]
         (cond
          (not (empty? search-term)) (json-response (search-jars search-term))
          (not (empty? jar-path)) (json-response (get-classes-in-zip jar-path)))))
  (GET "/request" request
       (html [:div (str request)]))
  ;;(route/files "/" {:root "resources/public"})
  (route/resources "/")
  (route/not-found "<h1>Not Found</h1>"))

(def app
  (->
   (handler/site application-routes)))
