#!/bin/bash

set -e

MYSQL_CMD="mysql --host=${MYSQL_HOST} --user=${MYSQL_USER} --password=${MYSQL_PASSWORD}"

#Drop databases
${MYSQL_CMD} --execute="DROP DATABASE IF EXISTS hopsworks;"
${MYSQL_CMD} --execute="DROP DATABASE IF EXISTS glassfish_timers;"
${MYSQL_CMD} --execute="DROP DATABASE IF EXISTS airflow;"
