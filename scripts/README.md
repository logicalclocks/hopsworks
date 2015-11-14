
## update-tables.sh

This script is used to synchronize updates in ../sql/hopsworks.sql to the Chef cookbooks.
Run this script if you update ../sql/hopsworks.sql
You need to be a committer on the hopshadoop/hopsworks-chef project on github to be able to run this script.


## deploy-war.sh

This script creates a new release for hopsworks.war on our http server. You need to have your public
key registered with the server to be authorized to execute this script.

## dev-deploy-frontend.sh

This script is a way to do frontend development without having to republish your war file on glassfish for every update.
