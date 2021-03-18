# Copyright (C) 2020, Logical Clocks AB. All rights reserved
# !/usr/bin/env python
# -*- coding: utf-8 -*-

import sys

arguments = len(sys.argv) - 1
position = 1

print ("Parameters%s" % (len(sys.argv) - 1))
while (arguments >= position):
    print ("Parameter %i: %s" % (position, sys.argv[position]))
    position = position + 1
in_fs_name = None
out_fs_name = None
in_fg_prefix = "fg_1_"
in_fg_count = 2
out_fg_name = "fg_2_1"

if arguments == 5 :
    in_fs_name = sys.argv[1]
    out_fs_name = sys.argv[2]
    in_fg_prefix = sys.argv[3]
    in_fg_count = int(sys.argv[4])
    out_fg_name = sys.argv[5]

from pyspark.sql.types import *
from pyspark.sql import SparkSession
from pyspark import SparkContext, HiveContext
import hsfs

spark = SparkSession.builder.appName("create_synthetic_fg").enableHiveSupport().getOrCreate()
sqlContext = HiveContext(spark.sparkContext)
connection = hsfs.connection()
in_fs = connection.get_feature_store(name=in_fs_name)
if in_fs_name == out_fg_name :
    out_fs = in_fs
else :
    out_fs = connection.get_feature_store(name=out_fs_name)
in_fgs = []
out_fg_query = None
for i in list(range(0,in_fg_count)):
    in_fg_name = in_fg_prefix + str(i)
    in_fgs.append(in_fs.get_feature_group(in_fg_name))
    if out_fg_query is None:
        out_fg_query = in_fgs[i].select_all()
    else:
        out_fg_query = out_fg_query.join(in_fgs[i].select_all())
out_fg = out_fs.create_feature_group(out_fg_name, version=1, description="synthetic featuregroup",  primary_key=['id'],
                                  time_travel_format=None, statistics_config=False)
out_fg.save(out_fg_query.read())

connection.close()
spark.stop()