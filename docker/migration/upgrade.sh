#!/bin/bash

MYSQL_CMD="mysql --host=${MYSQL_HOST} --user=${MYSQL_USER} --password=${MYSQL_PASSWORD}"

until "${MYSQL_CMD}" --execute="SELECT 1;"; do
 echo 'waiting for mysql'
 sleep 2
done

set -e

echo "Running flyway migrate"
flyway -configFiles=config/flyway.conf -validateOnMigrate=false migrate

echo "Running dml"
for version in dml/*.sql; do
  "${MYSQL_CMD}" hopsworks < "$version"
done