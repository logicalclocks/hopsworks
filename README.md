# HopsWorks
HopsWorks Big data Management Platform

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
