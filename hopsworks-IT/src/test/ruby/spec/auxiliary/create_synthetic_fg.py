# Copyright (C) 2020, Logical Clocks AB. All rights reserved
# !/usr/bin/env python
# -*- coding: utf-8 -*-

import sys

arguments = len(sys.argv) - 1
position = 1

fg_prefix = "fg_1_"
fg_count = 2
if arguments == 2 :
    fg_prefix = sys.argv[1]
    fg_count = int(sys.argv[2])

print ("Parameters%s" % (len(sys.argv) - 1))
while (arguments >= position):
    print ("Parameter %i: %s" % (position, sys.argv[position]))
    position = position + 1

from pyspark.sql.types import *
from pyspark.sql import SparkSession
from pyspark import SparkContext, HiveContext
import hsfs
import numpy as np

spark = SparkSession.builder.appName("create_synthetic_fg").enableHiveSupport().getOrCreate()
sqlContext = HiveContext(spark.sparkContext)
connection = hsfs.connection()
fs = connection.get_feature_store()

size = 10

for i in list(range(0,fg_count)):
    fg_data = []
    for j in list(range(1,size)):
        fg_data.append((j, np.random.normal(), np.random.normal()))
    fg_col_1 = 'fg' + str(i) + "_col1"
    fg_col_2 = 'fg' + str(i) + "_col2"
    fg_name = fg_prefix + str(i)
    fg_spark_df = spark.createDataFrame(fg_data, ['id', fg_col_1, fg_col_2])
    fg_description = "synthetic " + fg_name
    fg = fs.create_feature_group(fg_name, version=1, description=fg_description, primary_key=['id'],
                              time_travel_format=None, statistics_config=False)
    fg.save(fg_spark_df)

connection.close()
spark.stop()