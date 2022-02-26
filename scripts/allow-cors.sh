#!/bin/bash

sed -i.bak -e 's@// register(io\.hops\.hopsworks\.filters\.AllowCORSFilter@ register(io\.hops\.hopsworks\.filters\.AllowCORSFilter@g' ./hopsworks-api/src/main/java/io/hops/hopsworks/rest/application/config/ApplicationConfig.java
sed -i.bak -e 's@//register(io\.hops\.hopsworks\.filters\.AllowCORSFilter@ register(io\.hops\.hopsworks\.filters\.AllowCORSFilter@g' ./hopsworks-remote-user-api/src/main/java/io/hops/hopsworks/remote/user/rest/application/config/ApplicationConfig.java

rm -f ./hopsworks-api/src/main/java/io/hops/hopsworks/rest/application/config/ApplicationConfig.java.bak
rm -f ./hopsworks-remote-user-api/src/main/java/io/hops/hopsworks/remote/user/rest/application/config/ApplicationConfig.java.bak
