# Copyright (C) 2020, Logical Clocks AB. All rights reserved
# !/usr/bin/env python
# -*- coding: utf-8 -*-

import sys
from hops import hdfs

arguments = len(sys.argv) - 1
position = 1

print ("Parameters%s" % (len(sys.argv) - 1))
while (arguments >= position):
    print ("Parameter %i: %s" % (position, sys.argv[position]))
    position = position + 1

td_proj_name = hdfs.project_name()
if arguments >= 1 :
    td_proj_name = sys.argv[1]
td_ds = td_proj_name + '_Training_Datasets'
td = 'mnist_td_1'
if arguments >= 2 :
    td = sys.argv[2]

model_proj_name = None
if arguments >= 3 :
    model_proj_name = sys.argv[3]

model_name = 'mnist_model'
if arguments >= 4 :
    model_name = sys.argv[4]

experiment_name = model_name + '_experiment'

def keras_mnist():
    import os
    import uuid

    import tensorflow as tf

    from hops import tensorboard

    from hops import model as hops_model
    from hops import hdfs

    batch_size=32
    num_classes = 10

    # Provide path to train and validation datasets
    train_filenames = tf.io.gfile.glob(hdfs.project_path(td_proj_name) + '/' + td_ds + '/' + td  + '/train/part-r-*')
    validation_filenames = tf.io.gfile.glob(hdfs.project_path(td_proj_name) + '/' + td_ds + '/' + td + '/validate/part-r-*')

    # Define input function
    def data_input(filenames, batch_size=128, num_classes = 10, shuffle=False, repeat=None):

        def parser(serialized_example):
            """Parses a single tf.Example into image and label tensors."""
            features = tf.io.parse_single_example(
                serialized_example,
                features={
                    'image': tf.io.FixedLenFeature([28 * 28], tf.float32),
                    'label': tf.io.FixedLenFeature([], tf.int64),
                })

            image = tf.cast(features['image'], tf.float32)
            label = tf.cast(features['label'], tf.int32)

            # Create a one hot array for your labels
            label = tf.one_hot(label, num_classes)

            return image, label

        # Import MNIST data
        dataset = tf.data.TFRecordDataset(filenames)

        # Map the parser over dataset, and batch results by up to batch_size
        dataset = dataset.map(parser)
        if shuffle:
            dataset = dataset.shuffle(buffer_size=128)
        dataset = dataset.batch(batch_size, drop_remainder=True)
        dataset = dataset.repeat(repeat)
        return dataset

    # Define a Keras Model.
    model = tf.keras.Sequential()
    model.add(tf.keras.layers.Dense(128, activation='relu', input_shape=(784,)))
    model.add(tf.keras.layers.Dense(num_classes, activation='softmax'))

    # Compile the model.
    model.compile(loss=tf.keras.losses.categorical_crossentropy,
                  optimizer= tf.keras.optimizers.Adam(0.001),
                  metrics=['accuracy']
                  )

    callbacks = [
        tf.keras.callbacks.TensorBoard(log_dir=tensorboard.logdir()),
        tf.keras.callbacks.ModelCheckpoint(filepath=tensorboard.logdir()),
    ]
    model.fit(data_input(train_filenames, batch_size),
              verbose=0,
              epochs=3,
              steps_per_epoch=5,
              validation_data=data_input(validation_filenames, batch_size),
              validation_steps=1,
              callbacks=callbacks
              )

    score = model.evaluate(data_input(validation_filenames, batch_size), steps=1)

    # Export model
    # WARNING(break-tutorial-inline-code): The following code snippet is
    # in-lined in tutorials, please update tutorial documents accordingly
    # whenever code changes.

    export_path = os.getcwd() + '/model-' + str(uuid.uuid4())
    print('Exporting trained model to: {}'.format(export_path))

    tf.saved_model.save(model, export_path)

    print('Done exporting!')

    metrics = {'accuracy': score[1]}

    hops_model.export(export_path, model_name, metrics=metrics, project=model_proj_name)

    return metrics

from hops import experiment
experiment.launch(keras_mnist, name=experiment_name, local_logdir=True, metric_key='accuracy')