# Copyright (C) 2020, Logical Clocks AB. All rights reserved
# !/usr/bin/env python
# -*- coding: utf-8 -*-

import sys
import gzip
import numpy
import tensorflow as tf
from hops import hdfs

arguments = len(sys.argv) - 1
position = 1

print ("Parameters%s" % (len(sys.argv) - 1))
while (arguments >= position):
    print ("Parameter %i: %s" % (position, sys.argv[position]))
    position = position + 1

td = 'mnist_td'
if arguments >= 1 :
    td = sys.argv[1]
td_dir = "MNIST_data"

def _read32(bytestream):
    dt = numpy.dtype(numpy.uint32).newbyteorder('>')
    return numpy.frombuffer(bytestream.read(4), dtype=dt)[0]

def extract_images(f):
    """
    Extract the images into a 4D uint8 numpy array.
    """
    print('Extracting', f.name)
    with gzip.GzipFile(fileobj=f) as bytestream:
        magic = _read32(bytestream)
        if magic != 2051:
            raise ValueError('Invalid magic number %d in MNIST image file: %s' %
                             (magic, f.name))
        num_images = _read32(bytestream)
        rows = _read32(bytestream)
        cols = _read32(bytestream)
        buf = bytestream.read(rows * cols * num_images)
        data = numpy.frombuffer(buf, dtype=numpy.uint8)
        data = data.reshape(num_images, rows, cols, 1)
        return data

def extract_labels(f, one_hot=False, num_classes=10):
    """
    Extract the labels into a 1D uint8 numpy array.
    """
    print('Extracting', f.name)
    with gzip.GzipFile(fileobj=f) as bytestream:
        magic = _read32(bytestream)
        if magic != 2049:
            raise ValueError('Invalid magic number %d in MNIST label file: %s' %
                             (magic, f.name))
        num_items = _read32(bytestream)
        buf = bytestream.read(num_items)
        labels = numpy.frombuffer(buf, dtype=numpy.uint8)
        return labels

def load_dataset(directory, images_file, labels_file):
    """Download and parse MNIST dataset."""

    with tf.io.gfile.GFile(directory + images_file, 'rb') as f:
        images = extract_images(f)
        images = images.reshape(images.shape[0], images.shape[1] * images.shape[2])
        images = images.astype(numpy.float32)
        images = numpy.multiply(images, 1.0 / 255.0)

    with tf.io.gfile.GFile(directory + labels_file, 'rb') as f:
        labels = extract_labels(f)

    return images, labels

directory = hdfs.project_path() + 'Resources/' + td_dir + '/'

train_images, train_labels = load_dataset(directory, 'train-images-idx3-ubyte.gz', 'train-labels-idx1-ubyte.gz')
test_images, test_labels = load_dataset(directory, 't10k-images-idx3-ubyte.gz', 't10k-labels-idx1-ubyte.gz')

from pyspark.sql.types import *
from pyspark.sql import SparkSession

spark = SparkSession.builder.appName("create_mnist_td").getOrCreate()
data = [(train_images[i].tolist(), int(test_labels[i])) for i in range(len(test_images))]
schema = StructType([StructField("image", ArrayType(FloatType())),
                     StructField("label", LongType())])
df = spark.createDataFrame(data, schema)

import hsfs
connection = hsfs.connection()
fs = connection.get_feature_store()
training_dataset = fs.create_training_dataset(name=td, version=1, data_format='tfrecords',
                                              splits={'train': 0.7, 'test': 0.2, 'validate': 0.1},
                                              statistics_config=False)
training_dataset.save(df)
connection.close()
spark.stop()