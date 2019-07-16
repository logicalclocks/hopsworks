#!/bin/bash
# Deploy the frontend to the glassfish home directory and run bower

set -e

if [ "$1" == "-help" ] ; then
    echo "usage: $0 [hostname][full_pathname_to_karamel-chef]"
    exit 1
fi     

usr=${USER}
vagrant_user=vagrant
SERVER="127.0.0.1"
dir="/home/$usr/NetBeansProjects/karamel-chef"
SSH_PORT=22


if [ $# -eq 1 ] ; then
  SERVER=$1
fi    

 
if [ $# -eq 2 ] ; then
  SERVER=$1
  dir=$2
fi    

if [ ! -f ${HOME}/.ssh/id_rsa.pub ] ; then
    echo "No ssh keypair found. "
    echo "Generating a passwordless ssh keypair with ssh-keygen at ${HOME}/.ssh/id_rsa(.pub)"
    ssh-keygen -b 2048 -f ${HOME}/.ssh/id_rsa -t rsa -q -N ''
    if [ $? -ne 0 ] ; then
        echo "Problem generating a passwordless ssh keypair with the following command:"
        echo "ssh-keygen -b 2048 -f ${HOME}/.ssh/id_rsa -t rsa -q -N ''"
        echo "Exiting with error."
        exit 12
    fi
fi

public_key=$(cat ${HOME}/.ssh/id_rsa.pub)

grep "$public_key" ${HOME}/.ssh/authorized_keys
if [ $? -ne 0 ] ; then
    echo "Enabling ssh into localhost (needed by Karamel)"
    echo "Adding ${usr} public key to ${HOME}/.ssh/authorized_keys"
    cat ${HOME}/.ssh/id_rsa.pub >> ${HOME}/.ssh/authorized_keys
fi

# 2. check if openssh server installed

echo "Check if openssh server installed and that ${usr} can ssh without a password into ${usr}@localhost"

ssh -p $SSH_PORT -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no ${usr}@localhost "echo 'ssh connected'"

if [ $? -ne 0 ] ; then
    echo "Ssh server needs to be running on localhost. Starting one..."
    sudo service ssh restart
    if [ $? -ne 0 ] ; then
        echo "Installing ssh server."
        sudo apt-get install openssh-server -y
        sleep 2
        sudo service ssh status
        if [ $? -ne 0 ] ; then
            echo "Error: could not install/start a ssh server. Install an openssh-server (or some other ssh server) and re-run this install script."
            echo "Exiting..."
            exit 2
        fi
    fi
    ssh -p $SSH_PORT -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no ${usr}@localhost "echo 'hello'"
    if [ $? -ne 0 ] ; then
      echo "Error: could not ssh to $usr@localhost"
      echo "You need to setup you machine so that $usr can ssh to localhost"
      echo "Exiting..."
      exit 3
    fi
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

ssh -o PasswordAuthentication=no -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -p $PORT vagrant@${SERVER} "cd ${basedir} && sudo chown -R ${vagrant_user}:${vagrant_user} docroot && sudo chmod -R 775 * &&  sudo chown -R ${vagrant_user}:${vagrant_user} ~/.local || echo 'no .local'"

#echo "ssh -o PasswordAuthentication=no -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -p $PORT vagrant@${SERVER} \"cd ${basedir} && sudo chmod -R 775 *\""

scp -o PasswordAuthentication=no -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -P ${PORT} -r ../hopsworks-web/yo/app/ vagrant@${SERVER}:${basedir}/docroot

scp -o PasswordAuthentication=no -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -P ${PORT} ../hopsworks-web/yo/bower.json vagrant@${SERVER}:${basedir}/docroot/app
scp -o PasswordAuthentication=no -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -P ${PORT} ../hopsworks-web/yo/package.json vagrant@${SERVER}:${basedir}/docroot/app
scp -o PasswordAuthentication=no -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -P ${PORT} ../hopsworks-web/yo/.bowerrc vagrant@${SERVER}:/srv/hops/domains/domain1/docroot/app

ssh -o PasswordAuthentication=no -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -p $PORT vagrant@${SERVER} "cd ${basedir}/docroot/app && sudo npm install && bower install"
#&& sudo chown vagrant /home/vagrant/.config 
#ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -p $PORT vagrant@${SERVER} "cd ${basedir}/docroot/app && perl -pi -e \"s/getApiLocationBase\(\)/'http:\/\/${SERVER}:${WEBPORT}\/hopsworks-api/api\/'/g\" scripts/services/RequestInterceptorService.js"

google-chrome -new-tab http://${SERVER}:$WEBPORT/app
