# HopsWorks - Frontend
André More & Ermias Gebremeskel

## Deployment

Run the *deploy.sh* script. For this script to run, you need the environment variable GLASSFISH_HOME to be set to the directory where your glassfish instance is installed (e.g. /home/user/glassfish-4.x).

Alternatively, you can manually:
* Copy the *app* folder to the Glassfish *docroot*.
* Copy *bower.json* to the Glassfish *docroot*.
* Run `bower install` inside the Glassfish *docroot* folder.

## Access the application
Make sure the [HopsWorks backend](https://github.com/hopshadoop/hopsworksBackend/ "HopsWorks backend") is running.
In your browser, navigate to *http://localhost:8080/app*

## Adding dependencies
 * Using `bower install <package> --save` will add &lt;package&gt; to the project’s *bower.json* dependencies array.
 * Similarly, using `bower install <package> --save-dev` will add &lt;package&gt; to the project’s *bower.json* 
   devDependencies(Development dependencies) array.

 * This will let others install the new dependencies by running 
   `bower install`

