# Hopsworks

[![Join the chat at https://gitter.im/hopshadoop/hopsworks](https://badges.gitter.im/hopshadoop/services.png)](https://gitter.im/hopshadoop/hopsworks)
[![Google Group](https://img.shields.io/badge/google-group-blue.svg)](https://groups.google.com/forum/#!forum/hopshadoop)


<a href=""><img src="http://www.hops.io/sites/default/files/hops-50x50.png" align="left" hspace="10" vspace="6"></a>
**Hopsworks** is the UI for Hops, a new distribution of Apache Hadoop with scalable, highly available, customizable 
metadata. Hopsworks lowers the barrier to entry for users getting started with Hadoop by providing graphical access to services such as Spark, Flink, Kafka, HDFS, and YARN. HopsWorks provides self-service Hadoop by introducing two new abstractions: projects and datasets. Users manage membership of projects, which scope access to datasets. Datasets are again managed by users who can safely share them between projects or keep them private within a project. Hopsworks takes the administrator out of the loop for managing data and access to data.

## Information

<ul>
<li><a href="https://twitter.com/hopshadoop">Follow our Twitter account.</a></li>
<li><a href="https://groups.google.com/forum/#!forum/hopshadoop">Join our developer mailing list.</a></li>
<li><a href="https://cloud17.sics.se/jenkins/view/develop/">Checkout the current build status.</a></li>
</ul>

## Installing Hopsworks

Hopsworks is part of the Hops Hadoop platform which you can install by following instructions available in 
Hops [documentation](http://hops.readthedocs.io) under *Installation Guide*.

For a local single-node installation, to access Hopsworks just point your browser at:
```
  http://localhost:8080/hopsworks
  usename: admin@kth.se
  password: admin
```

## Build instructions
Hopsworks consists of the backend module which is packaged in two files, `hopsworks.ear`  and `hopsworks-ca.war`,
and the front-end module which is packaged in a single `.war` file. 


### Build Requirements (for Ubuntu)
NodeJS server and bower, both required for building the front-end.

```sh
sudo apt install nodejs-legacy
sudo apt-get install npm
sudo npm cache clean
# You must have a version of bower > 1.54
sudo npm install bower -g
sudo npm install grunt -g
```

### Build with Maven
```sh
mvn install 
```
Maven uses yeoman-maven-plugin to build both the front-end and the backend.
Maven first executes the Gruntfile in the yo directory, then builds the back-end in Java.
The yeoman-maven-plugin copies the dist folder produced by grunt from the yo directory to the target folder of the backend.

You can also build Hopsworks without the frontend (for Java EE development and testing):
```sh
mvn install -P-web
```

## Front-end Development 

The javascript produced by building maven is obsfuscated. For debugging javascript, we recommend that you use the following script
to deploy changes to HTML or javascript to your vagrant machine:

```sh
cd scripts
./js.sh
```

You should also add the chef recipe to the end of your Vagrantfile (or Karamel cluster definition):
```
 hopsworks::dev
```


#### For development

You can build Hopsworks without running grunt/bower using:

```
mvn install -P-dist
```

Then run your script to upload your javascript to snurran.sics.se:

```
cd scripts
./deploy.sh [yourName]
```


## Testing Guide
The following steps must be taken to run Hopsworks integration tests:


```diff
-Warning: This test will clean hdfs and drop Hopsworks database. So it should only be used on a test machine.
```

First create a .env file by copying the .env.example file. Then edit the .env file by providing your specific configuration. 
```sh
   cd hopsworks/hopsworks-ear/test
   cp .env.example .env
```


Then change the properties in Hopsworks parent pom.xml to match the server you are deploying to:
```xml
  <properties>
    ...
    <glassfish.hostname>{hostname}</glassfish.hostname>
    <glassfish.admin>{username}</glassfish.admin>
    <glassfish.passwd>{password}</glassfish.passwd>
    <glassfish.port>{http-port}</glassfish.port>
    <glassfish.admin_port>{admin-ui-port}</glassfish.admin_port>
    <glassfish.domain>{domain}</glassfish.domain>
  </properties>
```

To compile, deploy and run the integration test:
```sh
   cd hopsworks/
   mvn clean install -Pjruby-tests
```

If you have already deployed hopsworks-ear and just want to run the integration test:

```sh
   cd hopsworks/hopsworks-ear/test
   bundle install
   rspec --format html --out ../target/test-report.html
```
To run a single test 
```sh
   cd hopsworks/hopsworks-ear/test
   rspec ./spec/session_spec.rb:60
```
When the test is done if `LAUNCH_BROWSER` is set to true in `.env`, it will open the test report in a browser.

