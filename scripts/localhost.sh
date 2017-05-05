#!/bin/bash
# Deploy the frontend to the glassfish home directory and run bower

if [ !-d ../../karamel-chef ] ; then
 echo "Error. You must first have 'karamel-chef' installed in the same base directory as this 'hopsworks' project"
 echo ""
 exit 1
fi

vagrant_port=`../../karamel-chef/run.sh ports | grep 22 | sed -e 's/22 -> //'`
web_port=`../../karamel-chef/run.sh ports | grep 8080 | sed -e 's/8080 -> //'`
export PORT=${vagrant_port}
export WEBPORT=${web_port}
export SERVER=localhost
export key=private_key
usr=${USER}
basedir=/srv/hops/domains/domain1

scp ${usr}@${SERVER}:/home/${usr}/.vagrant.d/insecure_private_key .

ssh -o UserKnownHostFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -p $PORT vagrant@${SERVER} "cd ${basedir} && sudo chown -R glassfish:vagrant docroot && sudo chmod -R 775 *"

scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -P ${PORT} -r ../hopsworks-web/yo/app/ vagrant@${SERVER}:${basedir}/docroot
scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -P ${PORT} ../hopsworks-web/yo/bower.json vagrant@${SERVER}:${basedir}/docroot/app

ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -p $PORT vagrant@${SERVER} "cd ${basedir}/docroot/app && bower install"

#ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -p $PORT vagrant@${SERVER} "cd ${basedir}/docroot/app && perl -pi -e \"s/getApiLocationBase\(\)/'http:\/\/${SERVER}:${WEBPORT}\/hopsworks-api/api\/'/g\" scripts/services/RequestInterceptorService.js"

google-chrome -new-tab http://${SERVER}:$WEBPORT/app
