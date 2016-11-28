#!/bin/bash
export MASTER=yarn
export ZEPPELIN_JAVA_OPTS=""
export SPARK_HOME=%%spark_dir%%
export HADOOP_HOME=%%hadoop_dir%%
export HADOOP_CONF_DIR=%%hadoop_dir%%/etc/hadoop
export HADOOP_USER_NAME=%%hadoop_user%%
export JAVA_HOME=%%java_home%%
export HADOOP_HDFS_HOME=%%hadoop_dir%%
export LD_LIBRARY_PATH=%%ld_library_path%%:%%java_home%%/jre/lib/amd64/server:/usr/local/cuda/lib64/
export CLASSPATH=%%hadoop_classpath_global%%:$CLASSPATH