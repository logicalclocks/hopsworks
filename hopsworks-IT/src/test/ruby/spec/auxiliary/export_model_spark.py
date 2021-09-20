# Copyright (C) 2021, Logical Clocks AB. All rights reserved
# !/usr/bin/env python
# -*- coding: utf-8 -*-

import os
from hops import model
from pyspark.sql.types import *
from pyspark.sql import SparkSession

spark = SparkSession.builder.appName("export_model_spark").getOrCreate()
model_path = os.getcwd() + '/model_spark'
if not os.path.exists(model_path):
    os.mkdir(model_path)
model.export(model_path, "mnist_spark")
