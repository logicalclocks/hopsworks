#!/bin/bash
#mvn war:war
mvn clean install
scp target/kthfs-dashboard.war glassfish@snurran.sics.se:/var/www/hops
