FROM payara/server-node:5.2022.5

USER root
RUN apt-get update && apt install dnsutils -y
RUN mkdir -p /srv/hops && chown payara:payara /srv/hops

USER payara

RUN true \
    && chmod +x ${SCRIPT_DIR}/entrypoint.sh \
    && true