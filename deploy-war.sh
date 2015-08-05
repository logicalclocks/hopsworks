#!/bin/bash
set -e
mvn clean install

cp hopsworks.sql ../hopsworks-chef/templates/default/tables.sql.erb
cp rows.sql ../hopsworks-chef/templates/default/rows.sql.erb
cd ../hopsworks-chef
git commit -am "Updating schema"
git push
cd ../hopsworks

scp target/hopsworks.war glassfish@snurran.sics.se:/var/www/hops/
