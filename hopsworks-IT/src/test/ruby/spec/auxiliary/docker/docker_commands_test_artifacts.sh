#!/bin/bash

if [ ! -f Sample.json ]; then
    echo "File:  Sample.json does not exit"
    exit 1
fi

if [ !  -f sampleStore.jks ]; then
    echo "File:  sampleStore.jks does not exit"
    exit 1
fi
