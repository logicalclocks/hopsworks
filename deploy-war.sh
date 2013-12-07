#!/bin/bash
#mvn war:war
mvn clean install
scp target/hop-dashboard.war glassfish@snurran.sics.se:/var/www/hops
