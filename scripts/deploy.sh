#!/bin/sh

rm -rf resources/public
mkdir -p resources/public/images
serve export serve-project resources/public
mv resources/public/index.haml resources/public/index.html
# cp -R third_party resources/public/javascripts/closure/
# cp -R closure/css resources/public/stylesheets/css
# cp -R closure/images resources/public/images/closure
# cp -R images/* resources/public/images/


