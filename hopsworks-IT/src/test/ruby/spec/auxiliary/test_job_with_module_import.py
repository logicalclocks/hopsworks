# Copyright (C) 2023, Hopsworks AB. All rights reserved
# !/usr/bin/env python
# -*- coding: utf-8 -*-
import os
import sys
from hops import jobs
from test_module import hello

str = hello.world()
print(str)

arguments = len(sys.argv) - 1
position = 1

while (arguments >= position):
    print ("Parameter %i: %s" % (position, sys.argv[position]))
    position = position + 1

cwd = os.getcwd()
print("hello world")
#Print to stderr
print("fatal error", file=sys.stderr)

with open("./logs/output.txt", "w") as text_file:
    text_file.write(str(cwd) + "\n")
    text_file.write('\n'.join(sys.argv[1:]))


execs = jobs.get_executions(os.environ["HOPSWORKS_JOB_NAME"], "")

with open("./logs/output.txt", "w") as text_file:
    text_file.write(str(execs))
