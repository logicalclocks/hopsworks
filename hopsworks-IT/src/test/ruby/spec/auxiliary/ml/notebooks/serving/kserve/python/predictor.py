# Copyright (C) 2023, Hopsworks AB. All rights reserved

import os
from sklearn.externals import joblib

class Predict(object):

    def __init__(self):
        """Prepare and load a trained model"""
        # NOTE: The env var ARTIFACT_FILES_PATH contains the path to the model artifact files
        
        # load the trained model
        self.model = joblib.load(os.environ["ARTIFACT_FILES_PATH"] + "/iris_knn.pkl")

    def predict(self, inputs):
        """Serve prediction using a trained model"""
        return self.model.predict(inputs).tolist() # Numpy Arrays are note JSON serializable
