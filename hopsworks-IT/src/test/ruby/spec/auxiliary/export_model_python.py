# Copyright (C) 2021, Logical Clocks AB. All rights reserved
# !/usr/bin/env python
# -*- coding: utf-8 -*-

import os
import hsml

model_path = os.getcwd() + '/model_python'
if not os.path.exists(model_path):
    os.mkdir(model_path)
    
connection = hsml.connection()
mr = connection.get_model_registry()

py_model = mr.python.create_model("mnist_python")

ret = py_model.save(model_path)