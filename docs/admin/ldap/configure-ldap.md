# Configure LDAP/Kerberos

LDAP need some server configuration before you can enable it from the UI. For instruction on how to 
configure your hopsworks server see 
[Server Configuration for LDAP](./configure-server.md#server-configuration-for-ldap)

After configuring the server you can configure Authentication methods by clicking on your name in the top right 
corner of the navigation bar and choosing *Cluster Settings* from the dropdown menu.
In the _Authentication_ tab you can find in **Cluster Settings**, you can enable LDAP by clicking on the LDAP checkbox.

If LDAP/Kerberos checkbox is not checked make sure that you configured your application server and enable it by 
clicking on the checkbox.

<figure>
  <a  href="../../../assets/images/admin/auth-config.png">
    <img src="../../../assets/images/admin/auth-config.png" alt="Authentication config" />
  </a>
  <figcaption>Setup Authentication Methods</figcaption>
</figure>

Finally, click on edit configuration and fill in the attributes.

<figure>
  <a  href="../../../assets/images/admin/ldap/configure-ldap.png">
    <img src="../../../assets/images/admin/ldap/configure-ldap.png" alt="LDAP config" />
  </a>
  <figcaption>Configure LDAP</figcaption>
</figure>

- Account status: the status a user will be assigned when logging in for the first time. If a use is assigned a status 
  different from _Activated_ an admin needs to manually activate each user from the [User management](../user.md).
- Group mapping: allows you to specify a mapping between LDAP groups and Hopsworks groups. The mapping is a 
  semicolon separated string in the form ```Directory Administrators->HOPS_ADMIN;IT People-> HOPS_USER```. Default
  is empty. If no mapping is specified, users need to be assigned a role by an admin before they can log in.
- User id: the id field in LDAP with a string placeholder. Default ```uid=%s```.
- User given name: the given name field in LDAP. Default ```givenName```.
- User surname: the surname field in LDAP. Default ```sn```.
- User email: the email field in LDAP. Default ```mail```.
- User search filter: the search filter for user. Default ```uid=%s```.
- Group search filter: the search filter for groups. Default ```member=%d```.
- Group target: the target to search for groups in the LDAP directory tree. Default ```cn```.
- Dynamic group target: the target to search for dynamic groups in the LDAP directory tree. Default ```memberOf```.
- User dn: specify the distinguished name (DN) of the container or base point where the users are stored. Default is 
  empty. 
- Group dn: specify the DN of the container or base point where the groups are stored. Default is empty.

All defaults are taken from [OpenLDAP](https://www.openldap.org/).

The login page will now have the choice to use LDAP for authentication.
<figure>
  <a  href="../../../assets/images/admin/ldap/login-ldap.png">
    <img width="400px" src="../../../assets/images/admin/ldap/login-ldap.png" alt="Log in using LDAP" />
  </a>
  <figcaption>Log in using LDAP</figcaption>
</figure>
