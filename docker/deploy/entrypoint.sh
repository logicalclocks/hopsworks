#!/usr/bin/env bash

set -e

echo AS_ADMIN_PASSWORD="$AS_ADMIN_PASSWORD" > "$PAYARA_PASSWORD_FILE"

ASADMIN="${PAYARA_DIR}/bin/asadmin"
ASADMIN_COMMAND="${ASADMIN} -I false -T -a -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE}"

_log () {
  if { [ "$1" = "DEBUG" ] && [ "$DEBUG" = true ]; } || [[ $1 =~ INFO|WARNING  ]] ; then
    echo "$(date '+%Y-%m-%d %H:%M:%S') $1 -- $2"
  fi
}

_is_admin_ready() {
  ${ASADMIN_COMMAND} list-configs &>/dev/null
}

_check_if_application_exists() {
  local application_name=$1
  local application_exists
  application_exists=$(${ASADMIN_COMMAND} list-applications "${PAYARA_DEPLOYMENT_GROUP}" | grep -c "${application_name}")
  if [[ "${application_exists}" -eq 1 ]]; then
    _log INFO "Application ${application_name} already exists"
    return 1
  fi
  return 0
}

_check_instances_running() {
  local desired_instances=${1:-$NUM_WORKERS}
  local running_instances
  running_instances=$(${ASADMIN_COMMAND} list-instances "${PAYARA_DEPLOYMENT_GROUP}" | grep running | awk '!/not/' | wc -l)
  _log INFO "check if instances are running. running=$running_instances desired=$desired_instances"

  [ "$running_instances" -ge "$desired_instances" ]
}

