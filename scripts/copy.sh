#!/bin/bash

set -e
cp -fr ../hopsworks-web/yo/app/ /srv/hops/domains/domain1/docroot
cp -fr ../hopsworks-web/yo/bower.json /srv/hops/domains/domain1/docroot/app
pushd .
cd /srv/hops/domains/domain1/docroot/app 
bower install
#perl -pi -e \"s/getApiLocationBase\(\)/'http:\/\/localhost:8080\/hopsworks-api/api\/'/g\" scripts/services/RequestInterceptorService.js
popd
firefox --new-tab http://localhost:8080/app
