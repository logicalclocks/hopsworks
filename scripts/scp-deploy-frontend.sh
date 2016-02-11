#!/bin/bash
# Deploy the frontend to the glassfish home directory and run bower
export PORT=20003
export WEBPORT=14003
export SERVER=bbc1.sics.se
export key=private_key

scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -P ${PORT} -r ../yo/app/ vagrant@${SERVER}:/srv/glassfish/domain1/docroot
scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -P ${PORT} ../yo/bower.json vagrant@${SERVER}:/srv/glassfish/domain1/docroot

ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -p $PORT vagrant@${SERVER} "cd /srv/glassfish/domain1/docroot && chown -R glassfish:vagrant * && chmod -R 775 *"
ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -p $PORT vagrant@${SERVER} "cd /srv/glassfish/domain1/docroot && bower install && cd app && perl -pi -e \"s/bower_components/..\/bower_components/g\" index.html && perl -pi -e \"s/getLocationBase\(\)/'http:\/\/${SERVER}:${WEBPORT}\/hopsworks'/g\" scripts/services/RequestInterceptorService.js"

google-chrome -new-tab http://${SERVER}:$WEBPORT/app
