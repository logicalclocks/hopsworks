#!/bin/bash

ASADMIN="${PAYARA_DIR}/bin/asadmin"

#CHECK can be
#list-hazelcast-cluster-members | grep 5900" <- this will take more time
#list-instances ${PAYARA_DEPLOYMENT_GROUP} | grep running | awk '!/not/'"
CHECK='list-instances ${PAYARA_DEPLOYMENT_GROUP} | grep running | awk "!/not/"'
echo "${INSTANCE_STARTUP_CHECK:=$CHECK}"

ASADMIN_COMMAND="${ASADMIN} -I false -T -a -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} ${INSTANCE_STARTUP_CHECK}"

while true ;
do
    result="$(eval "${ASADMIN_COMMAND}" | wc -l)"
    exit_code=$?
    echo "DEBUG: Command result = $result"
    if [[ $exit_code -eq 0 && $result -eq $REPLICA_COUNT ]] ; then
        echo "All instances are running!"
        sleep "${INSTANCE_STARTUP_DELAY}" # give the instances time to start
        break
    fi
    sleep 2
    echo "Waiting for instances to start"
done

### Deploy ###
echo "Deploying hopsworks to ${PAYARA_DEPLOYMENT_GROUP}"
ASADMIN_COMMAND="${ASADMIN} -I false -T -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} --echo=true deploy --name hopsworks-ca:${HOPSWORKS_VERSION} --target ${PAYARA_DEPLOYMENT_GROUP} --force=true --precompilejsp=true --asyncreplication=false --contextroot /hopsworks-ca --keepstate=false --upload=false /opt/payara/k8s/hopsworks/hopsworks-ca.war"
${ASADMIN_COMMAND}

ASADMIN_COMMAND="${ASADMIN} -I false -T -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} --echo=true deploy --name hopsworks-ear:${HOPSWORKS_VERSION} --target ${PAYARA_DEPLOYMENT_GROUP} --force=true --precompilejsp=true --asyncreplication=false --keepstate=false --upload=false /opt/payara/k8s/hopsworks/hopsworks-ear.ear"
${ASADMIN_COMMAND}




