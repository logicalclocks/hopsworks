#!/bin/bash
export MASTER=yarn
export ZEPPELIN_JAVA_OPTS=""
export SPARK_HOME=%%spark_dir%%
export HADOOP_HOME=%%hadoop_dir%%
export HADOOP_CONF_DIR=%%hadoop_dir%%/etc/hadoop
export HADOOP_USER_NAME=%%hadoop_user%%