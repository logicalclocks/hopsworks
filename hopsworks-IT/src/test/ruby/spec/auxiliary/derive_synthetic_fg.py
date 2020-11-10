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
if arguments >= 1 :
    in_fs_name = sys.argv[1]
out_fs_name = None
if arguments >= 2 :
    out_fs_name = sys.argv[2]
out_fg_name = "fg3"
if arguments >= 3 :
    out_fg_name = sys.argv[3]

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

fg1 = in_fs.get_feature_group("fg1")
fg2 = in_fs.get_feature_group("fg2")
fg3_data = fg1.select_all().join(fg2.select_all()).read()
fg3 = out_fs.create_feature_group(out_fg_name, version=1, description="synthetic featuregroup",  primary_key=['id'], time_travel_format=None)
fg3.save(fg3_data)

connection.close()
spark.stop()