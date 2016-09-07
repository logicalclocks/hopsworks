#!/bin/bash
set -e

# Change to directory for the script
cd "$(dirname "$0")"
VERSION=`grep -o -a -m 1 -h -r "version>.*</version" ../pom.xml | head -1 | sed "s/version//g" | sed "s/>//" | sed "s/<\///g"`

echo "Deploying version ${VERSION} of hopsworks.war to http://snurran.sics.se/hops/hopsworks-${VERSION}.war"
scp ../target/hopsworks.war glassfish@snurran.sics.se:/var/www/hops/hopsworks-${VERSION}.war
