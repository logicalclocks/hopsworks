# User Management
Whether you run Hopsworks on-premise or on the cloud hopsworks.ai, you have a Hopsworks cluster which contains projects.
The cluster can be seen as the highest level entity which contains all of your data.

## Cluster users
All the users of your Hopsworks instance have access to your cluster with different access rights. 
You can find them in ```Cluster Settings > Users```.
<figure>
  <a  href="../../assets/images/admin/user-management/active-users.png">
    <img src="../../assets/images/admin/user-management/active-users.png" alt="active users" />
  </a>
  <figcaption>Active Users</figcaption>
</figure>

### Cluster roles

Roles let you manage the access rights of a user to the cluster configuration
- Hops user: it's the most restrictive access. This role limits the user to projects only and disable the access to 
cluster configuration.
- Hops admin: the highest level of access. This role lets the user manage any parameters of the cluster including 
validating new members and managing any projects.

## Validating and blocking users
By default, user who register on Hopsworks using their own credentials are not granted access to the cluster data. 
First, a Hopsworks admin needs to validate their account.

Similarly, you can block a user at any moment. To keep consistency with to history of your datasets, 
a user can not be deleted but only blocked. You can block a user from ```Cluster Settings > Users```.

<figure>
  <a  href="../../assets/images/admin/user-management/blocked-users.png">
    <img src="../../assets/images/admin/user-management/blocked-users.png" alt="blocked users" />
  </a>
  <figcaption>Blocked Users</figcaption>
</figure>

<figure>
  <a  href="../../assets/images/admin/user-management/user-request-review.png">
    <img src="../../assets/images/admin/user-management/user-request-review.png" alt="request" />
  </a>
  <figcaption>Review user request</figcaption>
</figure>

<figure>
  <a  href="../../assets/images/admin/user-management/new-user.png">
    <img src="../../assets/images/admin/user-management/new-user.png" alt="New user" />
  </a>
  <figcaption>Create new user</figcaption>
</figure>

<figure>
  <a  href="../../assets/images/admin/user-management/create-user.png">
    <img src="../../assets/images/admin/user-management/create-user.png" alt="create user" />
  </a>
  <figcaption>Copy temporary password</figcaption>
</figure>

<figure>
  <a  href="../../assets/images/admin/user-management/reset-password.png">
    <img src="../../assets/images/admin/user-management/reset-password.png" alt="reset password" />
  </a>
  <figcaption>Reset user password</figcaption>
</figure>

<figure>
  <a  href="../../assets/images/admin/user-management/temp-password.png">
    <img src="../../assets/images/admin/user-management/temp-password.png" alt="temp password" />
  </a>
  <figcaption>Copy temporary password</figcaption>
</figure>