FROM flyway/flyway:9-azure

ARG version=3.5.0

RUN apk add mysql-client
RUN apk add gcc musl-dev mariadb-connector-c-dev

COPY sql/glassfish_timers.sql /flyway/

COPY sql/ddl/"${version}__initial_tables.sql" /flyway/sql/"V${version}__initial_tables.sql"
COPY sql/ddl/updates/"${version}.sql" /flyway/sql/"V${version}__hopsworks.sql"

COPY migrate.sh /flyway/migrate.sh
COPY upgrade.sh /flyway/upgrade.sh
COPY delete.sh /flyway/delete.sh

RUN chmod +x /flyway/migrate.sh
RUN chmod +x /flyway/upgrade.sh
RUN chmod +x /flyway/delete.sh