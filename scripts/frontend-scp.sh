#!/bin/bash
# Deploy the frontend to the glassfish home directory and run bower
set -e
export PORT=22
export WEBPORT=8080
export SERVER=$1
export USER=ubuntu
export key=${HOME}/.ssh/id_rsa

ssh -i $key -p $PORT ${USER}@${SERVER} "cd /srv/glassfish/domain1 && sudo chown -R glassfish:${USER} docroot && sudo chmod -R 775 *"

scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -P ${PORT} -r ../yo/app/ ${USER}@${SERVER}:/srv/glassfish/domain1/docroot
scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -P ${PORT} ../yo/bower.json ${USER}@${SERVER}:/srv/glassfish/domain1/docroot/app

ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -p $PORT ${USER}@${SERVER} "cd /srv/glassfish/domain1/docroot/app && bower install && perl -pi -e \"s/getLocationBase\(\)/'http:\/\/${SERVER}:${WEBPORT}\/hopsworks'/g\" scripts/services/RequestInterceptorService.js"

google-chrome -new-tab http://${SERVER}:$WEBPORT/app
