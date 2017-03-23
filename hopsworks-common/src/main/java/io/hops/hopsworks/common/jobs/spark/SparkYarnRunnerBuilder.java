package io.hops.hopsworks.common.jobs.spark;

import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.jobs.yarn.LocalResourceDTO;
import io.hops.hopsworks.common.jobs.yarn.ServiceProperties;
import io.hops.hopsworks.common.jobs.yarn.YarnRunner;
import io.hops.hopsworks.common.util.Settings;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.hadoop.yarn.api.records.LocalResourceType;
import org.apache.hadoop.yarn.api.records.LocalResourceVisibility;

/**
 * Builder class for a Spark YarnRunner. Implements the common logic needed
 * for any Spark job to be started and builds a YarnRunner instance.
 * <p>
 */
public class SparkYarnRunnerBuilder {

  //Necessary parameters
  private final String appPath, mainClass;

  //Optional parameters
  private final List<String> jobArgs = new ArrayList<>();
  private String jobName = "Untitled Spark Job";
  private List<LocalResourceDTO> extraFiles = new ArrayList<>();
  private int numberOfExecutors = 1;
  private int numberOfExecutorsMin = Settings.SPARK_MIN_EXECS;
  private int numberOfExecutorsMax = Settings.SPARK_MAX_EXECS;
  private int numberOfExecutorsInit = Settings.SPARK_INIT_EXECS;
  private int executorCores = 1;
  private boolean dynamicExecutors;
  private String executorMemory = "512m";
  private int driverMemory = 1024; // in MB
  private int driverCores = 1;
  private String driverQueue;
  private final Map<String, String> envVars = new HashMap<>();
  private final Map<String, String> sysProps = new HashMap<>();
  private String classPath;
  private String hadoopDir;
  private ServiceProperties serviceProps;

  private JobType jobType;

  public SparkYarnRunnerBuilder(String appPath, String mainClass,
      JobType jobType) {
    if (appPath == null || appPath.isEmpty()) {
      throw new IllegalArgumentException(
          "Path to application executable cannot be empty!");
    }
    if (mainClass == null || mainClass.isEmpty()) {
      throw new IllegalArgumentException(
          "Name of the main class cannot be empty!");
    }
    this.appPath = appPath;
    this.mainClass = mainClass;
    this.jobType = jobType;
  }

