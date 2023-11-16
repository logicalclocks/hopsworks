#!/bin/sh

set -e

"${PAYARA_DIR}"/bin/asadmin --user="${ADMIN_USER}" --passwordfile="${PASSWORD_FILE}" < /opt/payara/k8s/test-script
