# Register Identity Provider in Hopsworks

Before registering your identity provider in Hopsworks you need to create a client application in your identity provider and 
acquire a _client id_ and a _client secret_. An example on how to create a client using [Okta](https://www.okta.com/)
and [Azure Active Directory](https://portal.azure.com/#blade/Microsoft_AAD_IAM/ActiveDirectoryMenuBlade/Overview) 
identity providers can be found [here](./create-okta-client.md) and [here](./create-azure-client.md) respectively.

After acquiring the _client id_ and _client secret_ create the client in Hopsworks by [enabling OAuth2](../auth.md)
and clicking on _add another identity provider_ in the [Authentication configuration page](../auth.md). Then set 
base uri of your identity provider in _Connection URL_ give a name to your identity provider (the name will be used 
in the login page as an alternative login method) and set the _client id_ and _client secret_ in their respective 
fields,  as shown in the figure below.

<figure>
  <a  href="../../../assets/images/admin/oauth2/register-app.png">
    <img src="../../../assets/images/admin/oauth2/register-app.png" alt="Application overview" />
  </a>
  <figcaption>Application overview</figcaption>
</figure>

- _Connection URL_: (provider Uri) is the base uri of the identity provider's API (URI should contain scheme http:// or 
  https://). 

Additional configuration can be set here:

- _Verify email_: if checked only users with verified email address (in the identity provider) can log in to Hopsworks. 
- _Code challenge_: if your identity provider requires code challenge for authorization request check 
  the _code challenge_ check box. This will allow you to choose code challenge method that can be either _plain_ or 
  _S256_.
- _Logo URL_: optionally a logo URL to an image can be added. The logo will be shown on the login page with the name 
  as shown in the figure below.

## Group mapping
Optionally you can add a group mapping from your identity provider to hopsworks groups, by clicking on your name in the 
top right corner of the navigation bar and choosing *Cluster Settings* from the dropdown menu. In the *Cluster 
Settings* _Configuration_ tab search for _oauth\_group\_mapping_ and click on the edit button.

  <figure>
    <a  href="../../../assets/images/admin/oauth2/sso/oauth-group-mapping.png">
      <img src="../../../assets/images/admin/oauth2/sso/oauth-group-mapping.png" alt="Set variables">
    </a>
    <figcaption>Set Configuration variables</figcaption>
  </figure>

!!! Note

    Setting *oauth_group_mapping* to *ANY_GROUP->HOPS_USER* will assign the role *user* to any user from any group in 
    your identity provider when they log into hopsworks with OAuth for the first time. You can replace *ANY_GROUP* with 
    the group of your choice in the identity provider. You can replace *HOPS_USER* by *HOPS_ADMIN* if you want the 
    users of that group to be admins in hopsworks. You can do several mappings by separating them with a semicolon.

Users will now see a new button on the login page. The button has the name you set above for _Name_ and will 
redirect to your identity provider.

  <figure>
    <a  href="../../../assets/images/auth/oauth2.png">
      <img width="400px" src="../../../assets/images/auth/oauth2.png" alt="OAuth2 login" />
    </a>
    <figcaption>Login with OAuth2</figcaption>
  </figure>

!!! note

    When creating a client make sure you can access the provider metadata by making a GET request on the well known 
    endpoint of the provider. The well-known URL, will typically be the _Connection URL_ plus 
    `.well-known/openid-configuration`. For the above client it would be 
    `https://dev-86723251.okta.com/.well-known/openid-configuration`.