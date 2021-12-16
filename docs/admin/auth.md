# Authentication Methods

In the **Cluster Settings** _Authentication_ tab you can configure how users authenticate.

1. **TOTP Two-factor Authentication**: can be _disabled_, _optional_ or _mandatory_. If set to mandatory all users are 
   required to set up tow-factor authentication when registering. 

    !!! note
    
        If two-factor is set to _mandatory_ on a cluster with registered users all users will need to go through  
        lost divice recovery step to enable two-factor. So consider setting it to _optional_ first and allow users to 
        enable it before setting it to mandatory.

2. **OAuth2**: if your organization already have an identity management system compatible with 
   [OpenID Connect (OIDC)](https://openid.net/connect/) you can configure Hopsworks to use your identity provider 
   to verify the identity of the End-User by enabling **OAuth** as shown in the figure bellow. After enabling OAuth 
   you can register your client by clicking on **Add Identity Provider** button. See
   [Create client](./oauth2/create-client.md) for details.

<figure>
  <a  href="../../../assets/images/admin/auth-config.png">
    <img src="../../../assets/images/admin/auth-config.png" alt="Authentication config" />
  </a>
  <figcaption>Setup Authentication Methods</figcaption>
</figure>