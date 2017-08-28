package io.hops.hopsworks.common.jobs.spark;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.jobs.description.JobDescription;
import io.hops.hopsworks.common.jobs.AsynchronousJobExecutor;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hadoop.yarn.api.records.LocalResourceType;
import org.apache.hadoop.yarn.api.records.LocalResourceVisibility;

/**
 * Builder class for a Spark YarnRunner. Implements the common logic needed
 * for any Spark job to be started and builds a YarnRunner instance.
 * <p>
 */
public class SparkYarnRunnerBuilder {

  private static final Logger LOG = Logger.getLogger(
      SparkYarnRunnerBuilder.class.getName());

  //Necessary parameters
  private final JobDescription jobDescription;

  //Optional parameters
  private final List<String> jobArgs = new ArrayList<>();
  private String jobName = "Untitled Spark Job";
  private final List<LocalResourceDTO> extraFiles = new ArrayList<>();
  private int numberOfExecutors = 1;
  private int numberOfExecutorsMin = Settings.SPARK_MIN_EXECS;
  private int numberOfExecutorsMax = Settings.SPARK_MAX_EXECS;
  private int numberOfExecutorsInit = Settings.SPARK_INIT_EXECS;
  private int executorCores = 1;
  private String properties;
  private boolean dynamicExecutors;
  private String executorMemory = "512m";
  private int driverMemory = 1024; // in MB
  private int driverCores = 1;
  private String driverQueue;
  private int numOfGPUs = 0;
  private int numOfPs = 0;
  private final Map<String, String> envVars = new HashMap<>();
  private final Map<String, String> sysProps = new HashMap<>();
  private String classPath;
  private ServiceProperties serviceProps;
  private String sessionId;
  final private Set<String> blacklistedProps = new HashSet<>();

  public SparkYarnRunnerBuilder(JobDescription jobDescription) {
    this.jobDescription = jobDescription;
    SparkJobConfiguration jobConfig = (SparkJobConfiguration) jobDescription.
        getJobConfig();

    if (jobConfig.getAppPath() == null || jobConfig.getAppPath().isEmpty()) {
      throw new IllegalArgumentException(
          "Path to application executable cannot be empty!");
    }
    if (jobConfig.getMainClass() == null || jobConfig.getMainClass().isEmpty()) {
      throw new IllegalArgumentException(
          "Name of the main class cannot be empty!");
    }

  }

