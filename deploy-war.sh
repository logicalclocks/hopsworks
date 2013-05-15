#!/bin/bash
mvn war:war
scp target/KTHFSDashboard.war glassfish@lucan.sics.se:/var/www/kthfs/
