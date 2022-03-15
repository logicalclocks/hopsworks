# Login using Kerberos

<figure>
  <a  href="../../../assets/images/admin/ldap/login-using-krb.png">
    <img width="400px" src="../../../assets/images/admin/ldap/login-using-krb.png" alt="Log in using Kerberos" />
  </a>
  <figcaption>Log in using Kerberos</figcaption>
</figure>

<figure>
  <a  href="../../../assets/images/admin/ldap/krb-login.png">
    <img width="400px" src="../../../assets/images/admin/ldap/krb-login.png" alt="Kerberos only" />
  </a>
  <figcaption>Kerberos only authentication</figcaption>
</figure>

<figure>
  <a  href="../../../assets/images/admin/ldap/no-ticket.png">
    <img width="400px" src="../../../assets/images/admin/ldap/no-ticket.png" alt="Browser not configured" />
  </a>
  <figcaption>Missing Kerberos ticket</figcaption>
</figure>

When logging in with Kerberos for the first time Hopsworks will retrieve and save consented claims (firstname, lastname
and email), about the logged in end-user.

<figure>
  <a  href="../../../assets/images/auth/consent.png">
    <img width="400px" src="../../../assets/images/auth/consent.png" alt="OAuth2 consent" />
  </a>
  <figcaption>Give consent</figcaption>
</figure>

After clicking on **Register** you will be redirected to the landing page:
  <figure>
    <a  href="../../../assets/images/project/landing-page.png">
      <img alt="landing page" src="../../../assets/images/project/landing-page.png">
    </a>
    <figcaption>Landing page</figcaption>
  </figure>
In the landing page, you will find two buttons. Use these buttons to either create a 
[demo project](../project/demoProject.md) or [a new project](../project/createProject.md).