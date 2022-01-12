# Create An Application in Okta

This example uses an Okta development account to create an application that will represent a Hopsworks client in the 
identity provider. To create a developer account go to [Okta developer](https://developer.okta.com/signup/).

After creating a developer account register a client by going to _Applications_ and click on **Create App Integration**.

  <figure>
    <a  href="../../../assets/images/admin/oauth2/okta.png">
      <img src="../../../assets/images/admin/oauth2/okta.png" alt="Okta Applications" />
    </a>
    <figcaption>Okta Applications</figcaption>
  </figure>

This will open a popup as shown in the figure below. Select **OIDC** as _Sign-in-method_ and **Web Application** as 
_Application type_ and click next.
  <figure>
    <a  href="../../../assets/images/admin/oauth2/create-new-app.png">
      <img src="../../../assets/images/admin/oauth2/create-new-app.png" alt="Create New Application" />
    </a>
    <figcaption>Create new Application</figcaption>
  </figure>

Give your application a name and select **Client credential** as _Grant Type_. Then add a _Sign-in redirect URI_ 
that is your Hopsworks cluster domain name (including the port number if needed) with path _/callback_, and a _Sign-out 
redirect URI_ that is Hopsworks cluster domain name (including the port number if needed) with no path.

  <figure>
    <a  href="../../../assets/images/admin/oauth2/new-web-app.png">
      <img src="../../../assets/images/admin/oauth2/new-web-app.png" alt="New Application" />
    </a>
    <figcaption>New Application</figcaption>
  </figure>

If you want to limit who can access your Hopsworks cluster select _Limit access to selected groups_ and 
select group(s) you want to give access to. Here we will allow everyone in the organization to access the cluster.

  <figure>
    <a  href="../../../assets/images/admin/oauth2/assignments.png">
      <img src="../../../assets/images/admin/oauth2/assignments.png" alt="Group assignment" />
    </a>
    <figcaption>Group assignment</figcaption>
  </figure>

After the application is created go back to _Applications_ and click on the application you just created. Use the
_Okta domain_ (_Connection URL_), _client id_ and _client secret_ generated for your app in the [client registration](./create-client.md) in Hopsworks.

  <figure>
    <a  href="../../../assets/images/admin/oauth2/overview.png">
      <img src="../../../assets/images/admin/oauth2/overview.png" alt="Application overview" />
    </a>
    <figcaption>Application overview</figcaption>
  </figure>

!!! note

    When copying the domain in the figure above make sure to add the url scheme (http:// or https://) when using it 
    in the _Connection URL_ in the [client registration form](./create-client.md).