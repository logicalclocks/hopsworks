## Hopsworks Dockerfiles

This directory contains Dockerfiles for deploying hopsworks on kubernetes.

### Migration
Migration contains Dockerfile and scripts to create, upgrade and delete the hopsworks database from the database.

#### Build image

```sh
sudo docker build --build-arg version=3.5.0 -f Dockerfile -t migration:3.5.0-SNAPSHOT .
```

### Payara server and node
Payara Server contains Dockerfile and scripts to create and configure the DAS node. 
Payara node contains Dockerfile to create and configure the worker node.
#### Build images
```sh
sudo docker build -f Dockerfile -t payara:"${PAYARA_VERSION}" .
sudo docker build -f Dockerfile -t payara-node:"${PAYARA_VERSION}" .
```

### Deploy

Deploy contains Dockerfile and script to deploy hopsworks-ear and hopsworks-ca to payara in a kubernetes cluster.
#### Build image
```sh
sudo docker build -f Dockerfile -t hopsworks-deploy:"${PAYARA_VERSION}" .
```

### Main Dockerfile

The main Dockerfile is used to create a DAS node with the specified version of hopsworks. The hopsworks-ear and 
hopsworks-ca will be downloaded from _download_url_ and the hopsworks front-end from _frontend_download_url_.


#### Build image
```sh
sudo docker build --build-arg version=3.5.0-SNAPSHOT --build-arg user="$USERNAME" --build-arg password="$PASSWORD" --build-arg download_url=https://nexus.hops.works/repository/hopsworks -f Dockerfile -t hopsworks:3.5.0-SNAPSHOT .
```