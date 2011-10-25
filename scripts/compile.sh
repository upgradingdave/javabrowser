#!/bin/sh

SERVE_PATH="serve-project/public/javascripts"

rm -rf $SERVE_PATH

cljsc src/main/cljs "{:libs [\"third_party/closure\"] :output-to \"$SERVE_PATH/javabrowser.js\" :output-dir \"$SERVE_PATH/closure/closure\"}"

# Fix for relative path to third_party javascripts
sed "1s%.*%goog.addDependency(\"../../third_party/closure/goog/dojo/dom/query.js\", ['goog.dom.query'], ['goog.array', 'goog.dom', 'goog.functions', 'goog.string', 'goog.userAgent']);%" "$SERVE_PATH/javabrowser.js" > "$SERVE_PATH/$$" && mv "$SERVE_PATH/$$" "$SERVE_PATH/javabrowser.js"

cp -R third_party serve-project/public/javascripts/closure/
cp -R closure/css serve-project/public/stylesheets/css
cp -R closure/images serve-project/public/images/closure
cp -R images/* serve-project/public/images/

cp -R serve-project/public/* resources/public/



