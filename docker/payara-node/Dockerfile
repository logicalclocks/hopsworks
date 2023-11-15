FROM payara/server-node:5.2022.5

USER root
RUN apt-get update && apt install dnsutils -y
RUN mkdir -p /srv/hops && chown payara:payara /srv/hops

USER payara
COPY --chown=payara:payara hw_entrypoint.sh ${SCRIPT_DIR}

RUN true \
    && chmod +x ${SCRIPT_DIR}/hw_entrypoint.sh \
    && true

# entrypoint.sh suited for Kubernetes.
ENTRYPOINT "${SCRIPT_DIR}/hw_entrypoint.sh"