#!/bin/bash
set -e

ext=""
if [ $# -eq 1 ] ; then
  ext="-$1"
fi

# Change to directory for the script
cd "$(dirname "$0")"
VERSION=`grep -o -a -m 1 -h -r "version>.*</version" ../pom.xml | head -1 | sed "s/version//g" | sed "s/>//" | sed "s/<\///g"`

echo "Deploying versions ${VERSION}${ext} of hopsworks war and ear files to http://snurran.sics.se/hops/"

ssh glassfish@snurran.sics.se "cd /var/www/hops/hopsworks ; mkdir -p ${VERSION}"
scp ../hopsworks-ear/target/hopsworks-ear.ear glassfish@snurran.sics.se:/var/www/hops/hopsworks/${VERSION}/hopsworks-ear${ext}.ear
scp ../hopsworks-web/target/hopsworks-web.war glassfish@snurran.sics.se:/var/www/hops/hopsworks/${VERSION}/hopsworks${ext}.war
scp ../hopsworks-ca/target/hopsworks-ca.war glassfish@snurran.sics.se:/var/www/hops/hopsworks/${VERSION}/hopsworks-ca${ext}.war

echo "Successfully Copied: ${ext} to http://snurran.sics.se/hops/"
