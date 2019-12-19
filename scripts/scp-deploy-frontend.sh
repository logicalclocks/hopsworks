#!/bin/bash
# Deploy the frontend to the glassfish home directory and run bower
export SERVER=vm
export LAST="/tmp/deploy-frontend-timestamp"

#run only once
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
  scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes ../hopsworks-web/yo/package.json vagrant@${SERVER}:/srv/hops/domains/domain1/docroot/app
fi
#ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes vagrant@${SERVER} "cd /srv/hops/domains/domain1/docroot/app && sudo npm install --production  && bower install"
touch $LAST

#curl -sL https://deb.nodesource.com/setup_8.x -o nodesource_setup.sh && sudo chmod +x nodesource_setup.sh && sudo ./nodesource_setup.sh && sudo apt-get install -y nodejs && sudo npm install -g phantomjs@2.1.1 --unsafe-perm && sudo npm install -g bower && sudo chown vagrant:vagrant -R /home/vagrant/.config
