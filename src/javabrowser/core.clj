(ns javabrowser.core
  (:gen-class
   :init init
   :name javabrowser.JavaBrowser
   :methods [[simple [] String]
             [startServer [] void]])
  (:use [ring.adapter.jetty :only (run-jetty)]
        [ring.util.response :only (redirect)]
        [compojure.core]
        [hiccup.core :only (html)]
        [hiccup.page-helpers :only (include-css include-js)]
        [ring.middleware.json-params])
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]
            [clojure.string :as str]
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
           (str/split (. System (getProperty "java.class.path")) #":"))))

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

(defn path-to-jar-name
  "Convert path like
  '/Users/dparoulek/code/clojure/javabrowser/lib/clj-json-0.4.0.jar'
  to filename"
  [filepath]
  (apply str
   (drop (+ 1 (.. filepath (lastIndexOf
                            (. java.io.File separator)))) filepath)))

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

(defn search-jars
  [search-term & [offset max]]
  (let [offset (or offset 0)
        max (or max 20)
        all-jars (filter #(re-seq (re-pattern (str "(?i)" search-term)) %) (get-jars-on-classpath))
        total (count all-jars)]
    (take max (drop offset all-jars))))

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

(defn get-java-constructors
  "Get a collection of all the constructors of CLASSNAME (string)"
  [classname]
  (try
    (seq (.. Class (forName classname) (getConstructors)))
    (catch ClassNotFoundException _ nil)))

(defn get-java-declared-constructors
  "Get a collection of all the constructors of CLASSNAME (string)"
  [classname]
  (try
    (seq (.. Class (forName classname) (getDeclaredConstructors)))
    (catch ClassNotFoundException _ nil)))

(defn display-java-methods
  "Display a collection of Methods as html"
  [coll]
  (html [:ul {:id "methods"}
         (map  #(html [:li
                       (.. Modifier (toString (.getModifiers %)))
                       " "
                       (.. % getReturnType getName)
                       " "
                       (.. % getName)
                       "("
                       (get-param-types %)
                       ")"])
               coll)]))

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
  (if (empty? query-string)
    ""
    (split-pairs-into-map (split-query-string-into-pairs query-string))))

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
   [:div
    [:a {:href "javascript:doSearch('.*');"} "classes"]
    "|" [:a {:href "javascript:doJarSearch('.*');"} "jars"]]
   [:input {:id "searchbox" :type "text"}]
   [:div {:id "search-results"}]))

(defn content-html
  "Render HTML for content section"
  [& [classname]]
  (str (build-class-html classname)
       (html [:br])
       (display-java-methods (get-java-methods classname))))

(defroutes application-routes
  (GET "/" [] (redirect "/methods?classname=java.lang.Object"))
  (GET "/methods" request
       (let [classname (:classname (parse-query-string (:query-string request)))]
         (layout (sidebar-html) (content-html classname))))
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
  (route/resources "/")
  (route/not-found "<h1>Not Found</h1>"))

(defn main
  []
  (run-jetty (var application-routes) {:port 9000 :join? false})
  (println "Javabrowser started successfully. Browse to http://localhost:9000 to get started!"))

(defn handler [request])

;; Java interop stuff
(defn -init [] [[] (atom [])])

(defn -simple
  [this]
  "Hello from clojure")

(defn -startServer [this]
  (main)
  )

(def app
  (-> (handler/site application-routes)))

