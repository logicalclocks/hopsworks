#!/bin/bash

set -e
if [ ! -d ../../hopsworks-chef ] ; then

  echo "Run this script from the ./hopsworks/scripts directory."
  echo "You must checkout the project: 'hopshadoop/hopsworks-chef' into the same root directory as the 'hopsworks' project"
  echo "This script will then update the sql tables in the 'hopsworks-chef' project"
  exit 1
fi


pushd .
cd ../../hopsworks-chef
git checkout develop
git pull
popd

diff ../sql/hopsworks.sql ../../hopsworks-chef/templates/default/tables.sql.erb

if [ $? -eq 0 ] ; then
  echo "hopsworks.sql is update-to-date. No changes made."
  exit 0
fi


cp ../sql/hopsworks.sql ../../hopsworks-chef/templates/default/tables.sql.erb

pushd .
cd ../../hopsworks-chef
git commit -am "Updating hopsworks.sql"
git push
popd

echo "Updated sql tables"

exit 0
