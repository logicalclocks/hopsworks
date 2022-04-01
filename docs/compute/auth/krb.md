# Login using Kerberos

If Kerberos is configured you will see a _Log in using_ alternative on the login page. Choose Kerberos and click on 
**Go to Hopsworks** to login. 

<figure>
  <a  href="../../../assets/images/admin/ldap/login-using-krb.png">
    <img width="400px" src="../../../assets/images/admin/ldap/login-using-krb.png" alt="Log in using Kerberos" />
  </a>
  <figcaption>Log in using Kerberos</figcaption>
</figure>

If password login is disabled you only see the _Log in using Kerberos/SSO_ alternative. Click on
**Go to Hopsworks** to login.
<figure>
  <a  href="../../../assets/images/admin/ldap/krb-login.png">
    <img width="400px" src="../../../assets/images/admin/ldap/krb-login.png" alt="Kerberos only" />
  </a>
  <figcaption>Kerberos only authentication</figcaption>
</figure>

To be able to authenticate with Kerberos you need to configure your browser to use Kerberos. 
Note that without a properly configured browser, the Kerberos token is not sent to the server and so SSO will not work.

If Kerberos is not configured properly you will see **Wrong credentials** message when trying to log in.
<figure>
  <a  href="../../../assets/images/admin/ldap/no-ticket.png">
    <img width="400px" src="../../../assets/images/admin/ldap/no-ticket.png" alt="Browser not configured" />
  </a>
  <figcaption>Missing Kerberos ticket</figcaption>
</figure>

When logging in with Kerberos for the first time Hopsworks will retrieve and save consented claims (firstname, lastname
and email), about the logged in end-user. If you have multiple email addresses registered in Kerberos you can choose 
one to use with Hopsworks.

If you do not want your information to be saved in Hopsworks you can click **Cancel**. This will redirect you back
to the login page.

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