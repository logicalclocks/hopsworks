FROM payara:5.2022.5

ARG version=3.8.0-SNAPSHOT
ARG download_url=https://repo.hops.works/master/hopsworks
ARG frontend_download_url=https://repo.hops.works/master/hopsworks/frontend
ARG user
ARG password

USER payara

RUN mkdir /opt/payara/k8s/hopsworks

RUN wget --user=${user} --password=${password} -O /opt/payara/k8s/hopsworks/hopsworks-ca.war ${download_url}/${version}/hopsworks-ca.war
RUN wget --user=${user} --password=${password} -O /opt/payara/k8s/hopsworks/hopsworks-ear.ear ${download_url}/${version}/hopsworks-ear.ear
RUN wget --user=${user} --password=${password} -O ${PAYARA_DIR}/glassfish/domains/domain1/lib/hopsworks-realm.jar ${download_url}/${version}/hopsworks-realm.jar
RUN wget --user=${user} --password=${password} -O /opt/payara/k8s/hopsworks/frontend.tgz ${frontend_download_url}/${version}/frontend.tgz

RUN tar -xvzf /opt/payara/k8s/hopsworks/frontend.tgz -C ${PAYARA_DIR}/glassfish/domains/domain1/docroot/

RUN rm -rf /opt/payara/k8s/hopsworks/frontend.tgz

ENTRYPOINT "${SCRIPT_DIR}/entrypoint.sh"