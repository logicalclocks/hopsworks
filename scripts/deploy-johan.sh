#!/bin/bash
set -e

#cp ../sql/hopsworks.sql ../../hopsworks-chef/templates/default/tables.sql.erb
#cp ../sql/rows.sql ../../hopsworks-chef/templates/default/rows.sql.erb

scp ../target/hopsworks.war glassfish@snurran.sics.se:/var/www/hops/hopsworks-johan.war