  /**
   * Get a YarnRunner instance that will launch a Spark job.
   * <p/>
   * @param project name of the project
   * @param sparkUser
   * @param jobUser
   * @param hadoopDir
   * @param sparkDir
   * @param nameNodeIpPort
   * @return The YarnRunner instance to launch the Spark job on Yarn.
   * @throws IOException If creation failed.
   */
  public YarnRunner getYarnRunner(String project, String sparkUser,
      String jobUser,
      final String hadoopDir, final String sparkDir,
      final String nameNodeIpPort)
      throws IOException {

    String hdfsSparkJarPath = Settings.getHdfsSparkJarPath(sparkUser);
    String log4jPath = Settings.getSparkLog4JPath(sparkUser);
    String metricsPath = Settings.getSparkMetricsPath(sparkUser);
    StringBuilder pythonPath = null;
    StringBuilder pythonPathExecs = null;
    //Create a builder
    YarnRunner.Builder builder = new YarnRunner.Builder(Settings.SPARK_AM_MAIN);
    builder.setJobType(jobType);

    this.hadoopDir = hadoopDir;

    String stagingPath = File.separator + "Projects" + File.separator + project
        + File.separator
        + Settings.PROJECT_STAGING_DIR + File.separator
        + "hopsstaging";
    builder.localResourcesBasePath(stagingPath);

    builder.addLocalResource(new LocalResourceDTO(
        Settings.SPARK_LOCALIZED_LIB_DIR, hdfsSparkJarPath,
        LocalResourceVisibility.PRIVATE.toString(),
        LocalResourceType.ARCHIVE.toString(), null), false);
    //Add log4j
    builder.addLocalResource(new LocalResourceDTO(
        Settings.SPARK_LOG4J_PROPERTIES, log4jPath,
        LocalResourceVisibility.PRIVATE.toString(),
        LocalResourceType.FILE.toString(), null), false);
    //Add metrics
    builder.addLocalResource(new LocalResourceDTO(
        Settings.SPARK_METRICS_PROPERTIES, metricsPath,
        LocalResourceVisibility.PRIVATE.toString(),
        LocalResourceType.FILE.toString(), null), false);

    builder.addLocalResource(new LocalResourceDTO(
        Settings.PYSPARK_ZIP,
        Settings.getPySparkLibsPath(sparkUser) + File.separator + Settings.PYSPARK_ZIP,
        LocalResourceVisibility.APPLICATION.toString(),
        LocalResourceType.ARCHIVE.toString(), null), false);

    builder.addLocalResource(new LocalResourceDTO(
        Settings.PYSPARK_PY4J,
        Settings.getPySparkLibsPath(sparkUser) + File.separator + Settings.PYSPARK_PY4J,
        LocalResourceVisibility.APPLICATION.toString(),
        LocalResourceType.ARCHIVE.toString(), null), false);

    //Add app file
    String appExecName = null;
    if (jobType == JobType.SPARK) {
      appExecName = Settings.SPARK_LOCRSC_APP_JAR;
    } else if (jobType == JobType.PYSPARK) {
      pythonPath = new StringBuilder();
      pythonPath
          .append(Settings.SPARK_LOCALIZED_PYTHON_DIR)
          //          .append("$PWD/").append(Settings.PYSPARK_ZIP).append(File.pathSeparator)
          //          .append("$PWD/").append(Settings.PYSPARK_PY4J)
          //          .append(File.pathSeparator)
          .append(File.pathSeparator).append(Settings.PYSPARK_ZIP)
          .append(File.pathSeparator).append(Settings.PYSPARK_PY4J);
      pythonPathExecs = new StringBuilder();
      pythonPathExecs
          //          .append("{{PWD}}/")
          .append(Settings.SPARK_LOCALIZED_PYTHON_DIR)//.append("<CPS>")
          //          .append("{{PWD}}/").append(Settings.PYSPARK_ZIP).append("<CPS>")
          //          .append("{{PWD}}/").append(Settings.PYSPARK_PY4J)
          //          .append(File.pathSeparator)
          .append(File.pathSeparator).append(Settings.PYSPARK_ZIP)
          .append(File.pathSeparator).append(Settings.PYSPARK_PY4J);
      //set app file from path
      appExecName = appPath.substring(appPath.lastIndexOf(File.separator) + 1);

      addSystemProperty(Settings.SPARK_APP_NAME_ENV, jobName);
      addSystemProperty(Settings.SPARK_YARN_IS_PYTHON_ENV, "true");
      addSystemProperty(Settings.SPARK_EXECUTORENV_LD_LIBRARY_PATH, "$JAVA_HOME/jre/lib/amd64/server");
    }

    builder.addLocalResource(new LocalResourceDTO(
        appExecName, appPath,
        LocalResourceVisibility.APPLICATION.toString(),
        LocalResourceType.FILE.toString(), null),
        !appPath.startsWith("hdfs:"));
    builder.addToAppMasterEnvironment(YarnRunner.KEY_CLASSPATH, "$PWD");
    StringBuilder extraClassPathFiles = new StringBuilder();
    StringBuilder secondaryJars = new StringBuilder();
    //Add hops-util.jar if it is a Kafka job
    if (serviceProps.isKafkaEnabled()) {
      builder.addLocalResource(new LocalResourceDTO(
          Settings.HOPSUTIL_JAR, Settings.getHopsutilPath(sparkUser),
          LocalResourceVisibility.APPLICATION.toString(),
          LocalResourceType.FILE.toString(), null), false);

      builder.addToAppMasterEnvironment(YarnRunner.KEY_CLASSPATH,Settings.HOPSUTIL_JAR);
      extraClassPathFiles.append(Settings.HOPSUTIL_JAR).append(File.pathSeparator);
    }

    //Add extra files to local resources, use filename as key
    for (LocalResourceDTO dto : extraFiles) {
      if (dto.getName().equals(Settings.K_CERTIFICATE) || dto.getName().equals(Settings.T_CERTIFICATE)) {
        //Set deletion to true so that certs are removed
        builder.addLocalResource(dto, true);
      } else {
        if (jobType == JobType.PYSPARK) {
          //For PySpark jobs prefix the resource name with __pyfiles__ as spark requires that.
          //github.com/apache/spark/blob/v2.1.0/yarn/src/main/scala/org/apache/spark/deploy/yarn/Client.scala#L624
          if (dto.getName().endsWith(".py")) {
            dto.setName(Settings.SPARK_LOCALIZED_PYTHON_DIR + File.separator + dto.getName());
          } else {
            pythonPath.append(File.pathSeparator).append(dto.getName());
            pythonPathExecs.append(File.pathSeparator).append(dto.getName());
          }
          if (dto.getPath().endsWith(".jar")) {
            secondaryJars.append(dto.getName()).append(",");
          }
        }
        extraClassPathFiles.append(dto.getName()).append(File.pathSeparator);
      }
      builder.addLocalResource(dto, !appPath.startsWith("hdfs:"));
    }

    builder.addToAppMasterEnvironment(YarnRunner.KEY_CLASSPATH,
        "$PWD/" + Settings.SPARK_LOCALIZED_CONF_DIR + File.pathSeparator
        + Settings.SPARK_LOCALIZED_CONF_DIR
        + File.pathSeparator + Settings.SPARK_LOCALIZED_LIB_DIR + "/*"
        + File.pathSeparator + Settings.SPARK_LOCRSC_APP_JAR
        + File.pathSeparator + Settings.SPARK_LOG4J_PROPERTIES
        + File.pathSeparator + Settings.SPARK_METRICS_PROPERTIES
    );
    //Set Spark specific environment variables
    builder.addToAppMasterEnvironment("SPARK_YARN_MODE", "true");
    builder.addToAppMasterEnvironment("SPARK_YARN_STAGING_DIR", stagingPath);
    builder.addToAppMasterEnvironment("SPARK_USER", jobUser);
//    builder.addToAppMasterEnvironment("SPARK_DIST_CLASSPATH",
//        "\"/srv/hops/hadoop/etc/hadoop:/srv/hops/hadoop-2.7.3/share/hadoop/common/lib/*:"
//        + "/srv/hops/hadoop-2.7.3/share/hadoop/common/*:/srv/hops/hadoop-2.7.3/share/hadoop/hdfs:/srv/hops/"
//        + "hadoop-2.7.3/share/hadoop/hdfs/lib/*:/srv/hops/hadoop-2.7.3/share/hadoop/hdfs/*:/srv/hops/hadoop-2.7.3/"
//        + "share/hadoop/yarn/lib/*:/srv/hops/hadoop-2.7.3/share/hadoop/yarn/*:/srv/hops/hadoop-2.7.3/share/hadoop/"
//        + "mapreduce/lib/*:/srv/hops/hadoop-2.7.3/share/hadoop/mapreduce/*:/contrib/capacity-scheduler/*.jar\"");
    for (String key : envVars.keySet()) {
      builder.addToAppMasterEnvironment(key, envVars.get(key));
    }

    if (extraClassPathFiles.toString().length() > 0) {
      addSystemProperty(Settings.SPARK_EXECUTOR_EXTRACLASSPATH,
          extraClassPathFiles.toString().substring(0, extraClassPathFiles.
              length() - 1));
      if (secondaryJars.length() > 0) {
        addSystemProperty("spark.yarn.secondary.jars",
            secondaryJars.toString().substring(0, secondaryJars.
                length() - 1));
      }
    }

    //If DynamicExecutors are not enabled, set the user defined number 
    //of executors
    if (dynamicExecutors) {
      addSystemProperty(Settings.SPARK_DYNAMIC_ALLOC_ENV, String.valueOf(
          dynamicExecutors));
      addSystemProperty(Settings.SPARK_DYNAMIC_ALLOC_MIN_EXECS_ENV,
          String.valueOf(numberOfExecutorsMin));
      //TODO: Fill in the init and max number of executors. Should it be a per job
      //or global setting?
      addSystemProperty(Settings.SPARK_DYNAMIC_ALLOC_MAX_EXECS_ENV,
          String.valueOf(numberOfExecutorsMax));
      addSystemProperty(Settings.SPARK_DYNAMIC_ALLOC_INIT_EXECS_ENV,
          String.valueOf(numberOfExecutorsInit));
      //Dynamic executors requires the shuffle service to be enabled
      addSystemProperty(Settings.SPARK_SHUFFLE_SERVICE, "true");
      //spark.shuffle.service.enabled
    } else {
      addSystemProperty(Settings.SPARK_NUMBER_EXECUTORS_ENV, Integer.toString(
          numberOfExecutors));
    }

    List<String> jobSpecificProperties = new ArrayList<>();
    jobSpecificProperties.add(Settings.SPARK_NUMBER_EXECUTORS_ENV);
    jobSpecificProperties.add(Settings.SPARK_DRIVER_MEMORY_ENV);
    jobSpecificProperties.add(Settings.SPARK_DRIVER_CORES_ENV);
    jobSpecificProperties.add(Settings.SPARK_EXECUTOR_MEMORY_ENV);
    jobSpecificProperties.add(Settings.SPARK_EXECUTOR_CORES_ENV);

    //These properties are set sot that spark history server picks them up
    addSystemProperty(Settings.SPARK_DRIVER_MEMORY_ENV, Integer.toString(
        driverMemory) + "m");
    addSystemProperty(Settings.SPARK_DRIVER_CORES_ENV, Integer.toString(
        driverCores));
    addSystemProperty(Settings.SPARK_EXECUTOR_MEMORY_ENV, executorMemory);
    addSystemProperty(Settings.SPARK_EXECUTOR_CORES_ENV, Integer.toString(
        executorCores));
    addSystemProperty(Settings.SPARK_DRIVER_STAGINGDIR_ENV, stagingPath);
    //Add log4j property
    addSystemProperty(Settings.SPARK_LOG4J_CONFIG, Settings.SPARK_LOG4J_PROPERTIES);
    //Comma-separated list of attributes sent to Logstash
    addSystemProperty(Settings.LOGSTASH_JOB_INFO, project.toLowerCase() + "," + jobName + ","
        + YarnRunner.APPID_PLACEHOLDER);
    addSystemProperty(Settings.HOPSUTIL_APPID_ENV_VAR, YarnRunner.APPID_PLACEHOLDER);
    addSystemProperty(Settings.SPARK_JAVA_LIBRARY_PROP, this.hadoopDir + "/lib/native/");

    //Set executor extraJavaOptions to make parameters available to executors
    StringBuilder extraJavaOptions = new StringBuilder();
    extraJavaOptions.append("'-Dspark.executor.extraJavaOptions=").
        append("-D").append(Settings.SPARK_LOG4J_CONFIG).append("=").append(Settings.SPARK_LOG4J_PROPERTIES).
        append(" ").
        append("-D").append(Settings.LOGSTASH_JOB_INFO).append("=").append(project.toLowerCase()).append(",").
        append(jobName).append(",").append(YarnRunner.APPID_PLACEHOLDER).
        append(" ").
        append("-D").append(Settings.SPARK_JAVA_LIBRARY_PROP).append("=").append(this.hadoopDir).append("/lib/native/").
        append(" ").
        append("-D").append(Settings.HOPSUTIL_APPID_ENV_VAR).append("=").append(YarnRunner.APPID_PLACEHOLDER);

    if (serviceProps != null) {
      addSystemProperty(Settings.HOPSWORKS_REST_ENDPOINT_ENV_VAR, serviceProps.getRestEndpoint());
      addSystemProperty(Settings.KEYSTORE_PASSWORD_ENV_VAR, serviceProps.getKeystorePwd());
      addSystemProperty(Settings.TRUSTSTORE_PASSWORD_ENV_VAR, serviceProps.getTruststorePwd());
      addSystemProperty(Settings.ELASTIC_ENDPOINT_ENV_VAR, serviceProps.getElastic().getRestEndpoint());
      addSystemProperty(Settings.HOPSUTIL_JOBNAME_ENV_VAR, serviceProps.getJobName());
      addSystemProperty(Settings.HOPSUTIL_JOBTYPE_ENV_VAR, jobType.getName());

      extraJavaOptions.append(" -D" + Settings.HOPSWORKS_REST_ENDPOINT_ENV_VAR + "=").
          append(serviceProps.getRestEndpoint()).
          append(" -D" + Settings.KEYSTORE_PASSWORD_ENV_VAR + "=").append(serviceProps.getKeystorePwd()).
          append(" -D" + Settings.TRUSTSTORE_PASSWORD_ENV_VAR + "=").append(serviceProps.getTruststorePwd()).
          append(" -D" + Settings.ELASTIC_ENDPOINT_ENV_VAR + "=").append(serviceProps.getElastic().getRestEndpoint()).
          append(" -D" + Settings.KAFKA_PROJECTID_ENV_VAR + "=").append(serviceProps.getProjectId()).
          append(" -D" + Settings.KAFKA_PROJECTNAME_ENV_VAR + "=").append(serviceProps.getProjectName()).
          append(" -D" + Settings.KAFKA_PROJECTID_ENV_VAR + "=").append(serviceProps.getProjectId()).
          append(" -D" + Settings.HOPSUTIL_JOBNAME_ENV_VAR + "=").append(serviceProps.getJobName()).
          append(" -D" + Settings.HOPSUTIL_JOBTYPE_ENV_VAR + "=").append(jobType.getName());
      //Handle Kafka properties
      if (serviceProps.getKafka() != null) {
        addSystemProperty(Settings.KAFKA_BROKERADDR_ENV_VAR, serviceProps.getKafka().getBrokerAddresses());
        addSystemProperty(Settings.KAFKA_JOB_TOPICS_ENV_VAR, serviceProps.getKafka().getTopics());
        addSystemProperty(Settings.KAFKA_PROJECTID_ENV_VAR, Integer.toString(serviceProps.getProjectId()));
        addSystemProperty(Settings.KAFKA_PROJECTNAME_ENV_VAR, serviceProps.getProjectName());

        addSystemProperty(Settings.KAFKA_CONSUMER_GROUPS, serviceProps.getKafka().getConsumerGroups());
        builder.
            addJavaOption(" -D" + Settings.KAFKA_CONSUMER_GROUPS + "=" + serviceProps.getKafka().getConsumerGroups());
        extraJavaOptions.
            append(" -D" + Settings.KAFKA_BROKERADDR_ENV_VAR + "=").
            append(serviceProps.getKafka().getBrokerAddresses()).
            append(" -D" + Settings.KAFKA_JOB_TOPICS_ENV_VAR + "=").
            append(serviceProps.getKafka().getTopics());
      }
      extraJavaOptions.append("'");
      builder.addJavaOption(extraJavaOptions.toString());
    }

    //Set up command
    StringBuilder amargs = new StringBuilder("--class ");
    amargs.append(mainClass);
    //TODO(set app file from path)

    if (jobType == JobType.PYSPARK) {
      amargs.append(" --primary-py-file ").append(appExecName);
      //Check if anaconda is enabled
      if (serviceProps.isAnacondaEnabled()) {
        //Add libs to PYTHONPATH
        builder.addToAppMasterEnvironment(Settings.SPARK_PYTHONPATH, pythonPath.toString());
        builder.addToAppMasterEnvironment(Settings.SPARK_PYSPARK_PYTHON, serviceProps.getAnaconda().getEnvPath());
        addSystemProperty(Settings.SPARK_EXECUTORENV_PYTHONPATH, pythonPathExecs.toString());
      } else {
        //Throw error in Hopswors UI to notify user to enable Anaconda
        throw new IOException("Pyspark job needs to have Python Anaconda environment enabled");
      }
    }

    Properties sparkProperties = new Properties();
    try (InputStream is = new FileInputStream(sparkDir + "/" + Settings.SPARK_CONFIG_FILE)) {
      sparkProperties.load(is);
      //For every property that is in the spark configuration file but is not
      //already set, create a java system property.
      for (String property : sparkProperties.stringPropertyNames()) {
        if (!jobSpecificProperties.contains(property) && sparkProperties.
            getProperty(property) != null && !sparkProperties.getProperty(
            property).isEmpty()) {
          addSystemProperty(property,
              sparkProperties.getProperty(property).trim());
        }
      }
    }
    for (String s : sysProps.keySet()) {
      //Exclude "hopsworks.yarn.appid" property because we do not want to 
      //escape it now
      String option;
      if (s.equals(Settings.LOGSTASH_JOB_INFO) || s.equals(Settings.HOPSUTIL_APPID_ENV_VAR) || s.equals(
          Settings.SPARK_EXECUTORENV_PYTHONPATH) || s.equals(Settings.SPARK_EXECUTORENV_LD_LIBRARY_PATH)) {
        option = "-D" + s + "=" + sysProps.get(s);
      } else {
        option = YarnRunner.escapeForShell("-D" + s + "=" + sysProps.get(s));
      }
      builder.addJavaOption(option);
    }

    //Add local resources to spark environment too
    for (String s : jobArgs) {
      amargs.append(" --arg '").append(s).append("'");
    }

    builder.amArgs(amargs.toString());

    //Set up Yarn properties
    builder.amMemory(driverMemory);
    builder.amVCores(driverCores);
    builder.amQueue(driverQueue);

    //Set app name
    builder.appName(jobName);

    return builder.build(hadoopDir, sparkDir, nameNodeIpPort, JobType.SPARK);
  }

