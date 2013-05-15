#!/usr/bin/env python

'''
Created on Oct 22, 2012

@author: Hamidreza Afzali <afzali@kth.se>
'''

import fileinput
import json
import requests
import sys

alert_url = 'http://lucan.sics.se:8080/KTHFSDashboard/rest/collectd/alert'

# reads from STDIN
alert={}
last_line=False
for line in fileinput.input():
        if line in ['\n', '\r\n']:
                last_line=True
        elif last_line is True:
                alert['Message']=line.strip()
        else:
                s = line.split(': ')
                alert[s[0]]=s[1].strip()

# sends to the RESTful service
try:
        headers = {'content-type': 'application/json'}
        payload=json.dumps(alert)
        requests.post(alert_url, data=payload, headers=headers)
except Exception as err:
    sys.exit(1)