_instances_not_registered() {
  local worker_pods
  local ready_instances
  local registered
  worker_pods=$(kubectl get pod -l app="$WORKER_DEPLOYMENT_NAME" -n "$NAMESPACE" -o 'jsonpath={..status.conditions[?(@.type=="Ready")].status}')
  # shellcheck disable=SC2206
  ready_instances=( ${worker_pods//False/} )
  registered=$(${ASADMIN_COMMAND} list-instances "${PAYARA_DEPLOYMENT_GROUP}" | grep -c running)
  [ ${#ready_instances[@]} -gt 0 ] && [ "$registered" -eq 0 ]
}

_deploy() {
  if _check_if_application_exists "hopsworks-ca:${HOPSWORKS_VERSION}"; then
    _log INFO "Deploying hopsworks-ca:${HOPSWORKS_VERSION} to ${PAYARA_DEPLOYMENT_GROUP}"
    ${ASADMIN_COMMAND} --echo=true deploy --name hopsworks-ca:"${HOPSWORKS_VERSION}" --target "${PAYARA_DEPLOYMENT_GROUP}" --force=true --asyncreplication=false --keepstate=false --contextroot /hopsworks-ca  /opt/payara/k8s/hopsworks/hopsworks-ca.war
  fi

  if _check_if_application_exists "hopsworks-ear:${HOPSWORKS_VERSION}"; then
    _log INFO "Deploying hopsworks-ear:${HOPSWORKS_VERSION} to ${PAYARA_DEPLOYMENT_GROUP}"
    ${ASADMIN_COMMAND} --echo=true deploy --name hopsworks-ear:"${HOPSWORKS_VERSION}" --target "${PAYARA_DEPLOYMENT_GROUP}" --force=true --asyncreplication=false --keepstate=false /opt/payara/k8s/hopsworks/hopsworks-ear.ear
  fi
}

_check_num_admin() {
  local admin_pods
  admin_pods=$(kubectl get pod -l app="$ADMIN_DEPLOYMENT_NAME" -n "$NAMESPACE" --no-headers | wc -l)
  _log DEBUG "num admin pods=$admin_pods"
  [ "$admin_pods" -eq "$NUM_ADMINS" ]
}

_wait_for_admin() {
  set +e
  while true  ;
  do
    #wait until only one admin is ready
    if _check_num_admin && _is_admin_ready ; then
      _log INFO "DAS is up and ready!"
      break
    fi
    sleep 10
    _log INFO "Waiting for the DAS to be ready..."
  done
  set -e
}

_wait_for_instances() {
  local num_workers_to_wait_on=$1
  local endTime
  local wait_sec=${240:-$REGISTER_WAIT_SEC}
  set +e
  endTime=$(( $(date +%s) + wait_sec ))
  while true;
  do
      if _check_instances_running "$num_workers_to_wait_on" ; then
          _log INFO "All requested instances are running!"
          sleep "$INITIAL_DELAY_SEC" # give the instances time to start
          break
      fi

      if [ "$(date +%s)" -gt $endTime ]; then
        _log WARNING "Timeout waiting for $num_workers_to_wait_on instances to start"
        break
      fi

      sleep 2
      _log INFO "Waiting for $num_workers_to_wait_on instances to start"
  done
  set -e
}

_check_and_deploy() {
  #This will wait until only one admin is ready
  _wait_for_admin

  #one instance is enough to start deploying
  _wait_for_instances 1

  # If no instance registered and there are ready worker pods then restart them
  # If upgrade instances are recreated and registered.
  if _instances_not_registered ; then
    _log DEBUG "Restarting deployment $WORKER_DEPLOYMENT_NAME ..."
    kubectl rollout restart deployment "$WORKER_DEPLOYMENT_NAME" -n "$NAMESPACE"
    sleep 10
    _wait_for_instances 1
  fi

  if ! _check_instances_running 1 ; then
    _log WARNING "No running instance found."
    exit 1
  fi

  _deploy
}

_cleanup_stopped_instances() {
  set +e
  local stopped_instances
  #Can only do cleanup if we know all instances are running and there are stopped instances.
  if _check_instances_running ; then
    # shellcheck disable=SC2207
    stopped_instances=( $(${ASADMIN_COMMAND} list-instances "${PAYARA_DEPLOYMENT_GROUP}" | grep 'not running' | awk '{print $1}') )
    exit_code=$?
    if [[ $exit_code -eq 0 && ${#stopped_instances[@]} -gt 0 ]] ; then
      _log INFO "Not running instances ${stopped_instances[*]}"
      for i in "${stopped_instances[@]}"
      do
        _log INFO "Deleting instance $i"
        ${ASADMIN_COMMAND} delete-instance "$i"
      done
    fi
  fi
  set -e
}

_create_api_key() {
    _log INFO "Current namespace: $NAMESPACE"

    secret_name=$1
    api_key_scopes=$2
    _log INFO "Creating Hopsworks API key with scopes $api_key_scopes in Secret $secret_name"
    set +e
    kubectl --namespace "${NAMESPACE}" get secret "${secret_name}" > /dev/null 2>&1
    exists=$?
    set -e

    # Name must be at most 45 characters
    api_key_name="${secret_name}_$(tr -dc A-Za-z0-9 </dev/urandom | head -c 5)"
    if [[ "$exists" -eq 1 ]]; then
        _log INFO "Creating Hopsworks API key $api_key_name"
        auth=$(curl --fail -s -k -i -XPOST --data "email=$AGENT_USER&password=$AGENT_PASSWORD" https://"${HOPSWORKS_SVC_NAME}"."${NAMESPACE}".svc.cluster.local:"${HOPSWORKS_PORT}"/hopsworks-api/api/auth/service | grep -i '^Authorization' | cut -d: -f2-)
        api_key=$(curl --fail -s -k -XPOST -H "Authorization: $auth" -G -d "name=$api_key_name" -d "scope=$api_key_scopes" https://"${HOPSWORKS_SVC_NAME}"."${NAMESPACE}".svc.cluster.local:"${HOPSWORKS_PORT}"/hopsworks-api/api/users/apiKey | jq -r ".key")
        # fail if api_key is empty
        if [[ -z "$api_key" ]]; then
            _log WARNING "Failed to create API key $api_key_name"
            exit 1
        fi
        kubectl --namespace "${NAMESPACE}" create secret generic "${secret_name}" --from-literal=key="$api_key"
        _log INFO "API key $api_key_name has been created in Secret ${secret_name}"
    else
        _log INFO "Secret $secret_name already exists. Skip creation."
    fi
}

_check_and_deploy

_create_api_key "hopsworks-api-key-auth" "AUTH"

while true ;
do
  _cleanup_stopped_instances
  sleep "$CLEANUP_INTERVAL_SEC"
done