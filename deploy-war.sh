#!/bin/bash
mvn war:war
scp target/kthfs-dashboard.war glassfish@lucan.sics.se:/var/www/kthfs
