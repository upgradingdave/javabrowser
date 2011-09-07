(ns javabrowser.core
  (:use [ring.adapter.jetty :only (run-jetty)]
        [ring.util.response :only (redirect)]
        [compojure.core :only (defroutes GET)]
        [hiccup.core :only (html)]
        [hiccup.page-helpers :only (include-css include-js)]
        [compojure.route]
        [ring.middleware.json-params])
  (:require [clojure.string :as str]
            [clj-json.core :as json])
  (:import (java.net URLEncoder)
           (java.lang.reflect Modifier)
           (java.lang.reflect TypeVariable)))

;; Get a list of all classes on classpath. Looks like there are
;; two relevant System properties: java.class.path and
;; sun.boot.class.path

(defn get-jars-on-classpath
  "Gets a list of (abs paths) to jars on the classpath"
  []
  (filter
   #(re-matches #".*\.jar" %)
   (concat (str/split (. System (getProperty "sun.boot.class.path")) #":")
           (str/split (. System (getProperty "java.class.path")) #":")))
  )

(defn get-entries-in-zip
  [fileName]
  "Get list of all entries inside jar file. TODO: when I tried to use
map, it complains that z is closed"
  (try
    (with-open [z (java.util.zip.ZipFile. fileName)]
      (loop [e (enumeration-seq (.entries z))
             result '()]
        (if (empty? e)
          result
          (recur (rest e) (conj result (.getName (first e)))))))
    (catch Exception e ())))

(defn get-classes-in-zip
  "Get list of class files inside jar file."
  [fileName]
  (filter #(re-matches #".*\.class" %) (get-entries-in-zip fileName)))

(defn path-to-class-name
  [filepath]
  "Convert path like 'com/apple/java/AppleSystemLog.class' into fully
qualified java class name"
  (str/replace (nth (re-matches #"(.*)\.class" filepath) 1) "/" "."))

(defn find-classes
  "TODO: WIP. Attempts to find names of all classes on classpath. First, find names of classes in all the jar files on the classpath"
  []
  (let [class-files (mapcat #(get-classes-in-zip %) (get-jars-on-classpath))]
    (map #(path-to-class-name %) class-files)
    ))

(defstruct search-results :total :offset :max :results)

(defn search-classes
  "Returns list of search results for a fully qualified class name on the classpath."
  [search-term & [offset max]] 
  (let [offset (or offset 0)
        max (or max 20)
        all-classes (filter #(re-seq (re-pattern (str "(?i)" search-term)) %) (find-classes))
        total (count all-classes)]
    (take max (drop offset all-classes))))

;; Get metadata about a Java Class
(defn string-to-class
  "Given a string, return corresponding class, or throw exception"
  [classname]
  (try
    (.. Class (forName classname))
    (catch ClassNotFoundException _ nil)))

(defn get-class-modifiers
  "Given a Class return string of Modifiers (like public static etc)"
  [aclass]
  (.. Modifier (toString (.getModifiers aclass))))

(defn get-class-type-params
  "Given a class return list of TypeParameters"
  [aclass]
  (seq (.. aclass (getTypeParameters))))

(defn format-class-type-params
  "Given list of TypeParams, produce nicely formatted string"
  [coll]
  (apply str (interpose "," (map #(format "%s" %) coll))))

(defn get-class-interfaces
  "Given a class return list of implemented Interfaces"
  [aclass]
  (seq (.. aclass (getGenericInterfaces))))

(defn format-class-interfaces
  "Given a list of Type interfaces, produce nicely formatted string"
  [coll]
  (apply str (interpose ", " (map
                              #(html
                                [:a {:href (format "methods?classname=%s" %)}
                                 (format "%s" %)]) coll))))

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
                 (format-class-interfaces (get-class-interfaces aclass))) ]
               )
             ])))

;; Get metadata about Java Methods

(defn get-java-methods
  "Get a collection of all the methods of string CLASSNAME"
  [classname]
  (try
    (seq (.. Class (forName classname) (getMethods)))
    (catch ClassNotFoundException _ nil)))

(defn get-param-types
  "Take a Method, and return param types as nicely formatted string"
  [method]
  (apply str
         (interpose ", "
                    (map #(format "%s" %)
                         (seq (.getParameterTypes method))))))

(defn display-java-methods
  "Convert a collection of Methods into a html table"
  [coll]
  (html [:table
         [:thead
          [:th "Modifiers"]
          [:th "Return Type"]
          [:th "Method Name"]
          [:th "Params"]]
         [:tbody
          (map
           #(html [:tr
                   [:td (.. Modifier (toString (.getModifiers %)))]
                   [:td (.. % getReturnType getName)]
                   [:td (.. % getName)]
                   [:td (get-param-types %)]])
           coll)]]))

;; Web App Related stuff

(def default-stylesheets
  ["/stylesheets/application.css"])

(def default-javascripts
  ["/javascripts/jquery.js"
   "/javascripts/application.js"])

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
  (split-pairs-into-map (split-query-string-into-pairs query-string)))

(defn json-response
  [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string data)})

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
      body]]]))

(defn sidebar-html
  [& search]
  (html
   [:input {:id "searchbox" :type "text"}]
   [:div {:id "search-results"}]))

(defn content-html
  "Render HTML for content section"
  [& [classname]]
     (str (build-class-html classname)
         (display-java-methods (get-java-methods classname))))

(defroutes application-routes
  (GET "/" [] (redirect "/methods?classname=java.lang.String"))
  (GET "/methods" request
       (let [classname (:classname (parse-query-string (:query-string request)))]
         (layout (sidebar-html) (content-html classname))))
  (GET "/rest/search" request
       (let [search-term (:search (parse-query-string (:query-string request)))]
         (json-response (search-classes search-term))))
  (GET "/request" request
       (html [:div (str request)]))
  (files "/")
  (not-found "<h1>Not Found</h1>"))

(defn main []
  (run-jetty (var application-routes) {:port 9000 :join? false})
  (println "Javabrowser started successfully. Browse to http://localhost:9000 to get started!"))


