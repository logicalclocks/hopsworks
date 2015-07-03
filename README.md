# HopsWorks
HopsWorks Big data Management Platform

#### Build Requirements
NodeJS server:
apt-get install node

## Build with Maven
    mvn install 
Maven uses yeoman-maven-plugin to build both the frontend and the backend.
Maven first executes the Gruntfile in the yo directory, then builds the back-end in Java.
The yeoman-maven-plugin copies the dist folder produced by grunt from the yo directory to the target folder of the backend.
Both the frontend and backend are packaged together in a single war file.

To access the admin page go to index.xhtml.

#### Zeppelin
This branch will allow zeppelin to run on glassfish by providing a NoteBook websocket endpoint on hopsworks.
Login is required to access zeppelin which will be deployed at 
    http://localhost:8080/hopsworks/zeppelin/ 
But access control is not yet implemented. 

###### To test zeppelin 
First clone incubator-zeppelin from git@github.com:apache/incubator-zeppelin.git 
then update the zeppelin-site.xml file found at /hopsworks/src/main/resources/zeppelinConf so that 
the properties 'zeppelin.conf.dir', 'zeppelin.notebook.dir', 'zeppelin.interpreter.dir', and 'zeppelin.interpreter.remoterunner'
find the related directories in incubator-zeppelin folder.  

Then deploy hopsworks as usual login and go to the url given above.
The first time the app is deployed the RemoteInterpreterServer is not yet started so run all paragraphs to start it.

###### Issues 

When deploying the app multiple times the RemoteInterpreterServer needs to be killed manually. If not an exception is thrown,
because the port is in use by the previous RemoteInterpreterServer.

###### TODO

  * Add project based access control.
  * persist notebooks in db.
 
