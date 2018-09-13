#!/bin/bash
set -e

ext=""
if [ ! $# -eq 1 ] ; then
  echo "JIM YOU ARE OVERWRITING THE MASTER BRANCH RELEASE. DO NOT DO IT"
  exit 1
fi

ext="-$1"

# Change to directory for the script
cd "$(dirname "$0")"
VERSION=`grep -o -a -m 1 -h -r "version>.*</version" ../pom.xml | head -1 | sed "s/version//g" | sed "s/>//" | sed "s/<\///g"`

#VERSION=`echo $VERSION | sed -e 's/-SNAPSHOT//'`


echo "Deploying ${VERSION}/hopsworks-ear${ext}.ear to http://snurran.sics.se/hops/hopsworks"
echo "Deploying ${VERSION}/hopsworks-web${ext}.war to http://snurran.sics.se/hops/hopsworks"
echo "Deploying ${VERSION}/hopsworks-ca${ext}.war to http://snurran.sics.se/hops/hopsworks"

ssh glassfish@snurran.sics.se "cd /var/www/hops/hopsworks ; mkdir -p ${VERSION}"
scp ../hopsworks-ear/target/hopsworks-ear.ear glassfish@snurran.sics.se:/var/www/hops/hopsworks/${VERSION}/hopsworks-ear${ext}.ear
scp ../hopsworks-web/target/hopsworks-web.war glassfish@snurran.sics.se:/var/www/hops/hopsworks/${VERSION}/hopsworks-web${ext}.war
scp ../hopsworks-ca/target/hopsworks-ca.war glassfish@snurran.sics.se:/var/www/hops/hopsworks/${VERSION}/hopsworks-ca${ext}.war

echo "Successfully Copied to http://snurran.sics.se/hops/hopsworks/${VERSION}/"
