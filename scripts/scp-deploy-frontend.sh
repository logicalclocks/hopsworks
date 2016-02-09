#!/bin/bash
# Deploy the frontend to the glassfish home directory and run bower
PORT=2222
cd ..


scp -r yo/app -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i /home/jdowling/NetBeansProjects/hopsworks-chef/.kitchen/kitchen-vagrant/default-centos-70/.vagrant/machines/default/virtualbox/private_key vagrant@127.0.0.1:${PORT}/srv/glassfish/domain1/docroot
scp -r yo/bower.json -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i /home/jdowling/NetBeansProjects/hopsworks-chef/.kitchen/kitchen-vagrant/default-centos-70/.vagrant/machines/default/virtualbox/private_key vagrant@127.0.0.1:${PORT}/srv/glassfish/domain1/docroot

ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i /home/jdowling/NetBeansProjects/hopsworks-chef/.kitchen/kitchen-vagrant/default-centos-70/.vagrant/machines/default/virtualbox/private_key -p $PORT vagrant@127.0.0.1 "cd /srv/glassfish/domain1/docroot && bower install && perl -pi -e \"s/bower_components/..\/bower_components/g\" index.html && perl -pi -e \"s/getLocationBase\(\)/'http:\/\/localhost:8080\/hopsworks'/g\" scripts/services/RequestInterceptorService.js"

google-chrome -new-tab http://localhost:8080/app
