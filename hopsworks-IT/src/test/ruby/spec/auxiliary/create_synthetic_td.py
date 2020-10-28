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
out_td_name = "td"
if arguments >= 3 :
    out_td_name = sys.argv[3]

from pyspark.sql.types import *
from pyspark.sql import SparkSession
from pyspark import SparkContext, HiveContext
import hsfs

spark = SparkSession.builder.appName("create_synthetic_td").enableHiveSupport().getOrCreate()
sqlContext = HiveContext(spark.sparkContext)
connection = hsfs.connection()
in_fs = connection.get_feature_store(name=in_fs_name)
out_fs = connection.get_feature_store(name=out_fs_name)

fg1 = in_fs.get_feature_group("fg1")
fg2 = in_fs.get_feature_group("fg2")
td_query = fg1.select_all().join(fg2.select_all())
td = out_fs.create_training_dataset(out_td_name, version=1, description="synthetic td")
td.save(td_query)

connection.close()
spark.stop()