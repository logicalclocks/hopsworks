# Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

def wrapper():
    import random
    import hsml
    import os
    import uuid
    import pandas
    import numpy
    from hsml.utils.signature import Signature
    from hsml.client.exceptions import RestAPIError

    model_path = os.path.join(os.getcwd(), str(uuid.uuid4()))
    os.mkdir(model_path)
    
    f = open(os.path.join(model_path, "saved_shared_model.pb"), "w+")
    f.write("model")
    f.close()

    connection = hsml.connection()
    mr = connection.get_model_registry(project=model_proj_name)
             
    input_data = {'int_column': [1, 2], 'string_column': ["John", "Jamie"], 'float_column': [0.5, 0.3]}
    input_df = pandas.DataFrame(data=input_data)
             
    predictions = numpy.array([1.1, 20.2, 30.3, 40.4, 50.0, 60.6, 70.7, 0.1])
    signature = Signature(inputs=input_df, predictions=predictions)
             
    model = mr.python.create_model(model_name, input_example=input_df, signature=signature)
    saved_meta_obj = model.save(model_path)
    
    def test_meta_model(meta_obj):
        
        # Should work to download saved meta object
        download_path = meta_obj.download()
        assert 'saved_shared_model.pb' in os.listdir(download_path), "model not in path"
        
        # Check input_example and signatures
        assert len(meta_obj.signature['inputs']['columnar_signature']['columns']) == 3, "signature len incorrect"
        assert meta_obj.signature['predictions']['tensor_signature']['tensor']['data_type'] == "float64", "signature type incorrect"
        assert meta_obj.signature['predictions']['tensor_signature']['tensor']['shape'] == [8], "signature shape incorrect"
        assert len(meta_obj.input_example['columns']) == 3, "input example columns len incorrect"
        
        assert 'dq23r23ard' in meta_obj.program, "string not in program"
        
    # Test model returned from save
    test_meta_model(saved_meta_obj)
    
    api_meta_obj = mr.get_model(saved_meta_obj.name, version=saved_meta_obj.version)
    
    # Test model returned from get_model
    test_meta_model(api_meta_obj)
    
    mr = connection.get_model_registry()
    
    # Make sure model is not accessible in default model registry
    try:
        meta_should_not_exist = mr.get_model(saved_meta_obj.name, version=saved_meta_obj.version)
    except RestAPIError as e:
        if e.response.status_code != 404:
            raise Exception("Should not exist")        
    
from hops import experiment
experiment.launch(wrapper)