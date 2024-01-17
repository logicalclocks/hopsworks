#!/bin/bash

set -e

MYSQL_CMD="mysql --host=${MYSQL_HOST} --user=${MYSQL_USER} --password=${MYSQL_PASSWORD}"

echo "${GRANT_HOST:='%'}"
#create database
echo "Creating ${HOPSWORKS_DB} database"
${MYSQL_CMD} --execute="CREATE DATABASE IF NOT EXISTS ${HOPSWORKS_DB} CHARACTER SET latin1;"
echo "Creating glassfish_timers database"
${MYSQL_CMD} --execute="CREATE DATABASE IF NOT EXISTS glassfish_timers;"
echo "Creating user '${HOPSWORKS_MYSQL_USER}'@'${GRANT_HOST}'"
${MYSQL_CMD} --execute="CREATE USER IF NOT EXISTS '${HOPSWORKS_MYSQL_USER}'@'${GRANT_HOST}' IDENTIFIED BY '${HOPSWORKS_MYSQL_PASSWORD}';"
echo "Creating glassfish_timers table"
${MYSQL_CMD} glassfish_timers < glassfish_timers.sql
echo "GRANT ALL PRIVILEGES ON glassfish_timers.* TO '${HOPSWORKS_MYSQL_USER}'@'${GRANT_HOST}'"
${MYSQL_CMD} --execute="GRANT ALL PRIVILEGES ON glassfish_timers.* TO '${HOPSWORKS_MYSQL_USER}'@'${GRANT_HOST}';"

if [[ -n "${AIRFLOW_DB}" ]]; then
  echo "Creating ${AIRFLOW_DB} database"
  ${MYSQL_CMD} --execute="CREATE DATABASE IF NOT EXISTS ${AIRFLOW_DB} CHARACTER SET latin1;"
  echo "Creating user '${AIRFLOW_MYSQL_USER}'@'${GRANT_HOST}'"
  ${MYSQL_CMD} --execute="CREATE USER IF NOT EXISTS '${AIRFLOW_MYSQL_USER}'@'${GRANT_HOST}' IDENTIFIED WITH mysql_native_password BY '${AIRFLOW_MYSQL_PASSWORD}'"
  echo "GRANT NDB_STORED_USER ON *.* TO '${AIRFLOW_MYSQL_USER}'@'${GRANT_HOST}'"
  ${MYSQL_CMD} --execute="GRANT NDB_STORED_USER ON *.* TO '${AIRFLOW_MYSQL_USER}'@'${GRANT_HOST}';"
  echo "GRANT ALL PRIVILEGES ON ${AIRFLOW_DB}.* TO '${AIRFLOW_MYSQL_USER}'@'${GRANT_HOST}'"
  ${MYSQL_CMD} --execute="GRANT ALL PRIVILEGES ON ${AIRFLOW_DB}.* TO '${AIRFLOW_MYSQL_USER}'@'${GRANT_HOST}'"
fi

echo "Running flyway migrate"
flyway -configFiles=config/flyway.conf -validateOnMigrate=false migrate

echo "Running dml"
for version in dml/*.sql; do
  ${MYSQL_CMD} hopsworks < "$version"
done