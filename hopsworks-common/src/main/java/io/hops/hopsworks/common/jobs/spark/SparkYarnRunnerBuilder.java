/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package io.hops.hopsworks.common.jobs.spark;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.exception.CryptoPasswordNotFoundException;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
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
import org.apache.hadoop.yarn.client.api.YarnClient;

/**
 * Builder class for a Spark YarnRunner. Implements the common logic needed
 * for any Spark job to be started and builds a YarnRunner instance.
 * <p>
 */
public class SparkYarnRunnerBuilder {

  private static final Logger LOG = Logger.getLogger(
      SparkYarnRunnerBuilder.class.getName());

  //Necessary parameters
  private final Jobs job;

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
  final private Set<String> blacklistedProps = new HashSet<>();

  public SparkYarnRunnerBuilder(Jobs job) {
    this.job = job;
    SparkJobConfiguration jobConfig = (SparkJobConfiguration) job.
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
   * @param jobUser
   * @param services
   * @param dfsClient
   * @param yarnClient
   * @param settings
   * @return The YarnRunner instance to launch the Spark job on Yarn.
   * @throws IOException If creation failed.
   */
  public YarnRunner getYarnRunner(String project,
      String jobUser, AsynchronousJobExecutor services,
      final DistributedFileSystemOps dfsClient, final YarnClient yarnClient,
      Settings settings)
      throws IOException {

    //Read blacklisted properties from local spark dir
    File blacklist = new File(settings.getSparkDir() + "/" + Settings.SPARK_BLACKLISTED_PROPS);
    try (InputStream is = new FileInputStream(blacklist)) {
      byte[] data = new byte[(int) blacklist.length()];
      is.read(data);
      String content = new String(data, "UTF-8");
      blacklistedProps.addAll(Arrays.asList(content.split("\n")));
    }

    JobType jobType = ((SparkJobConfiguration) job.getJobConfig()).
        getType();
    String appPath = ((SparkJobConfiguration) job.getJobConfig()).
        getAppPath();

    String hdfsSparkJarPath = settings.getHdfsSparkJarPath();
    String log4jPath = settings.getSparkLog4JPath();
    StringBuilder pythonPath = null;
    StringBuilder pythonPathExecs = null;
    //Create a builder
    YarnRunner.Builder builder = new YarnRunner.Builder(Settings.SPARK_AM_MAIN);
    builder.setJobType(jobType);
    builder.setYarnClient(yarnClient);
    builder.setDfsClient(dfsClient);
    builder.setJobUser(jobUser);
    if (settings.getHopsRpcTls()) {
      try {
        String password = services.getBaseHadoopClientsService()
            .getProjectSpecificUserCertPassword(jobUser);
        builder.setKeyStorePassword(password);
        builder.setTrustStorePassword(password);
      } catch (CryptoPasswordNotFoundException ex) {
        LOG.log(Level.SEVERE, ex.getMessage(), ex);
        throw new IOException(ex);
      }
    }

    String stagingPath = "/Projects/" + project + "/"
        + Settings.PROJECT_STAGING_DIR + "/.sparkjobstaging-" + YarnRunner.APPID_PLACEHOLDER;
    builder.localResourcesBasePath(stagingPath);
    //Add hdfs prefix so the monitor knows it should find it there
    builder.addFileToRemove("hdfs://" + stagingPath);
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
        Settings.SPARK_METRICS_PROPERTIES, settings.getSparkConfDir() + "/metrics.properties",
        LocalResourceVisibility.PRIVATE.toString(),
        LocalResourceType.FILE.toString(), null), false);
    //Add Glassfish ca truststore for hopsutil
    builder.addLocalResource(new LocalResourceDTO(
        Settings.DOMAIN_CA_TRUSTSTORE, settings.getGlassfishTrustStoreHdfs(),
        LocalResourceVisibility.PRIVATE.toString(),
        LocalResourceType.FILE.toString(), null), false);

