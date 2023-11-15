FROM payara/server-full:5.2022.5

ENV DG_NAME="hopsworks-dg" \
CONFIG="hopsworks-config"

USER root
RUN apt-get update && apt-get install xz-utils && apt-get install wget -y && apt install dnsutils -y

USER payara
ENV POSTDEPLOY_COMMANDS=/opt/payara/config/post-deploy-commands.asadmin POSTDEPLOY_COMMANDS_FINAL=/opt/payara/config/post-deploy-commands-final.asadmin
RUN ${PAYARA_DIR}/bin/asadmin start-domain ${DOMAIN_NAME} && \
 for MEMORY_JVM_OPTION in $(${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} list-jvm-options --target default-config | grep "Xm[sx]\|Xss"); do \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} delete-jvm-options --target default-config $MEMORY_JVM_OPTION; \
 done && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} create-jvm-options --target default-config '-XX\:+UseContainerSupport:-XX\:MaxRAMPercentage=${ENV=MEM_MAX_RAM_PERCENTAGE}:-Xss${ENV=MEM_XSS}' && \
 ${PAYARA_DIR}/bin/asadmin copy-config --passwordfile=${PASSWORD_FILE} default-config $CONFIG && \
 ${PAYARA_DIR}/bin/asadmin create-deployment-group --passwordfile=${PASSWORD_FILE} $DG_NAME && \
 #${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} set-hazelcast-configuration --clustermode kubernetes --kubernetesServiceName hopsworks --kubernetesNamespace hopsworks && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} set-healthcheck-configuration --enabled=true --dynamic=true && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} set-log-attributes --target server-config com.sun.enterprise.server.logging.GFFileHandler.logtoFile=true && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} set-log-attributes --target ${CONFIG} com.sun.enterprise.server.logging.GFFileHandler.logtoFile=true && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} set-monitoring-configuration --target ${CONFIG} --enabled=true --mbeansenabled=true --amxenabled=true --jmxlogfrequency=15 --jmxlogfrequencyunit=SECONDS --dynamic=true && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} set-metrics-configuration --target ${CONFIG} --enabled=true --dynamic=true && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} set-monitoring-level --target ${CONFIG} --module=jvm,connector-service,connector-connection-pool,jdbc-connection-pool,web-services-container,thread-pool,http-service,security,jersey,transaction-service,jpa,web-container --level=HIGH,HIGH,HIGH,HIGH,HIGH,HIGH,HIGH,HIGH,HIGH,HIGH,HIGH,HIGH && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} set configs.config.${CONFIG}.thread-pools.thread-pool.http-thread-pool.max-thread-pool-size=200 && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} set ${CONFIG}.http-service.virtual-server.server.property.send-error_1='code=404 path=${com.sun.aas.instanceRoot}/docroot/index.html reason=Resource_not_found' && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} create-managed-executor-service --target=${DG_NAME} --enabled=true --longrunningtasks=true --corepoolsize=50 --maximumpoolsize=400 --keepaliveseconds=60 --taskqueuecapacity=20000 --description "Hopsworks Conda Executor Service" concurrent/condaExecutorService && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} create-managed-executor-service --target=${DG_NAME} --enabled=true --longrunningtasks=true --corepoolsize=10 --maximumpoolsize=200 --keepaliveseconds=60 --taskqueuecapacity=10000 --description "Hopsworks Kagent Executor Service" concurrent/kagentExecutorService && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} create-managed-executor-service --target=${DG_NAME} --enabled=true --longrunningtasks=true --corepoolsize=50 --maximumpoolsize=400 --keepaliveseconds=60 --taskqueuecapacity=20000 --description "Hopsworks Executor Service" concurrent/hopsExecutorService && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} create-managed-executor-service --target=${DG_NAME} --enabled=true --longrunningtasks=true --corepoolsize=300 --maximumpoolsize=300 --threadpriority=8 --taskqueuecapacity=1000 --description "Hopsworks Jupyter Executor Service" concurrent/jupyterExecutorService && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} create-jdbc-connection-pool --restype javax.sql.DataSource --datasourceclassname com.mysql.cj.jdbc.MysqlDataSource --isconnectvalidatereq=true --validationmethod=auto-commit --description="Hopsworks Connection Pool" --property user=root:password=changeit:url="jdbc\:mysql\://mysql\:3306/":useSSL=false:allowPublicKeyRetrieval=true hopsworksPool && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} create-jdbc-resource --connectionpoolid hopsworksPool --description "Resource for Hopsworks Pool" jdbc/hopsworks && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} create-jdbc-connection-pool --restype javax.sql.XADataSource --datasourceclassname com.mysql.cj.jdbc.MysqlXADataSource --isconnectvalidatereq=true --validationmethod=auto-commit --description="Hopsworks EJB Connection Pool" --property user=root:password=changeit:url="jdbc\:mysql\://mysql\:3306/glassfish_timers":useSSL=false:allowPublicKeyRetrieval=true ejbTimerPool && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} create-jdbc-resource --connectionpoolid ejbTimerPool --description "Resource for Hopsworks EJB Timers Pool" jdbc/hopsworksTimers && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} set ${CONFIG}.ejb-container.ejb-timer-service.timer-datasource=jdbc/hopsworksTimers && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} set ${CONFIG}.ejb-container.ejb-timer-service.property.reschedule-failed-timer=true && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} set ${CONFIG}.network-config.network-listeners.network-listener.http-listener-1.enabled=false && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} set server.network-config.network-listeners.network-listener.http-listener-1.enabled=false && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} set server.network-config.network-listeners.network-listener.http-listener-2.enabled=false && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} set configs.config.server-config.rest-monitoring-configuration.enabled=false && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} set configs.config.server-config.monitoring-service.mbean-enabled=false && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} set configs.config.server-config.monitoring-service.monitoring-enabled=false && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} set configs.config.server-config.microprofile-metrics-configuration.enabled=false && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} create-javamail-resource \
