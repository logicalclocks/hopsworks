# Copyright (C) 2021, Logical Clocks AB. All rights reserved
# !/usr/bin/env python
# -*- coding: utf-8 -*-

import sys

arguments = len(sys.argv) - 1
position = 1

fg_prefix = "on_demand_fg_1_"
fg_count = 2
dir_name = None
connector_name = None
if arguments == 4 :
    fg_prefix = sys.argv[1]
    fg_count = int(sys.argv[2])
    dir_name = sys.argv[3]
    connector_name = sys.argv[4]

print ("Parameters%s" % (arguments))
while (arguments >= position):
    print ("Parameter %i: %s" % (position, sys.argv[position]))
    position = position + 1

from pyspark.sql.types import *
from pyspark.sql import SparkSession
from pyspark import SparkContext, HiveContext
from hops import hdfs
import hsfs
import numpy as np

spark = SparkSession.builder.appName("create_synthetic_on_demand_fg").enableHiveSupport().getOrCreate()
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
    file_format = "csv"
    file_name = "data" + str(i) + "." + file_format
    fg_spark_df.write.format(file_format).option("header", "true").save(hdfs.project_path()+dir_name+"/"+file_name)
    fg_description = "synthetic " + fg_name
    sc = fs.get_storage_connector(connector_name)
    fg_write = fs.create_on_demand_feature_group(name=fg_name, version=1, description=fg_description, primary_key=['id'],
                                                 data_format=file_format, path=file_name, statistics_config=False,
                                                 storage_connector=sc, options={"header":"true","inferSchema":"true"})
    fg_write.save()

connection.close()
spark.stop()