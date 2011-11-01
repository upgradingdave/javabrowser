This is a web project that I wrote primarily to learn Clojure (and
Clojurescript!). It's very alpha and so a little rough around the
edges, but as I have time and/if there's any interest, I'd be happy to
add more functionality.

Itch: You are working with Maven and would like a helpful view of
all the jars and classes on your project's classpath.

Scratch: When this webapp is started from within a directory that is a
maven project, it will use the pom to find jar dependencies and then
display a nice web page that allows you to browse and search the
classes that are being included the classpath by maven. 

Watch a 3 minute video of how to use inside you're maven project: 

http://www.screencast.com/t/CSXMSpCDR2i

# Directory Layout

* src/main/clj 

Contains clojure serve side code 

* src/test/clj

Server side unit tests

* src/test/resources

Stuff used by server unit tests

* src/main/cljs

Contains clojurescript client side stuff

* serve-project/stylesheets

Contains the sass stylesheets. These are automatically converted to
css files inside resources/public/stylesheets

* resources/public 

Static resources that are all bundled up and included in the
war. Stylesheets are generated from `stylesheets` sass
files. Javascript is generated from `src/main/cljs`. Html is generated
by exporting serve haml views. 

# Server

## Start Compojure Web Server

    lein ring server

## Run Unit tests

    lein test

## Start a Clojure REPL

In Emacs: 

    lein swank

Then, in emacs: 

    slime-connect

Outside of Emacs (but why?!!!): 

    lein repl

## Server Features

### Find all jars on classpath

API

    (get-jars-on-classpath)

REST

    /rest/jars?search=.*

### Search for jar by name

API 

    (search-jars <name>)

REST

    /rest/jars?search=<name>

### Find all classes in a jar

API

    (get-classes-in-zip <path-to-jar>)

REST (wip, need to finish)

    /rest/jars?jar=<path-to-jar>

### Searches inside jars in pom.xml for class by name

API 

    (search-classes <class-name>)

REST

    /rest/search?search=<class-name>

### Search for classes within jars

API
   
    (get-classes-in-zips [jar-file1 jar-file2] ... etc)

REST

    /rest/classes?jars="path-to-jar1,path-to-jar2,etc.."

    or filter by class name: 
    /rest/classes?jars="path-to-jar1,path-to-jar2,etc.."&search="Appender"

    or filter AND page the results
    /rest/classes?jars="path-to-jar1,path-to-jar2,etc.."&search="Appender"&offset=20&max=20

# Front End (Clojurescript, Sass, Haml)

I'm using a few technologies here including Clojurescript, and a ruby
gem called "Serve" (that uses haml, and sass).

## Ruby Serve Project

This is also a Serve project just for convenience of having compass,
sass and haml available. Run `serve` at the command line and then
browse to port 4000.

Use `serve export . resources/public`, then rename haml files to
html. 

## Clojurescript

Start a clojure repl then do the following to compile cljs to
javascript.

    (use 'cljs.closure)
    (def opts {:output-to "resources/public/javascripts/javabrowser.js" :output-dir "resources/public/javascripts/out"})
    (build "src/main/cljs" opts)

The haml files can then use the the js. You can also play around with
cljs files inside a cljs browser repl (use browser-repl shell script,
or, in emacs, call inferior-lisp with it set to use browser-repl).

# Building and deploying

## scripts/compile.sh

This compiles clojurescript into javascript code and copies resulting
javascript a well as required css and images from closure project into the serve-project. 

## Start serve

At this point, you can use the `serve` command to play around with
stylesheets and html layouts

## scripts/deploy.sh

This exports stuff out of serve and puts it into resources/public/ so
that it can be used by the java webapp. 

## Start jetty

At this point you can run the java webapp using `lein ring server`

## Create war

Change the version in project.clj and maven-project/pom.xml. 

    lein ring uberwar

## Deploy as Maven Plugin

Copy the uberwar to `src/main/resources/javabrowser.war`, then cd into
maven-plugin and do a `mvn install`

## Use locally as maven plugin

At this point, you can add it as a maven depencency to local
projects like this: 

      <build>
       <plugins>
         ...
         <plugin>
           <groupId>com.upgradingdave</groupId>
           <artifactId>javabrowser-maven-plugin</artifactId>
           <version>x.x.x</version>
         </plugin>
         ...
       </plugins>
      </build>

Then use `mvn javabrowser:start`

# RoadMap

Ideas for future versions

* Make all class and interface names in class details click thru links

* Add ability to drill down into source code using maven capabilities

* Add ability to toggle between runtime, test and provided scopes

* Maybe make it a hosted webapp where (in addition to local maven
  plugin)? People could search for jars similar to findjars website
  and then have ability to search and inspect classes.

## Version 0.0.3 (Released Oct 31 2011)

* Figured out how to use requiresDependencyResolution to include
  project's depenendencys inside the custom mojo. 

* Added more features for parsing maven pom.xml such as ability to
replace property tokens

* Converted jquery to clojurescript

* Changed UI to use combo box to select jars, then changed back to use
  list. Learned a lot about google closure widgets ;-)

* Fixed relative file path so it works in prod jar and in lein ring
  server (sort of)

# License

Copyright (C) 2011 Dave Paroulek (upgradingdave.com)

Distributed under the Eclipse Public License, the same as Clojure.
