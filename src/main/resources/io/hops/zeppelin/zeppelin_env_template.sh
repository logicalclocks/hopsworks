export ZEPPELIN_PORT=<%= node[:zeppelin][:port] %>
export ZEPPELIN_NOTEBOOK_DIR=notebook
export ZEPPELIN_INTERPRETERS=spark:com.nflabs.zeppelin.spark.SparkInterpreter,sql:com.nflabs.zeppelin.spark.SparkSqlInterpreter,md:com.nflabs.zeppelin.markdown.Markdown,sh:com.nflabs.zeppelin.shell.ShellInterpreter
export ZEPPELIN_INTERPRETER_DIR=interpreter
export MASTER=<%= @spark_master %>
export ZEPPELIN_JAVA_OPTS="-Dspark.jars=/mylib1.jar,/mylib2.jar -Dspark.files=/myfile1.dat,/myfile2.dat"
