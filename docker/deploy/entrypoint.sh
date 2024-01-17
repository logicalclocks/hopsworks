#!/usr/bin/env bash
set -e

ASADMIN="${PAYARA_DIR}/bin/asadmin"
ASADMIN_COMMAND="${ASADMIN} -I false -T -a -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} ${INSTANCE_STARTUP_CHECK}"

### Deploy ###
echo "Deploying hopsworks to ${PAYARA_DEPLOYMENT_GROUP}"
ASADMIN_COMMAND="${ASADMIN} -I false -T -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} --echo=true deploy --name hopsworks-ca:${HOPSWORKS_VERSION} --target ${PAYARA_DEPLOYMENT_GROUP} --force=true --precompilejsp=true --asyncreplication=false --contextroot /hopsworks-ca --keepstate=false --upload=false /opt/payara/k8s/hopsworks/hopsworks-ca.war"
${ASADMIN_COMMAND}

ASADMIN_COMMAND="${ASADMIN} -I false -T -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} --echo=true deploy --name hopsworks-ear:${HOPSWORKS_VERSION} --target ${PAYARA_DEPLOYMENT_GROUP} --force=true --precompilejsp=true --asyncreplication=false --keepstate=false --upload=false /opt/payara/k8s/hopsworks/hopsworks-ear.ear"
${ASADMIN_COMMAND}

_create_api_key() {
    NAMESPACE=$(cat /var/run/secrets/kubernetes.io/serviceaccount/namespace)
    echo "Current namespace: $NAMESPACE"

    secret_name=$1
    api_key_scopes=$2
    echo "Creating Hopsworks API key with scopes $api_key_scopes in Secret $secret_name"
    set +e
    kubectl --namespace ${NAMESPACE} get secret ${secret_name} > /dev/null 2>&1
    exists=$?
    set -e

    # Name must be at most 45 characters
    api_key_name="${secret_name}_$(tr -dc A-Za-z0-9 </dev/urandom | head -c 5)"
    if [[ "$exists" -eq 1 ]]; then
        echo "Creating Hopsworks API key $api_key_name"
        auth=$(curl --fail -s -k -i -XPOST --data "email=$AGENT_USER&password=$AGENT_PASSWORD" https://${HOPSWORKS_SVC_NAME}.${NAMESPACE}.svc.cluster.local:${HOPSWORKS_PORT}/hopsworks-api/api/auth/service | grep -i ^Authorization | cut -d: -f2-)
        api_key=$(curl --fail -s -k -XPOST -H "Authorization: $auth" -G -d "name=$api_key_name" -d "scope=$api_key_scopes" https://${HOPSWORKS_SVC_NAME}.${NAMESPACE}.svc.cluster.local:${HOPSWORKS_PORT}/hopsworks-api/api/users/apiKey | jq -r ".key")
        kubectl --namespace ${NAMESPACE} create secret generic ${secret_name} --from-literal=key=$api_key
        echo "API key $api_key_name has been created in Secret ${secret_name}"
    else
        echo "Secret $secret_name already exists. Skip creation."
    fi
}

_create_api_key "hopsworks-api-key-auth" "AUTH"
