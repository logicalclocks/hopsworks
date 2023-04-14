# Copyright (C) 2023, Hopsworks AB. All rights reserved
import joblib
from hops import hdfs

class Predict(object):

    def __init__(self):
        """ Initializes the serving state, reads a trained model from HDFS"""
        self.model_path = "Models/irisflowerclassifier/1/iris_knn.pkl"
        print("Copying SKLearn model from HDFS to local directory")
        hdfs.copy_to_local(self.model_path)
        print("Reading local SkLearn model for serving")
        self.model = joblib.load("./iris_knn.pkl")
        print("Initialization Complete")


    def predict(self, inputs):
        """ Serves a prediction request usign a trained model"""
        return self.model.predict(inputs).tolist() # Numpy Arrays are note JSON serializable

    def classify(self, inputs):
        """ Serves a classification request using a trained model"""
        return "not implemented"

    def regress(self, inputs):
        """ Serves a regression request using a trained model"""
        return "not implemented"