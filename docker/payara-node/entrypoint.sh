#!/usr/bin/env bash

ASADMIN="${PAYARA_DIR}/bin/asadmin"
ASADMIN_COMMAND="${ASADMIN} -I false -T -a -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} list-deployment-groups"

while true ;
do
    ${ASADMIN_COMMAND} | grep "${PAYARA_DEPLOYMENT_GROUP}"; result=$?
    echo "DEBUG: list-deployment-groups result = $result"
    if [ $result -eq 0 ] ; then
        echo "DAS node is up and created deployment group."
        break
    fi
    sleep 2
    echo "Waiting for DAS node"
done

set -e

"${SCRIPT_DIR}"/register.sh