    //Add app file
    String appExecName = null;
    if (jobType == JobType.SPARK) {
      appExecName = Settings.SPARK_LOCRSC_APP_JAR;
    } else if (jobType == JobType.PYSPARK || jobType == JobType.TFSPARK) {
      builder.addLocalResource(new LocalResourceDTO(
          Settings.PYSPARK_ZIP,
          settings.getPySparkLibsPath() + File.separator
          + Settings.PYSPARK_ZIP,
          LocalResourceVisibility.APPLICATION.toString(),
          LocalResourceType.ARCHIVE.toString(), null), false);

      builder.addLocalResource(new LocalResourceDTO(
          Settings.PYSPARK_PY4J,
          settings.getPySparkLibsPath() + File.separator
          + Settings.PYSPARK_PY4J,
          LocalResourceVisibility.APPLICATION.toString(),
          LocalResourceType.ARCHIVE.toString(), null), false);
      if (jobType == JobType.TFSPARK) {
        LocalResourceDTO pythonZip = new LocalResourceDTO(
            Settings.TFSPARK_PYTHON_NAME,
            settings.getPySparkLibsPath() + File.separator
            + Settings.TFSPARK_PYTHON_ZIP,
            LocalResourceVisibility.APPLICATION.toString(),
            LocalResourceType.ARCHIVE.toString(), null);

        builder.addLocalResource(pythonZip, false);
        extraFiles.add(pythonZip);
        LocalResourceDTO tfsparkZip = new LocalResourceDTO(
            Settings.TFSPARK_ZIP,
            settings.getPySparkLibsPath() + File.separator
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
    builder.addLocalResource(new LocalResourceDTO(
        settings.getHopsUtilFilename(), settings.getHopsUtilHdfsPath(),
        LocalResourceVisibility.APPLICATION.toString(),
        LocalResourceType.FILE.toString(), null), false);

    builder.addToAppMasterEnvironment(YarnRunner.KEY_CLASSPATH,
        settings.getHopsUtilFilename());
    extraClassPathFiles.append(settings.getHopsUtilFilename()).append(File.pathSeparator).
        append(settings.getHopsLeaderElectionJarPath()).append(File.pathSeparator);
    builder.addToAppMasterEnvironment(YarnRunner.KEY_CLASSPATH,
        "$PWD/" + Settings.SPARK_LOCALIZED_CONF_DIR + File.pathSeparator
        + Settings.SPARK_LOCALIZED_CONF_DIR
        + File.pathSeparator + Settings.SPARK_LOCALIZED_LIB_DIR + "/*"
        + File.pathSeparator + Settings.SPARK_LOCRSC_APP_JAR
        + File.pathSeparator + Settings.SPARK_LOG4J_PROPERTIES
    );

    //Add extra files to local resources, use filename as key
    for (LocalResourceDTO dto : extraFiles) {
      if (dto.getName().equals(Settings.K_CERTIFICATE)
          || dto.getName().equals(Settings.T_CERTIFICATE)
          || dto.getName().equals(Settings.CRYPTO_MATERIAL_PASSWORD)) {
        //Set deletion to true so that certs are removed
        builder.addLocalResource(dto, true);
      } else {
        if (jobType == JobType.PYSPARK || jobType == JobType.TFSPARK) {
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
        builder.addToAppMasterEnvironment(YarnRunner.KEY_CLASSPATH, dto.getName());
        extraClassPathFiles.append(dto.getName()).append(File.pathSeparator);
      }
      builder.addLocalResource(dto, !appPath.startsWith("hdfs:"));
    }

    //Set Spark specific environment variables
    builder.addToAppMasterEnvironment("SPARK_YARN_MODE", "true");
    builder.addToAppMasterEnvironment("SPARK_YARN_STAGING_DIR", stagingPath);
    builder.addToAppMasterEnvironment("SPARK_USER", jobUser);
    builder.addToAppMasterEnvironment("HADOOP_USER_NAME", jobUser);
    builder.addToAppMasterEnvironment("HDFS_USER", jobUser);
    builder.addToAppMasterEnvironment("HADOOP_HOME", settings.getHadoopSymbolicLinkDir());
    builder.addToAppMasterEnvironment("HADOOP_VERSION", settings.getHadoopVersion());
    addSystemProperty(Settings.SPARK_EXECUTORENV_HADOOP_USER_NAME, jobUser);
    addSystemProperty(Settings.SPARK_EXECUTORENV_HDFS_USER, jobUser);

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
      // 4. /usr/local/cuda/lib64 can be a symlink and should point to the real location with libcu(...).so files
      // 5. Run 'sudo ldconfig'
      String libCuda = settings.getCudaDir() + "/lib64";
      String libJVM = settings.getJavaHome() + "/jre/lib/amd64/server";
      String libHDFS = settings.getHadoopSymbolicLinkDir() + "/lib/native";

      builder.addToAppMasterEnvironment("LD_LIBRARY_PATH", libCuda);

      addSystemProperty(Settings.SPARK_EXECUTORENV_LD_LIBRARY_PATH,
          libCuda + ":" + libJVM + ":" + libHDFS);
    }

    for (String key : envVars.keySet()) {
      builder.addToAppMasterEnvironment(key, envVars.get(key));
    }

    addSystemProperty(Settings.SPARK_EXECUTOR_EXTRACLASSPATH,
        extraClassPathFiles.toString().substring(0, extraClassPathFiles.length() - 1));
    addSystemProperty(Settings.SPARK_DRIVER_EXTRACLASSPATH, settings.getHopsLeaderElectionJarPath());
    
    if (secondaryJars.length() > 0) {
      addSystemProperty(Settings.SPARK_YARN_SECONDARY_JARS,
          secondaryJars.toString().substring(0, secondaryJars.length() - 1));
    }

    //If DynamicExecutors are not enabled, set the user defined number 
    //of executors
    if (dynamicExecutors) {
      addSystemProperty(Settings.SPARK_DYNAMIC_ALLOC_ENV, String.valueOf(dynamicExecutors));
      addSystemProperty(Settings.SPARK_DYNAMIC_ALLOC_MIN_EXECS_ENV, String.valueOf(numberOfExecutorsMin));
      //TODO: Fill in the init and max number of executors. Should it be a per job
      //or global setting?
      addSystemProperty(Settings.SPARK_DYNAMIC_ALLOC_MAX_EXECS_ENV, String.valueOf(numberOfExecutorsMax));
      addSystemProperty(Settings.SPARK_DYNAMIC_ALLOC_INIT_EXECS_ENV, String.valueOf(numberOfExecutorsInit));
      //Dynamic executors requires the shuffle service to be enabled
      addSystemProperty(Settings.SPARK_SHUFFLE_SERVICE, "true");
      //spark.shuffle.service.enabled
    } else {
      addSystemProperty(Settings.SPARK_NUMBER_EXECUTORS_ENV, Integer.toString(numberOfExecutors));
    }

    List<String> jobSpecificProperties = new ArrayList<>();
    jobSpecificProperties.add(Settings.SPARK_NUMBER_EXECUTORS_ENV);
    jobSpecificProperties.add(Settings.SPARK_DRIVER_MEMORY_ENV);
    jobSpecificProperties.add(Settings.SPARK_DRIVER_CORES_ENV);
    jobSpecificProperties.add(Settings.SPARK_EXECUTOR_MEMORY_ENV);
    jobSpecificProperties.add(Settings.SPARK_EXECUTOR_CORES_ENV);

    //These properties are set so that spark history server picks them up
    addSystemProperty(Settings.SPARK_DRIVER_MEMORY_ENV, Integer.toString(driverMemory) + "m");
    addSystemProperty(Settings.SPARK_DRIVER_CORES_ENV, Integer.toString(driverCores));
    addSystemProperty(Settings.SPARK_EXECUTOR_MEMORY_ENV, executorMemory);
    addSystemProperty(Settings.SPARK_EXECUTOR_CORES_ENV, Integer.toString(executorCores));
    addSystemProperty(Settings.SPARK_DRIVER_STAGINGDIR_ENV, stagingPath);
    //Add log4j property
    addSystemProperty(Settings.SPARK_LOG4J_CONFIG, Settings.SPARK_LOG4J_PROPERTIES);
    //Comma-separated list of attributes sent to Logstash
    addSystemProperty(Settings.LOGSTASH_JOB_INFO, project.toLowerCase() + "," + jobName + "," + job.getId()
        + "," + YarnRunner.APPID_PLACEHOLDER);
    addSystemProperty(Settings.HOPSWORKS_APPID_PROPERTY, YarnRunner.APPID_PLACEHOLDER);
    addSystemProperty(Settings.SPARK_JAVA_LIBRARY_PROP, services.getSettings().getHadoopSymbolicLinkDir()
        + "/lib/native/");

    //Set executor extraJavaOptions to make parameters available to executors
    Map<String, String> extraJavaOptions = new HashMap<>();
    extraJavaOptions.put(Settings.SPARK_LOG4J_CONFIG, Settings.SPARK_LOG4J_PROPERTIES);
    extraJavaOptions.put(Settings.LOGSTASH_JOB_INFO, project.toLowerCase() + "," + jobName + "," + job.getId() + ","
        + YarnRunner.APPID_PLACEHOLDER);
    extraJavaOptions.put(Settings.SPARK_JAVA_LIBRARY_PROP, services.getSettings().getHadoopSymbolicLinkDir()
        + "/lib/native/");
    extraJavaOptions.put(Settings.HOPSWORKS_APPID_PROPERTY, YarnRunner.APPID_PLACEHOLDER);

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
          extraJavaOptions.put(key, val);
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
      addSystemProperty(Settings.HOPSWORKS_REST_ENDPOINT_PROPERTY, serviceProps.getRestEndpoint());
      addSystemProperty(Settings.SERVER_TRUSTSTORE_PROPERTY, Settings.SERVER_TRUSTSTORE_PROPERTY);
      addSystemProperty(Settings.HOPSWORKS_ELASTIC_ENDPOINT_PROPERTY, serviceProps.getElastic().getRestEndpoint());
      addSystemProperty(Settings.HOPSWORKS_JOBNAME_PROPERTY, serviceProps.getJobName());
      addSystemProperty(Settings.HOPSWORKS_JOBTYPE_PROPERTY, jobType.getName());
      addSystemProperty(Settings.HOPSWORKS_PROJECTUSER_PROPERTY, jobUser);
      addSystemProperty(Settings.HOPSWORKS_PROJECTID_PROPERTY, Integer.toString(serviceProps.getProjectId()));
      addSystemProperty(Settings.HOPSWORKS_PROJECTNAME_PROPERTY, serviceProps.getProjectName());

      extraJavaOptions.put(Settings.HOPSWORKS_REST_ENDPOINT_PROPERTY, serviceProps.getRestEndpoint());
      extraJavaOptions.put(Settings.SERVER_TRUSTSTORE_PROPERTY, Settings.SERVER_TRUSTSTORE_PROPERTY);
      extraJavaOptions.put(Settings.HOPSWORKS_ELASTIC_ENDPOINT_PROPERTY, serviceProps.getElastic().getRestEndpoint());
      extraJavaOptions.put(Settings.HOPSWORKS_PROJECTID_PROPERTY, Integer.toString(serviceProps.getProjectId()));
      extraJavaOptions.put(Settings.HOPSWORKS_PROJECTNAME_PROPERTY, serviceProps.getProjectName());
      extraJavaOptions.put(Settings.HOPSWORKS_JOBNAME_PROPERTY, serviceProps.getJobName());
      extraJavaOptions.put(Settings.HOPSWORKS_JOBTYPE_PROPERTY, jobType.getName());
      extraJavaOptions.put(Settings.HOPSWORKS_PROJECTUSER_PROPERTY, jobUser);

      //Handle Kafka properties
      if (serviceProps.getKafka() != null) {
        addSystemProperty(Settings.KAFKA_BROKERADDR_PROPERTY, serviceProps.getKafka().getBrokerAddresses());
        addSystemProperty(Settings.KAFKA_JOB_TOPICS_PROPERTY, serviceProps.getKafka().getTopics());
        addSystemProperty(Settings.KAFKA_CONSUMER_GROUPS, serviceProps.getKafka().getConsumerGroups());
        extraJavaOptions.put(Settings.KAFKA_BROKERADDR_PROPERTY, serviceProps.getKafka().getBrokerAddresses());
        extraJavaOptions.put(Settings.KAFKA_JOB_TOPICS_PROPERTY, serviceProps.getKafka().getTopics());
        extraJavaOptions.put(Settings.KAFKA_CONSUMER_GROUPS, serviceProps.getKafka().getConsumerGroups());

      }
    }

    //Set up command
    StringBuilder amargs = new StringBuilder("--class ");
    amargs.append(((SparkJobConfiguration) job.getJobConfig()).
        getMainClass());

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
        builder.addToAppMasterEnvironment(Settings.SPARK_PYSPARK_PYTHON, serviceProps.getAnaconda().getEnvPath());
      } else {
        //Throw error in Hopswors UI to notify user to enable Anaconda
        throw new IOException("Pyspark job needs to have Python Anaconda environment enabled");
      }
      builder.addToAppMasterEnvironment(Settings.SPARK_PYTHONPATH, pythonPath.toString());
      addSystemProperty(Settings.SPARK_EXECUTORENV_PYTHONPATH, pythonPathExecs.toString());
    }

