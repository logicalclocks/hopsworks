export ZEPPELIN_PORT=8080
export ZEPPELIN_NOTEBOOK_DIR="%%zeppelin_dir%%/notebook"
export ZEPPELIN_INTERPRETERS=spark:com.nflabs.zeppelin.spark.SparkInterpreter,sql:com.nflabs.zeppelin.spark.SparkSqlInterpreter,md:com.nflabs.zeppelin.markdown.Markdown,sh:com.nflabs.zeppelin.shell.ShellInterpreter
export ZEPPELIN_INTERPRETER_DIR="%%zeppelin_dir%%/interpreter"
export MASTER="yarn-client"
export SPARK_HOME="%%spark_dir%%"
export ZEPPELIN_JAVA_OPTS="%%extra_jars%%"
export HADOOP_CONF_DIR="%%hadoop_dir%%/etc/hadoop"