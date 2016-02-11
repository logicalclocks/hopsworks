#!/bin/bash
# Deploy the frontend to the glassfish home directory and run bower
export PORT=9191
export SERVER=snurran.sics.se
export key=private_key

scp -r ../yo/app -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key vagrant@${SERVER}:${PORT}/srv/glassfish/domain1/docroot
scp -r ../yo/bower.json -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key vagrant@${SERVER}:${PORT}/srv/glassfish/domain1/docroot

ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -p $PORT vagrant@${SERVER} "cd /srv/glassfish/domain1/docroot && bower install && perl -pi -e \"s/bower_components/..\/bower_components/g\" index.html && perl -pi -e \"s/getLocationBase\(\)/'http:\/\/${SERVER}:8080\/hopsworks'/g\" scripts/services/RequestInterceptorService.js"

google-chrome -new-tab http://${SERVER}:8080/app
