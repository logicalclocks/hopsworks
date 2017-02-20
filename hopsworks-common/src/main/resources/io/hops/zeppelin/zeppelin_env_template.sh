#!/bin/bash
export MASTER=yarn
export ZEPPELIN_JAVA_OPTS=""
export SPARK_HOME=%%spark_dir%%
export HADOOP_HOME=%%hadoop_dir%%
export HADOOP_CONF_DIR=$HADOOP_HOME/etc/hadoop
export HADOOP_HDFS_HOME=$HADOOP_HOME
export HADOOP_USER_NAME=%%hadoop_username%%
export JAVA_HOME=%%java_home%%
export LD_LIBRARY_PATH=${HADOOP_HOME}/lib/native:${JAVA_HOME}/jre/lib/amd64/server:/usr/local/cuda/lib64:/usr/local/lib:/usr/lib:%%ld_library_path%%
export CLASSPATH=%%hadoop_classpath%%
export SPARK_SUBMIT_OPTIONS="%%spark_options%%"

# This is to get matplotlib to not try and use the local $DISPLAY
export MPLBACKEND="agg"

# These are setting the project-specific conda environment directory for python
export PYSPARK_PYTHON=%%anaconda_env_dir%%
export PYTHONPATH=%%anaconda_env_dir%%