# Copyright (C) 2023, Hopsworks AB. All rights reserved
class Transformer(object):
    def __init__(self):
        """Initialization code goes here"""
        # NOTE: The env var ARTIFACT_FILES_PATH contains the path to the model artifact files

    def preprocess(self, inputs):
        """Transform the request inputs. The object returned by this method will be used as model input."""
        return inputs

    def postprocess(self, outputs):
        """Transform the predictions computed by the model before returning a response."""
        return outputs