  /**
   * Get a YarnRunner instance that will launch a Spark job.
   *
   * @param project name of the project
   * @param sparkUser
   * @param jobUser
   * @param sparkDir
   * @param services
   * @param settings
   * @return The YarnRunner instance to launch the Spark job on Yarn.
   * @throws IOException If creation failed.
   */
  public YarnRunner getYarnRunner(String project, String sparkUser,
      String jobUser, final String sparkDir,
      AsynchronousJobExecutor services,
      Settings settings)
      throws IOException {

    //Read blacklisted properties from local spark dir
    File blacklist = new File(sparkDir + "/" + Settings.SPARK_BLACKLISTED_PROPS);
    try (InputStream is = new FileInputStream(blacklist)) {
      byte[] data = new byte[(int) blacklist.length()];
      is.read(data);
      String content = new String(data, "UTF-8");
      blacklistedProps.addAll(Arrays.asList(content.split("\n")));
    }

    JobType jobType = ((SparkJobConfiguration) jobDescription.getJobConfig()).
        getType();
    String appPath = ((SparkJobConfiguration) jobDescription.getJobConfig()).
        getAppPath();

    String hdfsSparkJarPath = Settings.getHdfsSparkJarPath(sparkUser);
    String log4jPath = Settings.getSparkLog4JPath(sparkUser);
    String metricsPath = Settings.getSparkMetricsPath(sparkUser);
    StringBuilder pythonPath = null;
    StringBuilder pythonPathExecs = null;
    //Create a builder
    YarnRunner.Builder builder = new YarnRunner.Builder(Settings.SPARK_AM_MAIN);
    builder.setJobType(jobType);

    String stagingPath = "/Projects/ " + project + "/"
        + Settings.PROJECT_STAGING_DIR + "/.sparkjobstaging";
    builder.localResourcesBasePath(stagingPath);

    builder.addLocalResource(new LocalResourceDTO(
        Settings.SPARK_LOCALIZED_LIB_DIR, hdfsSparkJarPath,
        LocalResourceVisibility.PRIVATE.toString(),
        LocalResourceType.ARCHIVE.toString(), null), false);
    //Add log4j
    builder.addLocalResource(new LocalResourceDTO(
        Settings.SPARK_LOG4J_PROPERTIES, log4jPath,
        LocalResourceVisibility.APPLICATION.toString(),
        LocalResourceType.FILE.toString(), null), false);
    //Add metrics
    builder.addLocalResource(new LocalResourceDTO(
        Settings.SPARK_METRICS_PROPERTIES, metricsPath,
        LocalResourceVisibility.PRIVATE.toString(),
        LocalResourceType.FILE.toString(), null), false);

    //Add app file
    String appExecName = null;
    if (jobType == JobType.SPARK) {
      appExecName = Settings.SPARK_LOCRSC_APP_JAR;
    } else if (jobType == JobType.PYSPARK || jobType == JobType.TFSPARK) {
      builder.addLocalResource(new LocalResourceDTO(
          Settings.PYSPARK_ZIP,
          Settings.getPySparkLibsPath(sparkUser) + File.separator
          + Settings.PYSPARK_ZIP,
          LocalResourceVisibility.APPLICATION.toString(),
          LocalResourceType.ARCHIVE.toString(), null), false);

      builder.addLocalResource(new LocalResourceDTO(
          Settings.PYSPARK_PY4J,
          Settings.getPySparkLibsPath(sparkUser) + File.separator
          + Settings.PYSPARK_PY4J,
          LocalResourceVisibility.APPLICATION.toString(),
          LocalResourceType.ARCHIVE.toString(), null), false);
      if (jobType == JobType.TFSPARK) {
        LocalResourceDTO pythonZip = new LocalResourceDTO(
            Settings.TFSPARK_PYTHON_NAME,
            Settings.getPySparkLibsPath(sparkUser) + File.separator
            + Settings.TFSPARK_PYTHON_ZIP,
            LocalResourceVisibility.APPLICATION.toString(),
            LocalResourceType.ARCHIVE.toString(), null);

        builder.addLocalResource(pythonZip, false);
        extraFiles.add(pythonZip);
        LocalResourceDTO tfsparkZip = new LocalResourceDTO(
            Settings.TFSPARK_ZIP,
            Settings.getPySparkLibsPath(sparkUser) + File.separator
            + Settings.TFSPARK_ZIP,
            LocalResourceVisibility.APPLICATION.toString(),
            LocalResourceType.ARCHIVE.toString(), null);
        builder.addLocalResource(tfsparkZip, false);
        extraFiles.add(tfsparkZip);
        if (System.getenv().containsKey("LD_LIBRARY_PATH")) {
          addSystemProperty(Settings.SPARK_EXECUTORENV_LD_LIBRARY_PATH, System.
              getenv("LD_LIBRARY_PATH"));
        } else {
          addSystemProperty(Settings.SPARK_EXECUTORENV_LD_LIBRARY_PATH,
              "$JAVA_HOME/jre/lib/amd64/server");
        }
      }
      pythonPath = new StringBuilder();
      pythonPath
          .append(Settings.SPARK_LOCALIZED_PYTHON_DIR)
          .append(File.pathSeparator).append(Settings.PYSPARK_ZIP)
          .append(File.pathSeparator).append(Settings.PYSPARK_PY4J);
      pythonPathExecs = new StringBuilder();
      pythonPathExecs
          .append(Settings.SPARK_LOCALIZED_PYTHON_DIR)
          .append(File.pathSeparator).append(Settings.PYSPARK_ZIP)
          .append(File.pathSeparator).append(Settings.PYSPARK_PY4J);
      //set app file from path
      appExecName = appPath.substring(appPath.lastIndexOf(File.separator) + 1);

      addSystemProperty(Settings.SPARK_APP_NAME_ENV, jobName);
      addSystemProperty(Settings.SPARK_YARN_IS_PYTHON_ENV, "true");

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
          settings.getHopsUtilFilename(), settings.getHopsUtilHdfsPath(
          sparkUser),
          LocalResourceVisibility.APPLICATION.toString(),
          LocalResourceType.FILE.toString(), null), false);

