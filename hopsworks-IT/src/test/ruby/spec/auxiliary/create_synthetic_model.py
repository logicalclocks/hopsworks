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

model_proj_name = None
if arguments >= 1 :
    model_proj_name = sys.argv[1]

model_name = None
if arguments >= 2 :
    model_name = sys.argv[2]

model_path = None
if arguments >= 3 :
    model_path = sys.argv[3]

def wrapper():
    import random
    import hsml
    connection = hsml.connection()
    mr = connection.get_model_registry(project=model_proj_name)
    model = mr.python.create_model(model_name, metrics={'acc': random.randrange(10000)})
    model.save(model_path)

from hops import experiment
experiment.launch(wrapper)