  public SparkYarnRunnerBuilder setJobName(String jobName) {
    this.jobName = jobName;
    return this;
  }

  public SparkYarnRunnerBuilder addAllJobArgs(List<String> jobArgs) {
    this.jobArgs.addAll(jobArgs);
    return this;
  }

  public SparkYarnRunnerBuilder addAllJobArgs(String[] jobArgs) {
    this.jobArgs.addAll(Arrays.asList(jobArgs));
    return this;
  }

  public SparkYarnRunnerBuilder addJobArg(String jobArg) {
    jobArgs.add(jobArg);
    return this;
  }

  public SparkYarnRunnerBuilder setExtraFiles(List<LocalResourceDTO> extraFiles) {
    if (extraFiles == null) {
      throw new IllegalArgumentException("Map of extra files cannot be null.");
    }
    this.extraFiles = extraFiles;
    return this;
  }

  public SparkYarnRunnerBuilder addExtraFile(LocalResourceDTO dto) {
    if (dto.getName() == null || dto.getName().isEmpty()) {
      throw new IllegalArgumentException(
          "Filename in extra file mapping cannot be null or empty.");
    }
    if (dto.getPath() == null || dto.getPath().isEmpty()) {
      throw new IllegalArgumentException(
          "Location in extra file mapping cannot be null or empty.");
    }
    this.extraFiles.add(dto);
    return this;
  }

