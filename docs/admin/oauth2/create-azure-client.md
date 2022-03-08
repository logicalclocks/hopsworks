# Create An Application in Azure Active Directory.

This example uses Azure Active Directory as the identity provider, but the same can be done with any identity provider 
supporting OAuth2.

## Configure your identity provider.
To use OAuth2 in hopsworks you first need to create and configure an OAuth client in your identity provider. We will take the example of Azure AD for the remaining of this documentation, but equivalent steps can be taken on other identity providers.

Navigate to the [Microsoft Azure Portal](https://portal.azure.com) and authenticate. Navigate to [Azure Active Directory](https://portal.azure.com/#blade/Microsoft_AAD_IAM/ActiveDirectoryMenuBlade/Overview). Click on [App Registrations](https://portal.azure.com/#blade/Microsoft_AAD_IAM/ActiveDirectoryMenuBlade/RegisteredApps). Click on *New Registration*.

<p align="center">
  <figure>
    <a  href="../../../assets/images/admin/oauth2/sso/create_application.png">
      <img style="border: 1px solid #000" src="../../../assets/images/admin/oauth2/sso/create_application.png" alt="Create application">
    </a>
    <figcaption>Create application</figcaption>
  </figure>
</p>

Enter a name for the client such as *hopsworks_oauth_client*. Verify the Supported account type is set to *Accounts in this organizational directory only*. And Click Register.

<p align="center">
  <figure>
    <a  href="../../../assets/images/admin/oauth2/sso/name_application.png">
      <img style="border: 1px solid #000" src="../../../assets/images/admin/oauth2/sso/name_application.png" alt="Name application">
    </a>
    <figcaption>Name application</figcaption>
  </figure>
</p>

In the Overview section, copy the *Application (client) ID field*. We will use it in 
[Identity Provider registration](./create-client.md) under the name *Client id*.

<p align="center">
  <figure>
    <a  href="../../../assets/images/admin/oauth2/sso/client_id.png">
      <img style="border: 1px solid #000" src="../../../assets/images/admin/oauth2/sso/client_id.png" alt="Copy client ID">
    </a>
    <figcaption>Copy client ID</figcaption>
  </figure>
</p>

Click on *Endpoints* and copy the *OpenId Connect metadata document* endpoint excluding the *.well-known/openid-configuration* part. 
We will use it in [Identity Provider registration](./create-client.md) under the name *Connection URL*.

<p align="center">
  <figure>
    <a  href="../../../assets/images/admin/oauth2/sso/endpoint.png">
      <img style="border: 1px solid #000" src="../../../assets/images/admin/oauth2/sso/endpoint.png" alt="Endpoint">
    </a>
    <figcaption>Endpoint</figcaption>
  </figure>
</p>

Click on *Certificates & secrets*, then Click on *New client secret*.

<p align="center">
  <figure>
    <a  href="../../../assets/images/admin/admin/sso/new_client_secret.png">
      <img style="border: 1px solid #000" src="../../../assets/images/admin/oauth2/sso/new_client_secret.png" alt="New client secret">
    </a>
    <figcaption>New client secret</figcaption>
  </figure>
</p>

Add a *description* of the secret. Select an expiration period. And, Click *Add*.

<p align="center">
  <figure>
    <a  href="../../../assets/images/admin/oauth2/sso/new_client_secret_config.png">
      <img style="border: 1px solid #000" src="../../../assets/images/admin/oauth2/sso/new_client_secret_config.png" alt="Client secret creation">
    </a>
    <figcaption>Client secret creation</figcaption>
  </figure>
</p>

Copy the secret. This will be used in [Identity Provider registration](./create-client.md) under the name 
*Client Secret*.

<p align="center">
  <figure>
    <a  href="../../../assets/images/admin/oauth2/sso/copy_secret.png">
      <img style="border: 1px solid #000" src="../../../assets/images/admin/oauth2/sso/copy_secret.png" alt="Client secret creation">
    </a>
    <figcaption>Client secret creation</figcaption>
  </figure>
</p>

Click on *Authentication*. Then click on *Add a platform*

<p align="center">
  <figure>
    <a  href="../../../assets/images/admin/oauth2/sso/add_platform.png">
      <img style="border: 1px solid #000" src="../../../assets/images/admin/oauth2/sso/add_platform.png" alt="Add a platform">
    </a>
    <figcaption>Add a platform</figcaption>
  </figure>
</p>

In *Configure platforms* click on *Web*.

<p align="center">
  <figure>
    <a  href="../../../assets/images/admin/oauth2/sso/add_platform_web.png">
      <img style="border: 1px solid #000" src="../../../assets/images/admin/oauth2/sso/add_platform_web.png" alt="Configure platform: Web">
    </a>
    <figcaption>Configure platform: Web</figcaption>
  </figure>
</p>

Enter the *Redirect URI* and click on *Configure*. The redirect URI is *HOPSWORKS-URI/callback* with *HOPSWORKS-URI* the URI of your hopsworks cluster.

<p align="center">
  <figure>
    <a  href="../../../assets/images/admin/oauth2/sso/add_platform_redirect.png">
      <img style="border: 1px solid #000" src="../../../assets/images/admin/oauth2/sso/add_platform_redirect.png" alt="Configure platform: Redirect">
    </a>
    <figcaption>Configure platform: Redirect</figcaption>
  </figure>
</p>

!!! note

    If your hopsworks cluster is created on the cloud (hopsworks.ai),
    you can find your *HOPSWORKS-URI* by going to the [hopsworks.ai dashboard](https://managed.hopsworks.ai/dashboard) 
    in the *General* tab of your cluster and copying the URI.


