#!/bin/bash

SRC="/var/lib/collectd/rrd"
TARGET1="/home/x/glassfish-3.1.2.2/glassfish/domains/domain1/applications/KTHFSDashboard/jarmon/data"
TARGET2="/home/x/NetBeansProjects/kthfs/KTHFSDashboard/target/KTHFSDashboard/jarmon/data"

echo "Creting Symbolic Links:"
echo $SRC" --> "$TARGET1
echo $SRC" --> "$TARGET2
echo
#ln -s $SRC $TARGET1
ln -s $SRC $TARGET2

rm -rf /var/lib/collectd/rrd/rrd

