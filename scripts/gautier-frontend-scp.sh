#!/bin/bash
# Deploy the frontend to the glassfish home directory and run bower
export PORT=50970
export WEBPORT=8980
export SERVER=bbc1
export key=private_key
usr=gautier

cp /home/gautier/hop/myhops/hopsworks-chef/.vagrant/machines/default/virtualbox/private_key .

ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -p $PORT vagrant@${SERVER} "cd /srv/glassfish/domain1 && sudo chown -R glassfish:vagrant docroot && sudo chmod -R 775 *"

scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -P ${PORT} -r ../yo/app/ vagrant@${SERVER}:/srv/glassfish/domain1/docroot
scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -P ${PORT} ../yo/bower.json vagrant@${SERVER}:/srv/glassfish/domain1/docroot/app

ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -p $PORT vagrant@${SERVER} "cd /srv/glassfish/domain1/docroot/app && bower install && perl -pi -e \"s/getLocationBase\(\)/'http:\/\/${SERVER}:${WEBPORT}\/hopsworks'/g\" scripts/services/RequestInterceptorService.js"

#chromium-browser -new-tab http://${SERVER}:$WEBPORT/app
