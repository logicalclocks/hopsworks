# Copyright (C) 2020, Logical Clocks AB. All rights reserved
# !/usr/bin/env python
# -*- coding: utf-8 -*-

from pyspark.sql import SparkSession
from hops import hdfs

spark = SparkSession.builder.appName("hello_world_app").getOrCreate()
print("hello world")
project_path = hdfs.project_path()
mydf = spark.createDataFrame(["10","11","13"], "string").toDF("age")
mydf.write.csv(project_path + '/Resources/mycsv2.csv')