#!/bin/bash

echo "AS_ADMIN_MASTERPASSWORD=${CURRENT_AS_ADMIN_MASTERPASSWORD}
AS_ADMIN_NEWMASTERPASSWORD=${AS_ADMIN_MASTERPASSWORD}" > /tmp/masterpwdfile

echo "AS_ADMIN_PASSWORD=admin
AS_ADMIN_NEWPASSWORD=${ADMIN_PASSWORD}" > /tmp/adminpwdfile

${PAYARA_DIR}/bin/asadmin --passwordfile=/tmp/masterpwdfile change-master-password --savemasterpassword
echo "AS_ADMIN_MASTERPASSWORD=${AS_ADMIN_MASTERPASSWORD}" >> "$PASSWORD_FILE"
${PAYARA_DIR}/bin/asadmin --user="${ADMIN_USER}" --passwordfile="${PASSWORD_FILE}" start-domain "$DOMAIN_NAME"
${PAYARA_DIR}/bin/asadmin --user="${ADMIN_USER}" --passwordfile=/tmp/adminpwdfile change-admin-password

echo "AS_ADMIN_PASSWORD=${ADMIN_PASSWORD}" > "$PASSWORD_FILE"

${PAYARA_DIR}/bin/asadmin --user="${ADMIN_USER}" --passwordfile="${PASSWORD_FILE}" enable-secure-admin
${PAYARA_DIR}/bin/asadmin --user="${ADMIN_USER}" --passwordfile="${PASSWORD_FILE}" stop-domain "$DOMAIN_NAME"

rm -rf /tmp/masterpwdfile
rm -rf /tmp/adminpwdfile