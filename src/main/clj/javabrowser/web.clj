(ns javabrowser.web
  (:use [ring.adapter.jetty :only (run-jetty)]
        [ring.util.response :only (redirect)]
        [compojure.core]
        [hiccup.core :only (html)]
        [hiccup.page-helpers :only (include-css include-js)]
        [ring.middleware.json-params]
        [javabrowser.reflect]
        [javabrowser.filesystem]
        [clojure.java.io :only (file)])
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]
            [clojure.string :as str]
            [clj-json.core :as json]
            [clojure.contrib.logging :as log])
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

(defn java-constructors-html
  "Build a structure that represents methods of a class"
  [coll]
  (vec
   (cons :ul
         (cons {:id "methods"}
               (map #(vector :li (str (.. Modifier (toString (.getModifiers %)))
                                      " " (.. % getName)
                                      "(" (get-param-types %) ")"))
                    coll)))))

(defn loading-html []
  [:div {:class "loading-wrap"}
   [:div {:class "loading hidden"}
    [:img {:src "/images/ajax-loader.gif"}]]])

(defn classdetail-html
  "Render structure that represents details of a class"
  [& [classname]]
  (let [interfaces (get-class-interfaces classname)]
    [:div {:id "classdetail"}
     [:h1 "Class Detail"]
     (loading-html)
     [:div {:id "class-nav"}
      [:a {:href "#declaration"} "declaration"]
      [:span " | "]
      [:a {:href "#constructors"} "constructors"]
      [:span " | "]
      [:a {:href "#methods"} "methods"]]
     [:a {:id "declaration" :name "declaration"}]
     [:div {:id "class-declaration"}
      [:div {:id "package-modifiers"} (get-class-modifiers classname)]
      [:h2 {:id "class-short-name"} (fqn-to-short-class-name classname)]
      (if interfaces
        [:div {:id "class-implements"}
         (str "Implements " (apply str (interpose ", " interfaces)))]
        [:span {:id "class-implements"} ""])]
     [:a {:id "constructors" :name "constructors"}]
     [:div {:id "class-constructors"}
      [:h2 "Constructors"]
      (java-constructors-html (get-java-constructors classname))]
     [:a {:id "methods" :name "methods"}]
     [:div {:id "class-methods"}
      [:h2 "Methods"]
      (java-methods-html (get-java-methods classname))]]))

(defn get-class-results
  "Takes comma delimitd list of JARS and returns first MAX classes
  starting at OFFSET in COLLection of jars"
  [jars & [search-term max offset]]
  (let [search-term (or search-term ".*")
        max (or max 20)
        offset (or offset 0)
        jars (clojure.string/split jars #",")]
    (take max
          (drop offset
                (sort file-name-comparator
                      (search-classes search-term (get-classes-in-zips
                                                   (map file jars))))))))

(defroutes application-routes
  ;; Build a html fragment that describes all details about a class
  ;; and return it
  (GET "/rest/classdetail" request
       (let [classname (:classname (parse-query-string (:query-string request)))]
         (text-response (list (classdetail-html classname)))))
  (GET "/rest/jars" request
       (let [search-term (:search (parse-query-string (:query-string request)))
             jar-path (:jar (parse-query-string (:query-string request)))]
         (cond
          (not (empty? search-term)) (json-response (search-jars search-term))
          (not (empty? jar-path)) (json-response (get-classes-in-zip jar-path)))))
  (GET "/rest/classes" request
       (let [jars (:jars (parse-query-string (:query-string request)))
             search-term (:search (parse-query-string (:query-string request)))
             offset (:offset (parse-query-string (:query-string request)))
             max (:max (parse-query-string (:query-string request)))]
         (if (not (empty? jars))
           (json-response
            (get-class-results jars search-term max offset))
           (json-response ""))))

  ;; (GET "/request" request
  ;;      (html [:div (str request)]))
  ;; files serves static files from directory defined by root
  ;; for dev: 
  ;;(route/files "/" {:root "resources/public"})

  ;;for prod
  (route/files "/" {:root "target/javabrowser-tmp/webapp"})

  ;; resources serves static files out of classpath
  ;;(route/resources "/" {:root ""})
  (route/not-found "<h1>Not Found</h1>"))

(def app
  (->
   (handler/site application-routes)))
