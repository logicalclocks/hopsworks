FROM flyway/flyway:9-azure

ARG version=3.5.0

RUN apk add mysql-client
RUN apk add gcc musl-dev mariadb-connector-c-dev

RUN useradd -ms /bin/bash flyway

USER flyway
COPY --chown=flyway:flyway sql/glassfish_timers.sql /flyway/

COPY --chown=flyway:flyway sql/ddl/"${version}__initial_tables.sql" /flyway/sql/"V${version}__initial_tables.sql"
COPY --chown=flyway:flyway sql/ddl/updates/"${version}.sql" /flyway/updates/sql/"V${version}__hopsworks.sql"

COPY --chown=flyway:flyway migrate.sh /flyway/migrate.sh
COPY --chown=flyway:flyway upgrade.sh /flyway/upgrade.sh
COPY --chown=flyway:flyway delete.sh /flyway/delete.sh

RUN chmod +x /flyway/migrate.sh
RUN chmod +x /flyway/upgrade.sh
RUN chmod +x /flyway/delete.sh