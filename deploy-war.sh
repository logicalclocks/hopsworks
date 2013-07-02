#!/bin/bash
#mvn war:war
mvn clean install
scp target/kthfs-dashboard.war glassfish@lucan.sics.se:/var/www/kthfs
