#!/bin/bash
# Deploy the frontend to the glassfish home directory and run bower

# Check if GLASSFISH_HOME is set
if [ -z "$GLASSFISH_HOME" ]; then
    echo "GLASSFISH_HOME is not set. Set it to the directory in which your glassfish instance is installed."
    exit 1
fi

# Copy the app folder to the right location
cp -r yo/app/ $GLASSFISH_HOME/glassfish/domains/domain1/docroot/
# Copy the bower.json
cp yo/bower.json $GLASSFISH_HOME/glassfish/domains/domain1/docroot/

#change directory to the glassfish docroot
cd $GLASSFISH_HOME/glassfish/domains/domain1/docroot
#install bower components
bower install
#Edit the index.html
cd app/
perl -pi -e "s/bower_components/..\/bower_components/g" index.html
perl -pi -e "s/getLocationBase\(\)/'http:\/\/localhost:8080\/hopsworks'/g" scripts/services/RequestInterceptorService.js

#Open a new firefox tab
#firefox -new-tab http://localhost:8080/app
google-chrome -new-tab http://localhost:8080/app
