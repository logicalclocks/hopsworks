FROM payara/server-node:5.2022.5

USER root
RUN apt-get update && apt install -y dnsutils curl jq

#TODO: How do we set the kube version
RUN curl -LO https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/amd64/kubectl
RUN chmod +x ./kubectl
RUN mv ./kubectl /usr/local/bin/kubectl

USER payara
COPY --chown=payara:payara entrypoint.sh ${SCRIPT_DIR} 

RUN true \
    && chmod +x ${SCRIPT_DIR}/entrypoint.sh \
    && true

# entrypoint.sh to deploy hopsworks
ENTRYPOINT "${SCRIPT_DIR}/entrypoint.sh"