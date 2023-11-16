#!/bin/bash

MYSQL_CMD="mysql --host=${MYSQL_HOST} --user=${MYSQL_USER} --password=${MYSQL_PASSWORD}"

until ${MYSQL_CMD} --execute="SELECT 1;"; do
 echo 'waiting for mysql' 
 sleep 2
done

set -e

#Drop databases
${MYSQL_CMD} --execute="DROP DATABASE IF EXISTS hopsworks;"
${MYSQL_CMD} --execute="DROP DATABASE IF EXISTS glassfish_timers;"
${MYSQL_CMD} --execute="DROP DATABASE IF EXISTS airflow;"
