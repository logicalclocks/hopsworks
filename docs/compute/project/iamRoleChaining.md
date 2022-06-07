# Assuming AWS IAM Roles
When deploying Hopsworks on EC2 instances you might need to assume different roles to access resources on AWS. 
These roles are configured in AWS and mapped to a project in Hopsworks, for a guide on how to configure this go to 
[AWS IAM Role Chaining](../../admin/iamRoleChaining.md).

After an administrator configured role mappings in Hopsworks you can see the roles you can assume in the Project 
Settings IAM Role Chaining tab.
<figure>
  <a href="../../../assets/images/iam-role/project-settings.png">
    <img src="../../../assets/images/iam-role/project-settings.png" alt="Role Chaining"/>
  </a>
  <figcaption>Role Chaining</figcaption>
</figure>

You can then use the [Hops python library](https://hops-py.logicalclocks.com/) and 
[Hops java/scala library](https://github.com/logicalclocks/hops-util) to assume the roles listed in your project’s settings page.

When calling _assume\_role_ you can pass the role ARN string or use the get role method that takes the role id 
as an argument. If you assign a default role for your project you can call _assume\_role_ without arguments.

You can assign (if you are a Data owner in that project) a default role to you project by clicking on the _default_ 
checkbox of the role you want to make default. You can set one default per project role. If a default is set for 
a project role (Data scientist or Data owner) and all members (ALL) the default set for the project role will take 
precedence over the default set for all members.

###### python
```python
from hops.credentials_provider import get_role, assume_role
credentials = assume_role(role_arn=get_role(1))
spark.read.csv("s3a://resource/test.csv").show()
```

###### scala
```scala
import io.hops.util.CredentialsProvider
val creds = CredentialsProvider.assumeRole(CredentialsProvider.getRole(1))
spark.read.csv("s3a://resource/test.csv").show()
```

The _assume\_role_  method sets spark hadoop configurations that will allow spark to read s3 buckets. The code examples 
above show how to read s3 buckets using Python and Scala.

The method also sets environment variables **AWS_ACCESS_KEY_ID**, **AWS_SECRET_ACCESS_KEY** and 
**AWS_SESSION_TOKEN** so that programs running in the container can use the credentials for the newly assumed role.