  public SparkYarnRunnerBuilder addExtraFiles(
      List<LocalResourceDTO> projectLocalResources) {
    if (projectLocalResources != null && !projectLocalResources.isEmpty()) {
      this.extraFiles.addAll(projectLocalResources);
    }
    return this;
  }

  public SparkYarnRunnerBuilder setNumberOfExecutors(int numberOfExecutors) {
    if (numberOfExecutors < 1) {
      throw new IllegalArgumentException(
          "Number of executors cannot be less than 1.");
    }
    this.numberOfExecutors = numberOfExecutors;
    return this;
  }

  public SparkYarnRunnerBuilder setExecutorCores(int executorCores) {
    if (executorCores < 1) {
      throw new IllegalArgumentException(
          "Number of executor cores cannot be less than 1.");
    }
    this.executorCores = executorCores;
    return this;
  }

  public boolean isDynamicExecutors() {
    return dynamicExecutors;
  }

  public void setDynamicExecutors(boolean dynamicExecutors) {
    this.dynamicExecutors = dynamicExecutors;
  }

  public int getNumberOfExecutorsMin() {
    return numberOfExecutorsMin;
  }

  public void setNumberOfExecutorsMin(int numberOfExecutorsMin) {
    this.numberOfExecutorsMin = numberOfExecutorsMin;
  }

