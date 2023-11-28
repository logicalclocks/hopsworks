#!/bin/bash

set -e

MYSQL_CMD="mysql --host=${MYSQL_HOST} --user=${MYSQL_USER} --password=${MYSQL_PASSWORD}"

for filename in /flyway/updates/sql/*.sql; do
  file=$(basename "${filename%.*}");
  dir=$(dirname "$filename")
  mv -f "$filename" "$dir/V${file}__initial_tables.sql"
done

echo "Running flyway migrate"
flyway -configFiles=config/flyway.conf -locations=filesystem:/flyway/updates/sql -validateOnMigrate=false migrate

echo "Running dml"
for version in dml/*.sql; do
  ${MYSQL_CMD} hopsworks < "$version"
done