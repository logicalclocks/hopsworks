#!/bin/bash

ASADMIN="${PAYARA_DIR}/bin/asadmin"
ASADMIN_COMMAND="${ASADMIN} -I false -T -a -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} ${INSTANCE_STARTUP_CHECK}"

### Deploy ###
echo "Deploying hopsworks to ${PAYARA_DEPLOYMENT_GROUP}"
ASADMIN_COMMAND="${ASADMIN} -I false -T -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} --echo=true deploy --name hopsworks-ca:${HOPSWORKS_VERSION} --target ${PAYARA_DEPLOYMENT_GROUP} --force=true --precompilejsp=true --asyncreplication=false --contextroot /hopsworks-ca --keepstate=false --upload=false /opt/payara/k8s/hopsworks/hopsworks-ca.war"
${ASADMIN_COMMAND}

ASADMIN_COMMAND="${ASADMIN} -I false -T -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} --echo=true deploy --name hopsworks-ear:${HOPSWORKS_VERSION} --target ${PAYARA_DEPLOYMENT_GROUP} --force=true --precompilejsp=true --asyncreplication=false --keepstate=false --upload=false /opt/payara/k8s/hopsworks/hopsworks-ear.ear"
${ASADMIN_COMMAND}




