#!/bin/sh

RRD_FILES="/var/lib/collectd/rrd"
JARMON="../../jarmon/data"

ln -s $RRD_FILES $JARMON 