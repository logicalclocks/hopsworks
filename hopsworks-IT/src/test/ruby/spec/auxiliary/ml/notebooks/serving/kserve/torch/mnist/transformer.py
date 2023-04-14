# Copyright (C) 2023, Hopsworks AB. All rights reserved

import io
import base64
from typing import List, Dict
from PIL import Image
import torchvision.transforms as transforms
import logging
import kserve

class Transformer(object):
    """ A class object for the data handling activities of Image Classification
    Task and returns a KServe compatible response.

    Args:
        kserve (class object): The Model class from the KServe
        module is passed here.
    """
    def __init__(self):
        logging.basicConfig(level=kserve.constants.KSERVE_LOGLEVEL)

        self.image_processing = transforms.Compose([
            transforms.ToTensor(),
            transforms.Normalize((0.1307,), (0.3081,))
        ])

    def preprocess(self, inputs: Dict) -> Dict:
        """Pre-process activity of the Image Input data.

        Args:
            inputs (Dict): KServe http request

        Returns:
            Dict: Returns the request input after converting it into a tensor
        """
        return {'instances': [self.image_transform(instance) for instance in inputs['instances']]}

    def postprocess(self, inputs: List) -> List:
        """Post process function of Torchserve on the KServe side is
        written here.

        Args:
            inputs (List): The list of the inputs

        Returns:
            List: If a post process functionality is specified, it converts that into
            a list.
        """
        return inputs

    def image_transform(self, instance):
        """converts the input image of Bytes Array into Tensor

        Args:
            instance (dict): The request input to make an inference
            request for.

        Returns:
            list: Returns the data key's value and converts that into a list
            after converting it into a tensor
        """
        byte_array = base64.b64decode(instance["data"])
        image = Image.open(io.BytesIO(byte_array))
        instance["data"] = self.image_processing(image).tolist()
        return instance
