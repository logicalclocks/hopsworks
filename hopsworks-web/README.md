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

To allow Cross-Origin Resource Sharing for development uncomment the AllowCORSFilter registration line in 
io.hops.hopsworks.rest.application.config.ApplicationConfig then build and redeploy hopsworks-ear
 ```
 package io.hops.hopsworks.rest.application.config;
 ...
 public class ApplicationConfig extends ResourceConfig {
   ...
   public ApplicationConfig() {
    ...
    //uncomment to allow Cross-Origin Resource Sharing
    //register(io.hops.hopsworks.api.filter.AllowCORSFilter.class);
    ...
 ```
#### Build Requirements (for Ubuntu)
NodeJS server and bower, both required for building the front-end.

```sh
sudo apt install nodejs-legacy
sudo apt-get install npm
sudo npm cache clean
# You must have a version of bower > 1.54
sudo npm install bower -g
sudo npm install grunt -g
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