--mailhost mail.example.com \
--mailuser example \
--fromaddress example@example.com \
--storeprotocol=imap \
--storeprotocolclass=com.sun.mail.imap.IMAPStore \
--transprotocol=smtp \
--transprotocolclass=com.sun.mail.smtp.SMTPTransport \
--password mypassword \
--auth true \
--property mail-smtp-starttls-enable=true \
--target=${DG_NAME} \
mail/BBCMail && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} create-auth-realm \
--target ${CONFIG} \
--classname=com.sun.enterprise.security.auth.realm.jdbc.JDBCRealm \
--property=jaas-context=jdbcRealm:datasource-jndi=jdbc/hopsworks:user-table=hopsworks.users:user-name-column=email:password-column=password:group-table=hopsworks.users_groups:group-table-user-name-column=email:group-name-column=group_name:digest-algorithm=SHA-256:encoding=Hex \
kthfsrealm && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} set ${CONFIG}.security-service.default-realm=kthfsrealm  && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} create-jndi-resource \
 --restype javax.naming.ldap.LdapContext \
 --factoryclass com.sun.jndi.ldap.LdapCtxFactory \
 --jndilookupname dc\=example\,dc\=com \
 --property java.naming.provider.url=ldap\\://ldap\.example\.com\\:389:java.naming.ldap.attributes.binary=entryUUID:java.naming.security.authentication=none:java.naming.referral=ignore ldap/LdapResource && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} create-resource-ref --target=${DG_NAME} mail/BBCMail && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} create-resource-ref --target=${DG_NAME} jdbc/hopsworksTimers && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} create-resource-ref --target=${DG_NAME} jdbc/hopsworks && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} create-resource-ref --target=${DG_NAME} ldap/LdapResource && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} create-resource-ref --target=${DG_NAME} concurrent/condaExecutorService && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} create-resource-ref --target=${DG_NAME} concurrent/kagentExecutorService && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} create-resource-ref --target=${DG_NAME} concurrent/hopsExecutorService && \
 ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} create-resource-ref --target=${DG_NAME} concurrent/jupyterExecutorService && \
 ${PAYARA_DIR}/bin/asadmin stop-domain ${DOMAIN_NAME} && \
rm -rf \
 ${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}/osgi-cache \
 ${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}/logs

#can we do this here? Or add it to helm
RUN wget -O ${PAYARA_DIR}/glassfish/domains/domain1/lib/mysql-connector-java-8.0.21-bin.jar https://repo.hops.works/master/mysql-connector-java-8.0.21-bin.jar 

RUN mkdir /opt/payara/k8s

COPY --chown=payara:payara scripts/init_2_change_password.sh ${SCRIPT_DIR}/
COPY --chown=payara:payara scripts/init_3_download_hopsworks.sh ${SCRIPT_DIR}/
COPY --chown=payara:payara metrics.xml ${PAYARA_DIR}/glassfish/domains/domain1/config/

RUN chmod +x ${SCRIPT_DIR}/init_2_change_password.sh
RUN chmod +x ${SCRIPT_DIR}/init_3_download_hopsworks.sh