#!/bin/bash
# Deploy the frontend to the glassfish home directory and run bower
export PORT=9191
export HOST=snurran.sics.se
echo "copy rom .kitchen/kitchen-vagrant/default-centos-70/.vagrant/machines/default/virtualbox/private_key  to this directory"

scp -r ../yo/app -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i ./private_key vagrant@${HOST}:${PORT}/srv/glassfish/domain1/docroot

ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i ./private_key -p $PORT vagrant@${HOST} "cd /srv/glassfish/domain1/docroot && bower install && perl -pi -e \"s/bower_components/..\/bower_components/g\" index.html && perl -pi -e \"s/getLocationBase\(\)/'http:\/\/${HOST}:8080\/hopsworks'/g\" scripts/services/RequestInterceptorService.js"

google-chrome -new-tab http://localhost:8080/app
