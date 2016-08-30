#!/bin/bash
set -e

scp ../target/hopsworks.war glassfish@snurran.sics.se:/var/www/hops/hopsworks-daniel.war