      builder.addToAppMasterEnvironment(YarnRunner.KEY_CLASSPATH,
          settings.getHopsUtilFilename());
      extraClassPathFiles.append(settings.getHopsUtilFilename()).append(
          File.pathSeparator);
    }
    builder.addToAppMasterEnvironment(YarnRunner.KEY_CLASSPATH,
        "$PWD/" + Settings.SPARK_LOCALIZED_CONF_DIR + File.pathSeparator
        + Settings.SPARK_LOCALIZED_CONF_DIR
        + File.pathSeparator + Settings.SPARK_LOCALIZED_LIB_DIR + "/*"
        + File.pathSeparator + Settings.SPARK_LOCRSC_APP_JAR
        + File.pathSeparator + Settings.SPARK_LOG4J_PROPERTIES
        + File.pathSeparator + Settings.SPARK_METRICS_PROPERTIES
    );

    //Add extra files to local resources, use filename as key
    for (LocalResourceDTO dto : extraFiles) {
      if (dto.getName().equals(Settings.K_CERTIFICATE) || dto.getName().equals(
          Settings.T_CERTIFICATE)) {
        //Set deletion to true so that certs are removed
        builder.addLocalResource(dto, true);
      } else {
        if (jobType == JobType.PYSPARK || jobType == JobType.TFSPARK) {
          //For PySpark jobs prefix the resource name with __pyfiles__ as spark requires that.
          //github.com/apache/spark/blob/v2.1.0/yarn/src/main/scala/org/apache/spark/deploy/yarn/Client.scala#L624
          if (dto.getName().endsWith(".py")) {
            dto.setName(Settings.SPARK_LOCALIZED_PYTHON_DIR + File.separator
                + dto.getName());
          } else {
            pythonPath.append(File.pathSeparator).append(dto.getName());
            pythonPathExecs.append(File.pathSeparator).append(dto.getName());
          }
          if (dto.getPath().endsWith(".jar")) {
            secondaryJars.append(dto.getName()).append(",");
          }
        }
        builder.addToAppMasterEnvironment(YarnRunner.KEY_CLASSPATH, dto.getName());
        extraClassPathFiles.append(dto.getName()).append(File.pathSeparator);
      }
      builder.addLocalResource(dto, !appPath.startsWith("hdfs:"));
    }

    //Set Spark specific environment variables
    builder.addToAppMasterEnvironment("SPARK_YARN_MODE", "true");
    builder.addToAppMasterEnvironment("SPARK_YARN_STAGING_DIR", stagingPath);
    builder.addToAppMasterEnvironment("SPARK_USER", jobUser);

    //Set TensorFlowOnSpark required environment variables
    if (jobType == JobType.TFSPARK) {

      //Should always be false in case of TFoS
      dynamicExecutors = false;

      //No point in retrying since clusterspec is static
      addSystemProperty(Settings.SPARK_MAX_APP_ATTEMPTS, "1");

      //This is needed to solve a bug where the driver is able to allocate GPUs
      builder.addToAppMasterEnvironment("CUDA_VISIBLE_DEVICES", "''");

      //The following configuration is based on:
      //https://github.com/yahoo/TensorFlowOnSpark/wiki/GetStarted_YARN

      //IMPORTANT, if TFoS still can't find cuda libraries there may be issues with cuda installation
      // 1. ssh to machine where the container failed
      // 2. Make sure /usr/local/cuda exists and is a symlink pointing to e.g. /usr/local/cuda-8.0
      // 3. Make sure /etc/ld.so.conf.d directory on the host has an entry pointing to /usr/local/cuda/lib64
      // 4. Run 'sudo ldconfig'

      String binCuda = settings.getCudaDir() + "/bin";
      String libCuda = settings.getCudaDir() + "/lib64";
      String libJVM = settings.getJavaHome() + "/jre/lib/amd64/server";
      String libHDFS = settings.getHadoopDir() + "/lib/native";

      builder.addToAppMasterEnvironment("PATH", binCuda + ":$PATH");
      builder.addToAppMasterEnvironment("LD_LIBRARY_PATH", libCuda);

      addSystemProperty(Settings.SPARK_EXECUTORENV_PATH, binCuda + ":$PATH");
      addSystemProperty(Settings.SPARK_EXECUTORENV_LD_LIBRARY_PATH,
              libCuda + ":" + libJVM + ":" + libHDFS);
    }

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

    //These properties are set so that spark history server picks them up
    addSystemProperty(Settings.SPARK_DRIVER_MEMORY_ENV, Integer.toString(
        driverMemory) + "m");
    addSystemProperty(Settings.SPARK_DRIVER_CORES_ENV, Integer.toString(
        driverCores));
    addSystemProperty(Settings.SPARK_EXECUTOR_MEMORY_ENV, executorMemory);
    addSystemProperty(Settings.SPARK_EXECUTOR_CORES_ENV, Integer.toString(
        executorCores));
    addSystemProperty(Settings.SPARK_DRIVER_STAGINGDIR_ENV, stagingPath);
    //Add log4j property
    addSystemProperty(Settings.SPARK_LOG4J_CONFIG,
        Settings.SPARK_LOG4J_PROPERTIES);
    //Comma-separated list of attributes sent to Logstash
    addSystemProperty(Settings.LOGSTASH_JOB_INFO, project.toLowerCase() + ","
        + jobName + ","
        + jobDescription.getId() + "," + YarnRunner.APPID_PLACEHOLDER);
    addSystemProperty(Settings.HOPSWORKS_APPID_PROPERTY,
        YarnRunner.APPID_PLACEHOLDER);
    addSystemProperty(Settings.SPARK_JAVA_LIBRARY_PROP, services.getSettings().
        getHadoopDir() + "/lib/native/");

    //Set executor extraJavaOptions to make parameters available to executors
    StringBuilder extraJavaOptions = new StringBuilder();
    extraJavaOptions.append("'-Dspark.executor.extraJavaOptions=").
        append("-D").append(Settings.SPARK_LOG4J_CONFIG).append("=").append(
        Settings.SPARK_LOG4J_PROPERTIES).
        append(" ").
        append("-D").append(Settings.LOGSTASH_JOB_INFO).append("=").append(
        project.toLowerCase()).append(",").
        append(jobName).append(",").append(jobDescription.getId()).append(
        ",").append(YarnRunner.APPID_PLACEHOLDER).
        append(" ").
        append("-D").append(Settings.SPARK_JAVA_LIBRARY_PROP).append("=").
        append(services.getSettings().getHadoopDir()).
        append("/lib/native/").
        append(" ").
        append("-D").append(Settings.HOPSWORKS_APPID_PROPERTY).append("=").
        append(YarnRunner.APPID_PLACEHOLDER);

    try {
      //Parse user provided properties (if any) and add them to the job
      if (!Strings.isNullOrEmpty(properties)) {
        List<String> propsList = Arrays.asList(properties.split(":"));
        for (String pair : propsList) {
          //Split the pair
          String key = pair.split("=")[0];
          String val = pair.split("=")[1];
          if (blacklistedProps.contains(key)) {
            throw new IOException(
                "This user-provided Spark property is not allowed:" + key);
          }
          addSystemProperty(key, val);
          extraJavaOptions.append(" -D").append(key).append("=").append(val);
        }
      }
    } catch (Exception ex) {
      LOG.log(Level.WARNING,
          "There was an error while setting user-provided Spark properties:{0}",
          ex.getMessage());
      throw new IOException(
          "There was an error while setting user-provided Spark properties. Please check that the values conform to"
          + " the format K1=V1:K2=V2 or that the property is allowed:" + ex.
              getMessage());
    }

    if (serviceProps != null) {

      addSystemProperty(Settings.HOPSWORKS_REST_ENDPOINT_PROPERTY, serviceProps.
          getRestEndpoint());
      addSystemProperty(Settings.HOPSWORKS_KEYSTORE_PROPERTY, Settings.KEYSTORE_VAL_ENV_VAR);
      addSystemProperty(Settings.HOPSWORKS_TRUSTSTORE_PROPERTY, Settings.TRUSTSTORE_VAL_ENV_VAR);
      addSystemProperty(Settings.HOPSWORKS_ELASTIC_ENDPOINT_PROPERTY, serviceProps.
          getElastic().getRestEndpoint());
      addSystemProperty(Settings.HOPSWORKS_JOBNAME_PROPERTY, serviceProps.
          getJobName());
      addSystemProperty(Settings.HOPSWORKS_JOBTYPE_PROPERTY, jobType.getName());
      addSystemProperty(Settings.HOPSWORKS_PROJECTUSER_PROPERTY, jobUser);
      
      extraJavaOptions.append(" -D" + Settings.HOPSWORKS_REST_ENDPOINT_PROPERTY
          + "=").
          append(serviceProps.getRestEndpoint()).
          append(" -D" + Settings.HOPSWORKS_KEYSTORE_PROPERTY + "=").append(Settings.KEYSTORE_VAL_ENV_VAR).
          append(" -D" + Settings.HOPSWORKS_TRUSTSTORE_PROPERTY + "=").append(Settings.TRUSTSTORE_VAL_ENV_VAR).
          append(" -D" + Settings.HOPSWORKS_ELASTIC_ENDPOINT_PROPERTY + "=").append(
          serviceProps.getElastic().getRestEndpoint()).
          append(" -D" + Settings.HOPSWORKS_PROJECTID_PROPERTY + "=").append(
          serviceProps.getProjectId()).
          append(" -D" + Settings.HOPSWORKS_PROJECTNAME_PROPERTY + "=").append(
          serviceProps.getProjectName()).
          append(" -D" + Settings.HOPSWORKS_JOBNAME_PROPERTY + "=").append(
          serviceProps.getJobName()).
          append(" -D" + Settings.HOPSWORKS_JOBTYPE_PROPERTY + "=").append(
          jobType.getName()).
          append(" -D" + Settings.HOPSWORKS_SESSIONID_PROPERTY + "=").append(sessionId).
          append(" -D" + Settings.HOPSWORKS_PROJECTUSER_PROPERTY + "=").append(jobUser);
      //Handle Kafka properties
      if (serviceProps.getKafka() != null) {
        addSystemProperty(Settings.KAFKA_BROKERADDR_ENV_VAR, serviceProps.
            getKafka().getBrokerAddresses());
        addSystemProperty(Settings.KAFKA_JOB_TOPICS_ENV_VAR, serviceProps.
            getKafka().getTopics());
        addSystemProperty(Settings.HOPSWORKS_PROJECTID_PROPERTY, Integer.toString(
            serviceProps.getProjectId()));
        addSystemProperty(Settings.HOPSWORKS_PROJECTNAME_PROPERTY, serviceProps.
            getProjectName());
        addSystemProperty(Settings.HOPSWORKS_SESSIONID_PROPERTY, sessionId);

        addSystemProperty(Settings.KAFKA_CONSUMER_GROUPS, serviceProps.
            getKafka().getConsumerGroups());
        builder.
            addJavaOption(" -D" + Settings.KAFKA_CONSUMER_GROUPS + "="
                + serviceProps.getKafka().getConsumerGroups());
        extraJavaOptions.
            append(" -D" + Settings.KAFKA_BROKERADDR_ENV_VAR + "=").
            append(serviceProps.getKafka().getBrokerAddresses()).
            append(" -D" + Settings.KAFKA_JOB_TOPICS_ENV_VAR + "=").
            append(serviceProps.getKafka().getTopics());
      }
    }
    extraJavaOptions.append("'");
    builder.addJavaOption(extraJavaOptions.toString());

    //Set up command
    StringBuilder amargs = new StringBuilder("--class ");
    amargs.append(((SparkJobConfiguration) jobDescription.getJobConfig()).
        getMainClass());
    //TODO(set app file from path)

    if (jobType == JobType.PYSPARK || jobType == JobType.TFSPARK) {
      amargs.append(" --primary-py-file ").append(appExecName);
      //Check if anaconda is enabled
      if (jobType == JobType.TFSPARK) {
        builder.addToAppMasterEnvironment(Settings.SPARK_PYSPARK_PYTHON, "python");
        addSystemProperty(Settings.SPARK_TF_ENV, "true");
        addSystemProperty(Settings.SPARK_TF_GPUS_ENV, Integer.toString(numOfGPUs));
        addSystemProperty(Settings.SPARK_TF_PS_ENV, Integer.toString(numOfPs));
      } else if (serviceProps.isAnacondaEnabled()) {
        //Add libs to PYTHONPATH
        builder.addToAppMasterEnvironment(Settings.SPARK_PYSPARK_PYTHON,
            serviceProps.getAnaconda().getEnvPath());
      } else {
        //Throw error in Hopswors UI to notify user to enable Anaconda
        throw new IOException(
            "Pyspark job needs to have Python Anaconda environment enabled");
      }
      builder.addToAppMasterEnvironment(Settings.SPARK_PYTHONPATH, pythonPath.toString());
      addSystemProperty(Settings.SPARK_EXECUTORENV_PYTHONPATH, pythonPathExecs.toString());
    }

    Properties sparkProperties = new Properties();
    try (InputStream is = new FileInputStream(sparkDir + "/"
        + Settings.SPARK_CONFIG_FILE)) {
      sparkProperties.load(is);
      //For every property that is in the spark configuration file but is not
      //already set, create a java system property.
      for (String property : sparkProperties.stringPropertyNames()) {
        if (!jobSpecificProperties.contains(property) && sparkProperties.
            getProperty(property) != null
            && !sparkProperties.getProperty(
                property).isEmpty()) {
          addSystemProperty(property, sparkProperties.getProperty(property).
              trim());
        }
      }
    }
    for (String s : sysProps.keySet()) {
      //Exclude "hopsworks.yarn.appid" property because we do not want to 
      //escape it now
      String option;
      if (s.equals(Settings.LOGSTASH_JOB_INFO) || s.equals(Settings.HOPSWORKS_APPID_PROPERTY) || s.equals(
          Settings.SPARK_EXECUTORENV_PYTHONPATH) || s.equals(
              Settings.SPARK_EXECUTORENV_LD_LIBRARY_PATH)) {
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

    return builder.build(sparkDir, JobType.SPARK, services);
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

  /**
   * Parse and set user provided Spark properties.
   *
   * @param properties
   */
  public void setProperties(String properties) {
    this.properties = properties;
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

  public void setNumOfGPUs(int numOfGPUs) {
    this.numOfGPUs = numOfGPUs;
  }

  public void setNumOfPs(int numOfPs) {
    this.numOfPs = numOfPs;
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

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

}
