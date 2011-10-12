#!/bin/sh

rm -rf resources/public
serve export serve-project resources/public
mv resources/public/index.haml resources/public/index.html
cp -R third_party resources/public/javascripts/closure/

