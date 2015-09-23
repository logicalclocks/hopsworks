#!/bin/bash

schema=../sql/hopsworks.sql

perl -pi -e "s/.*DEFINER=\`\w.*//g" schema 
perl -pi -e "s/InnoDB/NDBCLUSTER/g" schema 
perl -pi -e "s/AUTO_INCREMENT=[0-9]*\b//g" schema 

cp schema ../../hopsworks-chef/templates/default/tables.sql.erb
