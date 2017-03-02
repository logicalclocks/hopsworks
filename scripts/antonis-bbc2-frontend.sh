#!/bin/bash
# Deploy the frontend to the glassfish home directory and run bower
export PORT=50058
export WEBPORT=8078
export SERVER=bbc2.sics.se
export key=private_key
usr=antonis
basedir=/srv/hops/domain1

scp -i $HOME/.ssh/bbc_id_rsa ${usr}@${SERVER}:/home/${usr}/hopsworks-chef/.vagrant/machines/default/virtualbox/private_key .  
ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -p $PORT vagrant@${SERVER} "cd ${basedir} && sudo chown -R glassfish:vagrant docroot && sudo chmod -R 775 *"

scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -P ${PORT} -r ../hopsworks-web/yo/app/ vagrant@${SERVER}:${basedir}/docroot
scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -P ${PORT} ../hopsworks-web/yo/bower.json vagrant@${SERVER}:${basedir}/docroot/app

ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -p $PORT vagrant@${SERVER} "cd ${basedir}/docroot/app && bower install"

#ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -p $PORT vagrant@${SERVER} "cd ${basedir}/docroot/app && perl -pi -e \"s/getApiLocationBase\(\)/'http:\/\/${SERVER}:${WEBPORT}\/hopsworks-api/api\/'/g\" scripts/services/RequestInterceptorService.js"

#google-chrome -new-tab http://${SERVER}:$WEBPORT/app
