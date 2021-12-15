# Register Client in Hopsworks

Before registering a client in Hopsworks you need to create a client application in your identity provider and 
acquire a _client id_ and _client secret_. An example on how to create a client using [Okta](https://www.okta.com/) 
identity provider can be found [here](./create-okta-client.md).

After acquiring the _client id_ and _client secret_ create the client in Hopsworks by giving it a name (the name 
will be used in the login page) as shown in the figure bellow.

<figure>
  <a  href="../../../assets/images/admin/oauth2/register-app.png">
    <img src="../../../assets/images/admin/oauth2/register-app.png" alt="Application overview" />
  </a>
  <figcaption>Application overview</figcaption>
</figure>

Optionally a logo URL to an image can be added. The logo will be shown on the login page with the name as shown in the 
figure bellow.

  <figure>
    <a  href="../../../assets/images/auth/oauth2.png">
      <img width="400px" src="../../../assets/images/auth/oauth2.png" alt="OAuth2 login" />
    </a>
    <figcaption>Login with OAuth2</figcaption>
  </figure>

!!! note

    When creating a client make sure you can access the provider metadata by making a GET a request on the well known 
    endpoint of the provider. The well-known URL, will typically be the _Connection URL_ plus 
    `.well-known/openid-configuration`. For the above client it would be 
    `https://dev-86723251.okta.com/.well-known/openid-configuration`.