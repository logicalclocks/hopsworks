# Copyright (C) 2021, Logical Clocks AB. All rights reserved
# !/usr/bin/env python
# -*- coding: utf-8 -*-

import os
from hops import model

model_path = os.getcwd() + '/model_python'
if not os.path.exists(model_path):
    os.mkdir(model_path)
model.export(model_path, "mnist_python")