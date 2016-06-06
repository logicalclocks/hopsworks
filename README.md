# HopsWorks
HopsWorks Big data Management Platform


#### Installing Hopsworks

## Single-Machine Deployment
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

## Distributed Deployment
We recommend that you visit www.karamel.io (or www.hops.io) to deploy a Hopsworks cluster.


#### Build Requirements (for Ubuntu)
NodeJS server, bower.

```
sudo apt-get install node
sudo apt-get install npm
sudo npm install bower -g
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


#### Front-end Development Tips

The javascript produced by building maven is obsfuscated. For debugging javascript, we recommend that you use the following script
to deploy changes to HTML or javascript:

```
cd scripts
./dev-deploy-frontend.sh
```
