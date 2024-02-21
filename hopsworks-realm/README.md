## Hopsworks jdbc realm
Hopsworks JDBC realm is a security realm for Payara with a JDBC backend.

Payara's JDBC realm assumes a data model with two tables.
One for user with encoded password and another with pairs of usernames and group names
to define to which groups a user belongs.

Hopsworks JDBC realm accepts two queries one to determine the password of the
user based on the username and one to determine the groups the user belongs to based
on the username. This allows it to handle any data model.


### Build hopsworks JDBC realm
```sh
 mvn clean compile assembly:single
```

### Use hopsworks JDBC realm

Copy ```hopsworks-realm-jar-with-dependencies.jar``` to ```[payara home installation]/domains/domain1/lib/hopsworks-realm.jar```

#### Create the realm

```sh
PASSWORD_QUERY="SELECT password FROM hopsworks.users WHERE email = ?"
GROUP_QUERY ="SELECT G.group_name from hopsworks.bbc_group AS G, hopsworks.user_group AS UG, hopsworks.users AS U WHERE U.email=? AND UG.gid = G.gid AND UG.uid = U.uid"

${PAYARA_DIR}/bin/asadmin create-auth-realm \
--login-module=io.hops.hopsworks.realm.jdbc.HopsworksLoginModule \
--classname=io.hops.hopsworks.realm.jdbc.HopsworksJDBCRealm \
--property=jaas-context=hopsworksJdbcRealm:datasource-jndi=jdbc/hopsworks:password-query=${PASSWORD_QUERY}:group-query=${GROUP_QUERY}:digest-algorithm=SHA-256:encoding=Hex \
hopsworksrealm
```

1. _password-query_ query used to determine the password of the user based on the username (login name). default
   ```"SELECT password FROM hopsworks.users WHERE email = ?"```
2. _group-query_ query used to determine the groups the user belongs to based on the username (login name). default
   ```"SELECT G.group_name from hopsworks.bbc_group AS G, hopsworks.user_group AS UG, hopsworks.users AS U WHERE U.email=? AND UG.gid = G.gid AND UG.uid = U.uid"```
3. _datasource-jndi_ the datasource used to access the database.