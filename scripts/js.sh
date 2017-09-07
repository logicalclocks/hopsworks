#!/bin/bash
# Deploy the frontend to the glassfish home directory and run bower

set -e

if [ "$1" == "-help" ] ; then
    echo "usage: $0 [hostname]"
    exit 1
fi     

usr=${USER}
vagrant_user=vagrant
SERVER="127.0.0.1"
dir="/home/$usr/NetBeansProjects/karamel-chef"

if [ $# -eq 1 ] ; then
  SERVER=$1
fi    

stuff=`ssh ${usr}@${SERVER} "cd $dir && ./run.sh ports | grep ^22 "`
vagrant_port=`echo $stuff | sed -e 's/22 -> //'`

webbie=`ssh ${usr}@${SERVER} "cd $dir && ./run.sh ports | grep ^8080 "`
web_port=`echo $webbie | sed -e 's/8080 -> //'`

echo "vagrant port: $vagrant_port"
echo "web port: $web_port"

export PORT=${vagrant_port}
export WEBPORT=${web_port}
export SERVER=$SERVER
export key=insecure_private_key
export usr=$usr

basedir=/srv/hops/domains/domain1

scp ${usr}@${SERVER}:/home/${usr}/.vagrant.d/${key} .

#echo "ssh -o PasswordAuthentication=no -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -p $PORT vagrant@${SERVER} \"cd ${basedir} && sudo chmod -R 775 *\""
ssh -o PasswordAuthentication=no -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -p $PORT vagrant@${SERVER} "sudo chown -R ${vagrant_user} ~/.config && cd ${basedir} && sudo chown -R ${vagrant_user}:${vagrant_user} docroot && sudo chmod -R 775 * && sudo chown -R ${vagrant_user}:${vagrant_user} ~/.local"


scp -o PasswordAuthentication=no -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -P ${PORT} -r ../hopsworks-web/yo/app/ vagrant@${SERVER}:${basedir}/docroot

scp -o PasswordAuthentication=no -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -P ${PORT} ../hopsworks-web/yo/bower.json vagrant@${SERVER}:${basedir}/docroot/app

ssh -o PasswordAuthentication=no -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -p $PORT vagrant@${SERVER} "cd ${basedir}/docroot/app && bower install"

#ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -p $PORT vagrant@${SERVER} "cd ${basedir}/docroot/app && perl -pi -e \"s/getApiLocationBase\(\)/'http:\/\/${SERVER}:${WEBPORT}\/hopsworks-api/api\/'/g\" scripts/services/RequestInterceptorService.js"

google-chrome -new-tab http://${SERVER}:$WEBPORT/app
