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
fg1_data = []
fg2_data = []

for i in list(range(1,size)):
    fg1_data.append((i, np.random.normal(), np.random.normal()))
    fg2_data.append((i, np.random.normal(), np.random.normal()))

fg1_spark_df = spark.createDataFrame(fg1_data, ['id','fg1_col1', 'fg1_col2'])
fg1 = fs.create_feature_group("fg1", version=1, description="synthetic fg1", primary_key=['id'],
                              time_travel_format=None, statistics_config=False)
fg1.save(fg1_spark_df)

fg2_spark_df = spark.createDataFrame(fg2_data, ['id','fg2_col1', 'fg2_col2'])
fg2 = fs.create_feature_group("fg2", version=1, description="synthetic fg2", primary_key=['id'],
                              time_travel_format=None, statistics_config=False)
fg2.save(fg2_spark_df)

connection.close()
spark.stop()