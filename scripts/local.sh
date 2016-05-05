#!/bin/bash
# Deploy the frontend to the glassfish home directory and run bower
export PORT=2222
export WEBPORT=8081
export SERVER=localhost
export usr=$USER

echo "Make sure ownership of files has been fixed: /srv/glassfish/domain1/docroot/"
echo "sudo chown $usr:$USER -R /srv/glassfish/domain1/docroot/"
echo "sudo chmod -R 775 /srv/glassfish/domain1/docroot/"
echo "working...."

cp -r ../yo/app/ /srv/glassfish/domain1/docroot
#cp ../yo/bower.json /srv/glassfish/domain1/docroot/app
cp -r ../yo/bower_components /srv/glassfish/domain1/docroot/app/
cd /srv/glassfish/domain1/docroot/app 
#bower install 
perl -pi -e "s/getLocationBase\(\)/'http:\/\/${SERVER}:${WEBPORT}\/hopsworks'/g" scripts/services/RequestInterceptorService.js

google-chrome -new-tab http://${SERVER}:$WEBPORT/app