    //Parse properties from Spark config file
    Properties sparkProperties = new Properties();
    try (InputStream is = new FileInputStream(settings.getSparkDir() + "/" + Settings.SPARK_CONFIG_FILE)) {
      sparkProperties.load(is);
      //For every property that is in the spark configuration file but is not already set, create a system property.
      for (String property : sparkProperties.stringPropertyNames()) {
        if (!jobSpecificProperties.contains(property)
            && sparkProperties.getProperty(property) != null
            && !sparkProperties.getProperty(property).isEmpty()
            && !extraJavaOptions.containsKey(property)) {
          addSystemProperty(property, sparkProperties.getProperty(property).trim());
        }
      }
    }
    
    
    //Create a string with system properties from extraJavaOptions
    StringBuilder extraJavaOptionsSb = new StringBuilder();
    extraJavaOptionsSb.append("'-D"+Settings.SPARK_EXECUTOR_EXTRA_JAVA_OPTS+"=");
    for (String key : extraJavaOptions.keySet()) {
      extraJavaOptionsSb.append(" -D").append(key).append("=").append(extraJavaOptions.get(key)).append(" ");
    }
    extraJavaOptionsSb.append("'");
    extraJavaOptionsSb = extraJavaOptionsSb.deleteCharAt(extraJavaOptionsSb.lastIndexOf(","));
    builder.addJavaOption(extraJavaOptionsSb.toString());
    
    for (String s : sysProps.keySet()) {
      //Exclude "hopsworks.yarn.appid" property because we do not want to 
      //escape it now
      String option;
      if (s.equals(Settings.LOGSTASH_JOB_INFO)
          || s.equals(Settings.HOPSWORKS_APPID_PROPERTY)
          || s.equals(Settings.SPARK_EXECUTORENV_PYTHONPATH)
          || s.equals(Settings.SPARK_EXECUTORENV_LD_LIBRARY_PATH)) {
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

    return builder.build(settings.getSparkDir(), JobType.SPARK, services);
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

}
