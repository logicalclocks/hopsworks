#!/bin/bash

curl -o external/cdnjs.json "http://api.cdnjs.com/libraries?fields=name,assets,filename"
npm test
