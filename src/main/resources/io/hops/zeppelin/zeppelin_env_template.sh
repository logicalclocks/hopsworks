#!/bin/bash
export ZEPPELIN_HOME="%%zeppelin_dir%%/%%project_dir%%"
export ZEPPELIN_CONF_DIR="${ZEPPELIN_HOME}/conf"
export ZEPPELIN_LOG_DIR="${ZEPPELIN_HOME}/logs"
export ZEPPELIN_NOTEBOOK_DIR="${ZEPPELIN_HOME}/notebook"
export ZEPPELIN_PID_DIR="${ZEPPELIN_HOME}/run"
export ZEPPELIN_INTERPRETER_DIR="${ZEPPELIN_HOME}/interpreter"
export ZEPPELIN_INTERPRETERS=org.apache.zeppelin.spark.SparkInterpreter,org.apache.zeppelin.spark.PySparkInterpreter,org.apache.zeppelin.spark.SparkSqlInterpreter,org.apache.zeppelin.spark.DepInterpreter,org.apache.zeppelin.markdown.Markdown,org.apache.zeppelin.angular.AngularInterpreter,org.apache.zeppelin.shell.ShellInterpreter,org.apache.zeppelin.hive.HiveInterpreter,org.apache.zeppelin.tajo.TajoInterpreter,org.apache.zeppelin.flink.FlinkInterpreter,org.apache.zeppelin.lens.LensInterpreter,org.apache.zeppelin.ignite.IgniteInterpreter,org.apache.zeppelin.ignite.IgniteSqlInterpreter,org.apache.zeppelin.cassandra.CassandraInterpreter,org.apache.zeppelin.geode.GeodeOqlInterpreter,org.apache.zeppelin.postgresql.PostgreSqlInterpreter,org.apache.zeppelin.phoenix.PhoenixInterpreter,org.apache.zeppelin.kylin.KylinInterpreter,org.apache.zeppelin.elasticsearch.ElasticsearchInterpreter,org.apache.zeppelin.scalding.ScaldingInterpreter
export MASTER="yarn-client"
export SPARK_HOME="%%spark_dir%%"
export HADOOP_HOME="%%hadoop_dir%%"
export HADOOP_CONF_DIR="%%hadoop_dir%%/etc/hadoop"
export HADOOP_USER_NAME="%%hadoop_user%%"
#export ZEPPELIN_JAVA_OPTS="%%extra_jars%%"