export ZEPPELIN_PORT=%%zeppelin_port%%
export ZEPPELIN_NOTEBOOK_DIR=notebook
export ZEPPELIN_INTERPRETERS=spark:com.nflabs.zeppelin.spark.SparkInterpreter,sql:com.nflabs.zeppelin.spark.SparkSqlInterpreter,md:com.nflabs.zeppelin.markdown.Markdown,sh:com.nflabs.zeppelin.shell.ShellInterpreter
export ZEPPELIN_INTERPRETER_DIR=interpreter
export MASTER="yarn-client"
export SPARK_HOME="%%spark_home%%"
export ZEPPELIN_JAVA_OPTS="%%extra_jars%%"
export HADOOP_CONF_DIR=%%hadoop_dir%%/etc/hadoop