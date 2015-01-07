l#!/bin/sh
/home/x/.netbeans/7.1.2/config/GF3_1/bin/asaadmin create-auth-realm --classname com.sun.enterprise.security.auth.realm.jdbc.JDBCRealm --property jaas-context=jdbcRealm:datasource-jndi="jdbc/kthfs":user-table=users:user-name-column=email:password-column=password:group-table=users_groups:group-name-column=groupname:digest-algorithm=none userMgmtJdbcRealm
#/home/glassfish/glassfish/glassfish3/bin/asaadmin create-auth-realm --classname com.sun.enterprise.security.auth.realm.jdbc.JDBCRealm --property jaas-context=jdbcRealm:datasource-jndi="jdbc/kthfs":user-table=users:user-name-column=email:password-column=password:group-table=users_groups:group-name-column=groupname:digest-algorithm=none userMgmtJdbcRealm
 

   <jdbc-connection-pool allow-non-component-callers="false" 
                          associate-with-thread="false" 
                          connection-creation-retry-attempts="0" 
                          connection-creation-retry-interval-in-seconds="10" 
                          connection-leak-reclaim="false" 
                          connection-leak-timeout-in-seconds="0" 
                          connection-validation-method="auto-commit" 
                          datasource-classname="com.mysql.jdbc.jdbc2.optional.MysqlDataSource" 
                          fail-all-connections="false" 
                          idle-timeout-in-seconds="30" 
                          is-connection-validation-required="false" 
                          is-isolation-level-guaranteed="true" 
                          lazy-connection-association="false" 
                          lazy-connection-enlistment="false" 
                          match-connections="false" 
                          max-connection-usage-count="0" 
                          max-pool-size="32" 
                          max-wait-time-in-millis="60000" 
                          name="Mysqlkthfs" 
                          non-transactional-connections="false" 
                          pool-resize-quantity="2" 
                          res-type="javax.sql.DataSource" 
                          statement-timeout-in-seconds="-1" 
                          steady-pool-size="8" 
                          validate-atmost-once-period-in-seconds="0" 
                          wrap-jdbc-objects="false">
        <property name="serverName" value="snurran.sics.se"/>
        <property name="portNumber" value="3306"/>
        <property name="databaseName" value="kthfs"/>
        <property name="User" value="kthfs"/>
        <property name="Password" value="kthfs"/>
        <property name="URL" value="jdbc:mysql://snurran.sics.se:3306/kthfs"/>
        <property name="driverClass" value="com.mysql.jdbc.Driver"/>
    </jdbc-connection-pool>
    <jdbc-resource enabled="true" jndi-name="jdbc/kthfs" 
                   object-type="kthfs" pool-name="Mysqlkthfs"/>

    <!-- 
    To create the declared jdbc resources with GlassFish asadmin command:
    $ asadmin add-resources $HOME/glassfish-resources.xml
    -->

    <mail-resource debug="false" 
                   enabled="true" 
                   from="jdowling@sics.se" 
                   host="smtp.sics.se"  
                   jndi-name="mail/jdowling" 
                   object-type="user" 
                   store-protocol="imap" 
                   pasword=""
                   store-protocol-class="com.sun.mail.imap.IMAPStore" 
                   transport-protocol="smtp" 
                   transport-protocol-class="com.sun.mail.smtp.SMTPSSLTransport" 
                   user="jdowling@sics.se"
                   mail-smtp.port="465"
                   mail-smtp.auth="true"
                   mail-smtp.socketFactory.class="javax.net.ssl.SSLSocketFactory"
                   mail-smtp.starttls.enable="false"
        <description/>
    </mail-resource>
