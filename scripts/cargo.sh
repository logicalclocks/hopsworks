#!/bin/bash

set -e

if [ $# -lt 1 ] ; then
  echo "Usage: <prog> hostname [admin_port port]"
fi

admin_port=16005
port=16005
if [ $# -eq 3 ] ; then
  admin_port=$2
  port=$3
fi

cd ..
mvn -Dglassfish.port=14005 -Dglassfish.admin_port=$admin_port -Dglassfish.hostname=$1 clean install -Pdeploy
cd scripts
./jim-bbc1-scp.sh