  public int getNumberOfExecutorsMax() {
    return numberOfExecutorsMax;
  }

  public void setNumberOfExecutorsMax(int numberOfExecutorsMax) {
    if (numberOfExecutorsMax > Settings.SPARK_MAX_EXECS) {
      throw new IllegalArgumentException(
          "Maximum number of  executors cannot be greate than:"
          + Settings.SPARK_MAX_EXECS);
    }
    this.numberOfExecutorsMax = numberOfExecutorsMax;
  }

  public int getNumberOfExecutorsInit() {
    return numberOfExecutorsInit;
  }

  public void setNumberOfExecutorsInit(int numberOfExecutorsInit) {
    this.numberOfExecutorsInit = numberOfExecutorsInit;
  }

  public SparkYarnRunnerBuilder setExecutorMemoryMB(int executorMemoryMB) {
    if (executorMemoryMB < 1) {
      throw new IllegalArgumentException(
          "Executor memory bust be greater than zero.");
    }
    this.executorMemory = "" + executorMemoryMB + "m";
    return this;
  }

  public SparkYarnRunnerBuilder setExecutorMemoryGB(float executorMemoryGB) {
    if (executorMemoryGB <= 0) {
      throw new IllegalArgumentException(
          "Executor memory must be greater than zero.");
    }
    int mem = (int) (executorMemoryGB * 1024);
    this.executorMemory = "" + mem + "m";
    return this;
  }

