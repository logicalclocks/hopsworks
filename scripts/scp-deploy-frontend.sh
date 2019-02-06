#!/bin/bash
# Deploy the frontend to the glassfish home directory and run bower
export SERVER=vm
export WEBPORT=35139
export key=private_key

ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes vagrant@${SERVER} "cd /srv/hops/domains/domain1 && sudo chown -R glassfish:vagrant docroot && sudo chmod -R 775 *"

scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -r ../hopsworks-web/yo/app/ vagrant@${SERVER}:/srv/hops/domains/domain1/docroot
scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes ../hopsworks-web/yo/bower.json vagrant@${SERVER}:/srv/hops/domains/domain1/docroot/app
scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes ../hopsworks-web/yo/.bowerrc vagrant@${SERVER}:/srv/hops/domains/domain1/docroot/app
ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes vagrant@${SERVER} "cd /srv/hops/domains/domain1/docroot/app && sudo npm install -g bower-npm-resolver && bower install && perl -pi -e \"s/getLocationBase\(\)/'http:\/\/${SERVER}:${WEBPORT}\/hopsworks'/g\" scripts/services/RequestInterceptorService.js"

