#!/bin/bash

#This will be used for development only

mkdir /opt/payara/k8s/hopsworks

if [[ -n "${HOPSWORKS_EAR_DOWNLOAD_URL}" ]]; then
  wget --user="$HOPSWORKS_DOWNLOAD_USERNAME" --password="$HOPSWORKS_DOWNLOAD_PASSWORD" -O /opt/payara/k8s/hopsworks/hopsworks-ear.ear "$HOPSWORKS_EAR_DOWNLOAD_URL"
fi

if [[ -n "${HOPSWORKS_CA_DOWNLOAD_URL}" ]]; then
  wget --user="$HOPSWORKS_DOWNLOAD_USERNAME" --password="$HOPSWORKS_DOWNLOAD_PASSWORD" -O /opt/payara/k8s/hopsworks/hopsworks-ca.war "$HOPSWORKS_CA_DOWNLOAD_URL"
fi

if [[ -n "${HOPSWORKS_FRONT_DOWNLOAD_URL}" ]]; then
  wget --user="$HOPSWORKS_DOWNLOAD_USERNAME" --password="$HOPSWORKS_DOWNLOAD_PASSWORD" -O /opt/payara/k8s/hopsworks/frontend.tgz "$HOPSWORKS_FRONT_DOWNLOAD_URL"
  tar -xvzf /opt/payara/k8s/hopsworks/frontend.tgz -C "${PAYARA_DIR}"/glassfish/domains/domain1/docroot/
  rm -rf /opt/payara/k8s/hopsworks/frontend.tgz
fi