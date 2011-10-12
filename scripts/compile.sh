#!/bin/sh

cljsc src/main/cljs '{:libs ["third_party/closure"] :output-to "serve-project/public/javascripts/javabrowser.js" :output-dir "serve-project/public/javascripts/closure/closure"}'

# Fix for relative path to third_party javascripts
sed "1s/.*/goog.addDependency(\"..\/..\/third_party\/closure\/goog\/dojo\/dom\/query.js\", ['goog.dom.query'], ['goog.array', 'goog.dom', 'goog.functions', 'goog.string', 'goog.userAgent']);/" "serve-project/public/javascripts/javabrowser.js" > "serve-project/public/javascripts/javabrowser.js"
