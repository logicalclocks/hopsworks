# Project-based multi-tenancy

A project can be thought of as an abstraction combining three entities:

- Data
- Users
- Compute management (Jobs, Notebooks ...)

A user in a project is called a member of the project and can carry one of two roles:

- Data Owner
- Data Scientist

**Data Owners** can perform all kinds of operations in project, most importantly invite members, create and 
share Datasets, delete data in a project.

**Data Scientists** are given mostly read-only access to Datasets and are not allowed to invite other members. This 
role is mostly useful for inviting members from other departments of an organization, so they can apply their AI 
pipelines and develop programs on the data that is owned by the Data Owners.

A Data Owner can invite other members by clicking **Add members** button on the right-hand side of the Members card 
then search by email of the user to invite. The figure below shows a project project1 with two members, 
a Data Owner (Admin) and a Data Scientist (OnlineFS). 
The role of a member can be altered at any time from the same screen by any Data Owner by clicking on **manage 
members** link.

<p align="center">
  <figure>
    <a  href="../../../assets/images/project/addMember.png">
      <img width="800px" src="../../../assets/images/project/addMember.png" alt="OTP">
    </a>
    <figcaption>Add a new member</figcaption>
  </figure>
</p>

!!! warning
    **OnlineFS** is a system user, so deleting or altering the role of this user can create problems in the online 
    feature store.