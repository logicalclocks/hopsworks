# Hopsworks

[![Join the chat at https://gitter.im/hopshadoop/hopsworks](https://badges.gitter.im/hopshadoop/services.png)](https://gitter.im/hopshadoop/hopsworks)
[![Google Group](https://img.shields.io/badge/google-group-blue.svg)](https://groups.google.com/forum/#!forum/hopshadoop)


<a href=""><img src="http://www.hops.io/sites/default/files/hops-50x50.png" align="left" hspace="10" vspace="6"></a>
**Hopsworks** is the UI for Hops, a new distribution of Apache Hadoop with scalable, highly available, customizable metadata. Hopsworks lowers the barrier to entry for users getting started with Hadoop by providing graphical access to services such as Spark, Flink, Kafka, HDFS, and YARN. Hopsworks provides self-service Hadoop by introducing two new abstractions: projects and datasets. Users manage membership of projects, which scope access to datasets. Datasets are again managed by users who can safely share them between projects or keep them private within a project. Hopsworks takes the administrator out of the loop for managing data and access to data.

## Information

<ul>
<li><a href="https://twitter.com/hopshadoop">Follow our Twitter account.</a></li>
<li><a href="https://groups.google.com/forum/#!forum/hopshadoop">Join our developer mailing list.</a></li>
<li><a href="https://cloud17.sics.se/jenkins/view/develop/">Checkout the current build status.</a></li>
</ul>

## Installing Hopsworks

#### Single-Machine Deployment
We recommend that you use Vagrant/virtualbox/chef for deploying Hopsworks on a single host. 
These are the instruction for getting started with an Ubuntu 14.04+ server:
```
  sudo apt-get install virtualbox vagrant git
  wget https://packages.chef.io/stable/ubuntu/12.04/chefdk_0.9.0-1_amd64.deb
  sudo dpkg -i chefdk_0.9.0-1_amd64.deb
  git clone https://github.com/hopshadoop/hopsworks-chef.git
  cd hopsworks-chef
  ./run-vagrant.sh
```
When vagrant completes successfully, point your browser at:
```
  http://localhost:8080/hopsworks
  usename: admin@kth.se
  password: admin
```

#### Distributed Deployment
We recommend that you visit www.karamel.io (or www.hops.io) to deploy a Hopsworks cluster.


## Build Requirements (for Ubuntu)
NodeJS server, bower.

```
sudo apt install nodejs-legacy
sudo apt-get install npm
sudo npm cache clean
# You must have a version of bower > 1.54
sudo npm install bower -g
sudo npm install grunt -g
```

## Build with Maven
```
mvn install 
```
Maven uses yeoman-maven-plugin to build both the frontend and the backend.
Maven first executes the Gruntfile in the yo directory, then builds the back-end in Java.
The yeoman-maven-plugin copies the dist folder produced by grunt from the yo directory to the target folder of the backend.
Both the frontend and backend are packaged together in a single war file.

To access the admin page go to index.xhtml.


## Front-end Development Tips

The javascript produced by building maven is obsfuscated. For debugging javascript, we recommend that you use the following script
to deploy changes to HTML or javascript to your vagrant machine:

```
cd scripts
./dev-deploy-frontend.sh
```

You should also add the chef recipe to the end of your Vagrantfile:
```
 hopsworks::dev
```



#### For development

You can build hopsworks without running grunt/bower using:

```
mvn install -P-dist
```

Then run your script to upload your javascript to bbc1:

```
cd scripts
./jim-bbc1-scp.sh
```
