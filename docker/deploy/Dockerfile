FROM payara/server-node:5.2022.5

USER payara
COPY --chown=payara:payara entrypoint.sh ${SCRIPT_DIR} 

RUN true \
    && chmod +x ${SCRIPT_DIR}/entrypoint.sh \
    && true

# entrypoint.sh suited for Kubernetes.
ENTRYPOINT "${SCRIPT_DIR}/entrypoint.sh"