  /**
   * Set the memory requested for each executor. The given string should have
   * the form of a number followed by a 'm' or 'g' signifying the metric.
   * <p/>
   * @param memory
   * @return
   */
  public SparkYarnRunnerBuilder setExecutorMemory(String memory) {
    memory = memory.toLowerCase();
    if (!memory.endsWith("m") && !memory.endsWith("g")) {
      throw new IllegalArgumentException(
          "Memory string does not follow the necessary format.");
    } else {
      String memnum = memory.substring(0, memory.length() - 1);
      try {
        Integer.parseInt(memnum);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(
            "Memory string does not follow the necessary format.", e);
      }
    }
    this.executorMemory = memory;
    return this;
  }

  public SparkYarnRunnerBuilder setDriverMemoryMB(int driverMemoryMB) {
    if (driverMemoryMB < 1) {
      throw new IllegalArgumentException(
          "Driver memory must be greater than zero.");
    }
    this.driverMemory = driverMemoryMB;
    return this;
  }

  public SparkYarnRunnerBuilder setDriverMemoryGB(int driverMemoryGB) {
    if (driverMemoryGB <= 0) {
      throw new IllegalArgumentException(
          "Driver memory must be greater than zero.");
    }
    int mem = driverMemoryGB * 1024;
    this.driverMemory = mem;
    return this;
  }

  public void setDriverCores(int driverCores) {
    this.driverCores = driverCores;
  }

  public void setDriverQueue(String driverQueue) {
    this.driverQueue = driverQueue;
  }

  public void setServiceProps(ServiceProperties serviceProps) {
    this.serviceProps = serviceProps;
  }

  public SparkYarnRunnerBuilder addEnvironmentVariable(String name, String value) {
    envVars.put(name, value);
    return this;
  }

  public SparkYarnRunnerBuilder addSystemProperty(String name, String value) {
    sysProps.put(name, value);
    return this;
  }

  public SparkYarnRunnerBuilder addToClassPath(String s) {
    if (classPath == null || classPath.isEmpty()) {
      classPath = s;
    } else {
      classPath = classPath + ":" + s;
    }
    return this;
  }

}
