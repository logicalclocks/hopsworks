# AWS IAM Role Chaining
Using an EC2 instance profile enables your Hopsworks cluster to access AWS resources. 
This forces all Hopsworks users to share the instance profile role and the resource access policies attached to 
that role. To allow for per project access policies you could have your users use AWS credentials directly in 
their programs which is not recommended so you should instead use [Role chaining](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_terms-and-concepts.html#iam-term-role-chaining).
To use Role chaining, you need to first setup IAM roles in AWS:

 **Step 1**. Create an instance profile role with policies that will allow it to assume all resource roles that we can 
 assume 
from the Hopsworks cluster.

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "AssumeDataRoles",
            "Effect": "Allow",
            "Action": "sts:AssumeRole",
            "Resource": [
                "arn:aws:iam::123456789011:role/test-role",
                "arn:aws:iam::xxxxxxxxxxxx:role/s3-role",
                "arn:aws:iam::xxxxxxxxxxxx:role/dev-s3-role",
                "arn:aws:iam::xxxxxxxxxxxx:role/redshift"
            ]
        }
    ]
}
```
<figcaption>Example policy for assuming four roles.</figcaption>

 **Step 2**. Create the resource roles and edit trust relationship and add policy document that will allow the instance 
 profile 
to assume this role.

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
        "Effect": "Allow",
        "Principal": {
            "AWS": "arn:aws:iam::xxxxxxxxxxxx:role/instance-profile"
        },
        "Action": "sts:AssumeRole"
        }
    ]
}
```
<figcaption>Example policy document.</figcaption>

Role chaining allows the instance profile to assume any role in the policy attached in step 1. To limit access to 
iam roles we can create a per-project mapping from the admin page in Hopsworks.

<figure>
  <a href="../../assets/images/admin/iam-role/cluster-settings.png">
    <img src="../../assets/images/admin/iam-role/cluster-settings.png" alt="Role Chaining"/>
  </a>
  <figcaption>Role Chaining</figcaption>
</figure>

Click on your name in the top right corner of the navigation bar and choose _Cluster Settings_ from the dropdown menu.
In the Cluster Settings' _IAM Role Chaining_ tab you can configure the mappings between projects and IAM roles.
You can add mappings by entering the project name, which roles in that project can access the cloud role and the 
role ARN. 
Optionally you can set a role mapping as default by marking the default checkbox. The default roles can be changed from 
the project setting by a Data owner in that project.

<figure>
  <a href="../../assets/images/admin/iam-role/new-role-chaining.png">
    <img src="../../assets/images/admin/iam-role/new-role-chaining.png" alt="Create Role Chaining"/>
  </a>
  <figcaption>Create Role Chaining</figcaption>
</figure>

Any member of a project can then go to the _Project Settings_ -> 
[Assuming IAM Roles](../compute/project/iamRoleChaining.md) page to see which roles they can assume.