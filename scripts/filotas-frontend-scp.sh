#!/bin/bash
# Deploy the frontend to the glassfish home directory and run bower
export PORT=24752
export WEBPORT=8080
export SERVER=bbc2.sics.se
export key=./insecure_private_key
usr=filotas
basedir=/srv/hops/domains/domain1

scp ${usr}@${SERVER}:/home/${usr}/.vagrant.d/${key} .

ssh -o PasswordAuthentication=no -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -o ProxyCommand='nc -x 127.0.0.1:4444 %h %p' -i $key -p $PORT vagrant@${SERVER} "cd ${basedir} && sudo chown -R ${vagrant_user}:${vagrant_user} docroot && sudo chmod -R 775 * && sudo chown -R ${vagrant_user}:${vagrant_user} ~/.local"


scp -o PasswordAuthentication=no -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -o ProxyCommand='nc -x 127.0.0.1:4444 %h %p' -i $key -P ${PORT} -r ../hopsworks-web/yo/app/ vagrant@${SERVER}:${basedir}/docroot

scp -o PasswordAuthentication=no -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -o ProxyCommand='nc -x 127.0.0.1:4444 %h %p' -i $key -P ${PORT} ../hopsworks-web/yo/bower.json vagrant@${SERVER}:${basedir}/docroot/app

ssh -o PasswordAuthentication=no -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -o ProxyCommand='nc -x 127.0.0.1:4444 %h %p' -i $key -p $PORT vagrant@${SERVER} "cd ${basedir}/docroot/app && bower install"
