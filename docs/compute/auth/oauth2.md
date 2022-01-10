# Login using a third-party identity provider.
If OAuth is configured a **Login with ** button will appear in the login page. Use this button to log in to Hopsworks
using your OAuth credentials.

  <figure>
    <a  href="../../../assets/images/auth/oauth2.png">
      <img width="400px" src="../../../assets/images/auth/oauth2.png" alt="OAuth2 login" />
    </a>
    <figcaption>Login with OAuth2</figcaption>
  </figure>

When logging in with OAuth for the first time Hopsworks will retrieve and save consented claims (firstname, lastname 
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