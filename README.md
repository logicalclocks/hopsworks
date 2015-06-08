# HopsWorks - Frontend
André More & Ermias Gebremeskel

## Build & development

This application needs to run on the same port as the backend.

To accomplish this, you need to copy the *app* folder and *bower.json* file to GlassFish *docroot* folder.
Then run *bower install* this will add all the dependencies to the *bower_components* folder.
Then run the *hopsworksBackend*.
To access the application you simply go to *http://localhost:8080/app*.

### Adding dependencies
 * Using *bower install <package> --save* will add <package> to the project’s *bower.json* dependencies array.
 * Similarly, using *bower install <package> --save-dev* will add <package> to the project’s *bower.json* 
   devDependencies(Development dependencies) array.

 * This will let others install the new dependencies by running 
   *bower install*

