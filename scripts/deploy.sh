#!/bin/bash
set -e

# Change to directory for the script
cd "$(dirname "$0")"
VERSION=`grep -o -a -m 1 -h -r "version>.*</version" ../pom.xml | head -1 | sed "s/version//g" | sed "s/>//" | sed "s/<\///g"`

echo "Deploying versions ${VERSION} of hopsworks war and ear files to http://snurran.sics.se/hops/"

ssh glassfish@snurran.sics.se "cd /var/www/hops/hopsworks ; mkdir -p ${VERSION}"
scp ../hopsworks-ear/target/hopsworks-ear.ear glassfish@snurran.sics.se:/var/www/hops/hopsworks/${VERSION}/hopsworks-ear.ear
scp ../hopsworks-web/target/hopsworks-web.war glassfish@snurran.sics.se:/var/www/hops/hopsworks/${VERSION}/hopsworks.war

