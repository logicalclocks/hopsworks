# Hopsworks High Availability and Disaster Recovery Documentation

The Hopsworks Feature Store is the underlying component powering enterprise ML pipelines as well as serving feature data to model making user facing predictions. Sometimes the Hopsworks cluster can experience hardware failures or power loss, to help you plan for these occasions and avoid Hopsworks Feature Store downtime, we put together this guide. This guide is divided into three sections:

* **High availability**: deployment patterns and best practices to make sure individual component failures do not impact the availability of the Hopsworks cluster.
* **Backup**: configuration policies and best practices to make sure you have have fresh copy of the data and metadata in case of necessity
* **Restore**: procedures and best practices to restore a previous backup if needed.