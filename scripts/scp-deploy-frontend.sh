#!/bin/bash
# Deploy the frontend to the glassfish home directory and run bower
export SERVER=vm
export LAST="/tmp/deploy-frontend-timestamp"
ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes vagrant@${SERVER} "cd /srv/hops/domains/domain1 && sudo chown -R glassfish:vagrant docroot && sudo chmod -R 775 *"
if [ -f "$LAST" ]
then
  for file in $(find ../hopsworks-web/yo/app/ -newer $LAST -type f)
  do
    FILE_PATH=/srv/hops/domains/domain1/docroot$(echo $file | awk -F 'yo' '{print $2}')
    scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes $file vagrant@${SERVER}:$FILE_PATH
  done
  for file in $(find ../hopsworks-web/yo/bower.json ../hopsworks-web/yo/.bowerrc -newer $LAST -type f)
  do
    FILE_PATH=/srv/hops/domains/domain1/docroot/app$(echo $file | awk -F 'yo' '{print $2}')
    scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes $FILE vagrant@${SERVER}:$FILE_PATH
  done
else
  scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -r ../hopsworks-web/yo/app/ vagrant@${SERVER}:/srv/hops/domains/domain1/docroot
  scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes ../hopsworks-web/yo/bower.json vagrant@${SERVER}:/srv/hops/domains/domain1/docroot/app
  scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes ../hopsworks-web/yo/.bowerrc vagrant@${SERVER}:/srv/hops/domains/domain1/docroot/app
fi
ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes vagrant@${SERVER} "cd /srv/hops/domains/domain1/docroot/app && sudo npm install && bower install"
touch $LAST
