/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 */
package io.hops.hopsworks.common.jobs.spark;

import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.jobs.AsynchronousJobExecutor;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.jobs.yarn.LocalResourceDTO;
import io.hops.hopsworks.common.jobs.yarn.ServiceProperties;
import io.hops.hopsworks.common.jobs.yarn.YarnRunner;
import io.hops.hopsworks.common.util.HopsUtils;
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

import io.hops.hopsworks.common.util.templates.ConfigProperty;
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
  private int numberOfGpus = 0;
  private String properties;
  private boolean dynamicExecutors;
  private String executorMemory = "512m";
  private int driverMemory = 1024; // in MB
  private int driverCores = 1;
  private String driverQueue;
  private int numOfGPUs = 0;
//  private int numOfPs = 0;
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

    Map<String, ConfigProperty> jobHopsworksProps = new HashMap<>();
    JobType jobType = job.getJobConfig().getType();
    String appPath = ((SparkJobConfiguration) job.getJobConfig()).getAppPath();

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

    /**
     * * 1. Set stagingPath **
     */
    String stagingPath = "/Projects/" + project + "/" + Settings.PROJECT_STAGING_DIR + "/.sparkjobstaging-"
        + YarnRunner.APPID_PLACEHOLDER;
    builder.localResourcesBasePath(stagingPath);
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * * 2. Set job local resources, i.e. project certificates, job jar etc. **
     */
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
    } else if (jobType == JobType.PYSPARK) {
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
      if (jobType == JobType.PYSPARK) {
//        if (System.getenv().containsKey("LD_LIBRARY_PATH")) {
        jobHopsworksProps.put(Settings.SPARK_EXECUTORENV_LD_LIBRARY_PATH,
            new ConfigProperty(
                Settings.SPARK_EXECUTORENV_LD_LIBRARY_PATH,
                HopsUtils.APPEND_PATH,
                System.getenv("LD_LIBRARY_PATH")));
//        } else {
        jobHopsworksProps.put(Settings.SPARK_EXECUTORENV_LD_LIBRARY_PATH,
            new ConfigProperty(
                Settings.SPARK_EXECUTORENV_LD_LIBRARY_PATH,
                HopsUtils.APPEND_PATH,
                "$JAVA_HOME/jre/lib/amd64/server"));
//        }
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

      jobHopsworksProps.put(Settings.SPARK_APP_NAME_ENV,
          new ConfigProperty(
              Settings.SPARK_APP_NAME_ENV,
              HopsUtils.IGNORE,
              jobName));
      jobHopsworksProps.put(Settings.SPARK_YARN_IS_PYTHON_ENV,
          new ConfigProperty(
              Settings.SPARK_YARN_IS_PYTHON_ENV,
              HopsUtils.IGNORE,
              "true"));

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
        if (jobType == JobType.PYSPARK) {
          //For PySpark jobs prefix the resource name with __pyfiles__ as spark requires that.
          /*
           * https://github.com/hopshadoop/spark/blob/v2.3.0-hops/resource-managers/yarn/src/main/scala/org/apache
           * /spark/deploy/yarn/Client.scala#L803
           */
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
    jobHopsworksProps.put(Settings.SPARK_EXECUTORENV_HADOOP_USER_NAME,
        new ConfigProperty(
            Settings.SPARK_EXECUTORENV_HADOOP_USER_NAME,
            HopsUtils.IGNORE,
            jobUser));
    jobHopsworksProps.put(Settings.SPARK_EXECUTORENV_HDFS_USER,
        new ConfigProperty(
            Settings.SPARK_EXECUTORENV_HDFS_USER,
            HopsUtils.IGNORE,
            jobUser));

    //Set TensorFlowOnSpark required environment variables
    if (jobType == JobType.PYSPARK) {
      String libCuda = settings.getCudaDir() + "/lib64";
      String libJVM = settings.getJavaHome() + "/jre/lib/amd64/server";
      String libHDFS = settings.getHadoopSymbolicLinkDir() + "/lib/native";
      builder.addToAppMasterEnvironment("LD_LIBRARY_PATH", libCuda);
      jobHopsworksProps.put(Settings.SPARK_EXECUTORENV_LD_LIBRARY_PATH,
          new ConfigProperty(
              Settings.SPARK_EXECUTORENV_LD_LIBRARY_PATH,
              HopsUtils.APPEND_PATH,
              libCuda + ":" + libJVM + ":" + libHDFS));
    }
//    if (jobType == JobType.TFSPARK) {
//      //Should always be false in case of TFoS
//      dynamicExecutors = false;
//      //No point in retrying since clusterspec is static
//      jobHopsworksProps.put(Settings.SPARK_MAX_APP_ATTEMPTS,
//          new ConfigProperty(
//              Settings.SPARK_MAX_APP_ATTEMPTS,
//              HopsUtils.IGNORE, "1"));
//      //This is needed to solve a bug where the driver is able to allocate GPUs
//      builder.addToAppMasterEnvironment("CUDA_VISIBLE_DEVICES", "''");
//      //The following configuration is based on:
//      //https://github.com/yahoo/TensorFlowOnSpark/wiki/GetStarted_YARN
//      //IMPORTANT, if TFoS still can't find cuda libraries there may be issues with cuda installation
//      // 1. ssh to machine where the container failed
//      // 2. Make sure /usr/local/cuda exists and is a symlink pointing to e.g. /usr/local/cuda-8.0
//      // 3. Make sure /etc/ld.so.conf.d directory on the host has an entry pointing to /usr/local/cuda/lib64
//      // 4. /usr/local/cuda/lib64 can be a symlink and should point to the real location with libcu(...).so files
//      // 5. Run 'sudo ldconfig'
//      String libCuda = settings.getCudaDir() + "/lib64";
//      String libJVM = settings.getJavaHome() + "/jre/lib/amd64/server";
//      String libHDFS = settings.getHadoopSymbolicLinkDir() + "/lib/native";
//
//      builder.addToAppMasterEnvironment("LD_LIBRARY_PATH", libCuda);
//  
//      jobHopsworksProps.put(Settings.SPARK_EXECUTORENV_LD_LIBRARY_PATH,
//          new ConfigProperty(
//              Settings.SPARK_EXECUTORENV_LD_LIBRARY_PATH,
//              HopsUtils.APPEND_PATH,
//              libCuda + ":" + libJVM + ":" + libHDFS));
//    }

    for (String key : envVars.keySet()) {
      builder.addToAppMasterEnvironment(key, envVars.get(key));
    }

    jobHopsworksProps.put(Settings.SPARK_EXECUTOR_EXTRACLASSPATH,
        new ConfigProperty(
            Settings.SPARK_EXECUTOR_EXTRACLASSPATH,
            HopsUtils.APPEND_PATH,
            extraClassPathFiles.toString().substring(0, extraClassPathFiles.length() - 1)));
    jobHopsworksProps.put(Settings.SPARK_DRIVER_EXTRACLASSPATH,
        new ConfigProperty(
            Settings.SPARK_DRIVER_EXTRACLASSPATH,
            HopsUtils.APPEND_PATH,
            settings.getHopsLeaderElectionJarPath()));

    if (secondaryJars.length() > 0) {
      jobHopsworksProps.put(Settings.SPARK_YARN_SECONDARY_JARS,
          new ConfigProperty(
              Settings.SPARK_YARN_SECONDARY_JARS,
              HopsUtils.APPEND_PATH,
              secondaryJars.toString().substring(0, secondaryJars.length() - 1)));
    }

    //If DynamicExecutors are not enabled, set the user defined number 
    //of executors
    if (dynamicExecutors) {
      jobHopsworksProps.put(Settings.SPARK_DYNAMIC_ALLOC_ENV,
          new ConfigProperty(
              Settings.SPARK_DYNAMIC_ALLOC_ENV,
              HopsUtils.IGNORE,
              String.valueOf(dynamicExecutors)));
      jobHopsworksProps.put(Settings.SPARK_DYNAMIC_ALLOC_MIN_EXECS_ENV,
          new ConfigProperty(
              Settings.SPARK_DYNAMIC_ALLOC_MIN_EXECS_ENV,
              HopsUtils.IGNORE,
              String.valueOf(numberOfExecutorsMin)));

      jobHopsworksProps.put(Settings.SPARK_DYNAMIC_ALLOC_MAX_EXECS_ENV,
          new ConfigProperty(
              Settings.SPARK_DYNAMIC_ALLOC_MAX_EXECS_ENV,
              HopsUtils.IGNORE,
              String.valueOf(numberOfExecutorsMax)));

      jobHopsworksProps.put(Settings.SPARK_DYNAMIC_ALLOC_INIT_EXECS_ENV,
          new ConfigProperty(
              Settings.SPARK_DYNAMIC_ALLOC_INIT_EXECS_ENV,
              HopsUtils.IGNORE,
              String.valueOf(numberOfExecutorsInit)));

      //Dynamic executors requires the shuffle service to be enabled
      jobHopsworksProps.put(Settings.SPARK_SHUFFLE_SERVICE,
          new ConfigProperty(
              Settings.SPARK_SHUFFLE_SERVICE,
              HopsUtils.IGNORE,
              "true"));
      //spark.shuffle.service.enabled
    } else {
      jobHopsworksProps.put(Settings.SPARK_NUMBER_EXECUTORS_ENV,
          new ConfigProperty(
              Settings.SPARK_NUMBER_EXECUTORS_ENV,
              HopsUtils.IGNORE,
              Integer.toString(numberOfExecutors)));
    }

    List<String> jobSpecificProperties = new ArrayList<>();
    jobSpecificProperties.add(Settings.SPARK_NUMBER_EXECUTORS_ENV);
    jobSpecificProperties.add(Settings.SPARK_DRIVER_MEMORY_ENV);
    jobSpecificProperties.add(Settings.SPARK_DRIVER_CORES_ENV);
    jobSpecificProperties.add(Settings.SPARK_EXECUTOR_MEMORY_ENV);
    jobSpecificProperties.add(Settings.SPARK_EXECUTOR_CORES_ENV);

    //These properties are set so that spark history server picks them up
    jobHopsworksProps.put(Settings.SPARK_DRIVER_MEMORY_ENV,
        new ConfigProperty(
            Settings.SPARK_DRIVER_MEMORY_ENV,
            HopsUtils.IGNORE,
            Integer.toString(driverMemory) + "m"));
    jobHopsworksProps.put(Settings.SPARK_DRIVER_CORES_ENV,
        new ConfigProperty(
            Settings.SPARK_DRIVER_CORES_ENV,
            HopsUtils.IGNORE,
            Integer.toString(driverCores)));
    jobHopsworksProps.put(Settings.SPARK_EXECUTOR_MEMORY_ENV,
        new ConfigProperty(
            Settings.SPARK_EXECUTOR_MEMORY_ENV,
            HopsUtils.IGNORE,
            executorMemory));
    jobHopsworksProps.put(Settings.SPARK_EXECUTOR_CORES_ENV,
        new ConfigProperty(
            Settings.SPARK_EXECUTOR_CORES_ENV,
            HopsUtils.IGNORE,
            Integer.toString(executorCores)));
    jobHopsworksProps.put(Settings.SPARK_DRIVER_STAGINGDIR_ENV,
        new ConfigProperty(
            Settings.SPARK_DRIVER_STAGINGDIR_ENV,
            HopsUtils.IGNORE,
            stagingPath));
    //Add log4j property
    jobHopsworksProps.put(Settings.SPARK_LOG4J_CONFIG,
        new ConfigProperty(
            Settings.SPARK_LOG4J_CONFIG,
            HopsUtils.OVERWRITE,
            Settings.SPARK_LOG4J_PROPERTIES));
    //Comma-separated list of attributes sent to Logstash
    jobHopsworksProps.put(Settings.LOGSTASH_JOB_INFO,
        new ConfigProperty(
            Settings.LOGSTASH_JOB_INFO,
            HopsUtils.IGNORE,
            project.toLowerCase() + "," + jobName + "," + job.getId() + "," + YarnRunner.APPID_PLACEHOLDER));
    jobHopsworksProps.put(Settings.HOPSWORKS_APPID_PROPERTY,
        new ConfigProperty(
            Settings.HOPSWORKS_APPID_PROPERTY,
            HopsUtils.IGNORE,
            YarnRunner.APPID_PLACEHOLDER));
    jobHopsworksProps.put(Settings.SPARK_JAVA_LIBRARY_PROP,
        new ConfigProperty(
            Settings.SPARK_JAVA_LIBRARY_PROP,
            HopsUtils.IGNORE,
            services.getSettings().getHadoopSymbolicLinkDir() + "/lib/native/"));

    //Set executor extraJavaOptions to make parameters available to executors
    Map<String, String> extraJavaOptions = new HashMap<>();
    extraJavaOptions.put(Settings.SPARK_LOG4J_CONFIG, Settings.SPARK_LOG4J_PROPERTIES);
    extraJavaOptions.put(Settings.LOGSTASH_JOB_INFO, project.toLowerCase() + "," + jobName + "," + job.getId() + ","
        + YarnRunner.APPID_PLACEHOLDER);
    extraJavaOptions.put(Settings.SPARK_JAVA_LIBRARY_PROP, services.getSettings().getHadoopSymbolicLinkDir()
        + "/lib/native/");
    extraJavaOptions.put(Settings.HOPSWORKS_APPID_PROPERTY, YarnRunner.APPID_PLACEHOLDER);

    if (serviceProps != null) {
      jobHopsworksProps.put(Settings.HOPSWORKS_REST_ENDPOINT_PROPERTY,
          new ConfigProperty(
              Settings.HOPSWORKS_REST_ENDPOINT_PROPERTY,
              HopsUtils.IGNORE,
              serviceProps.getRestEndpoint()));
      jobHopsworksProps.put(Settings.SERVER_TRUSTSTORE_PROPERTY,
          new ConfigProperty(
              Settings.SERVER_TRUSTSTORE_PROPERTY,
              HopsUtils.IGNORE,
              Settings.SERVER_TRUSTSTORE_PROPERTY));
      jobHopsworksProps.put(Settings.HOPSWORKS_ELASTIC_ENDPOINT_PROPERTY,
          new ConfigProperty(
              Settings.HOPSWORKS_ELASTIC_ENDPOINT_PROPERTY,
              HopsUtils.IGNORE,
              serviceProps.getElastic().getRestEndpoint()));
      jobHopsworksProps.put(Settings.HOPSWORKS_JOBNAME_PROPERTY,
          new ConfigProperty(
              Settings.HOPSWORKS_JOBNAME_PROPERTY,
              HopsUtils.IGNORE,
              serviceProps.getJobName()));
      jobHopsworksProps.put(Settings.HOPSWORKS_JOBTYPE_PROPERTY,
          new ConfigProperty(
              Settings.HOPSWORKS_JOBTYPE_PROPERTY,
              HopsUtils.IGNORE,
              jobType.getName()));
      jobHopsworksProps.put(Settings.HOPSWORKS_PROJECTUSER_PROPERTY,
          new ConfigProperty(
              Settings.HOPSWORKS_PROJECTUSER_PROPERTY,
              HopsUtils.IGNORE,
              jobUser));
      jobHopsworksProps.put(Settings.HOPSWORKS_PROJECTID_PROPERTY,
          new ConfigProperty(
              Settings.HOPSWORKS_PROJECTID_PROPERTY,
              HopsUtils.IGNORE,
              Integer.toString(serviceProps.getProjectId())));
      jobHopsworksProps.put(Settings.HOPSWORKS_PROJECTNAME_PROPERTY,
          new ConfigProperty(
              Settings.HOPSWORKS_PROJECTNAME_PROPERTY,
              HopsUtils.IGNORE,
              serviceProps.getProjectName()));

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
        jobHopsworksProps.put(Settings.KAFKA_BROKERADDR_PROPERTY,
            new ConfigProperty(
                Settings.KAFKA_BROKERADDR_PROPERTY,
                HopsUtils.IGNORE,
                serviceProps.getKafka().getBrokerAddresses()));
        jobHopsworksProps.put(Settings.KAFKA_JOB_TOPICS_PROPERTY,
            new ConfigProperty(
                Settings.KAFKA_JOB_TOPICS_PROPERTY,
                HopsUtils.IGNORE,
                serviceProps.getKafka().getTopics()));
        jobHopsworksProps.put(Settings.KAFKA_CONSUMER_GROUPS,
            new ConfigProperty(
                Settings.KAFKA_CONSUMER_GROUPS,
                HopsUtils.IGNORE,
                serviceProps.getKafka().getConsumerGroups()));
        extraJavaOptions.put(Settings.KAFKA_BROKERADDR_PROPERTY, serviceProps.getKafka().getBrokerAddresses());
        extraJavaOptions.put(Settings.KAFKA_JOB_TOPICS_PROPERTY, serviceProps.getKafka().getTopics());
        extraJavaOptions.put(Settings.KAFKA_CONSUMER_GROUPS, serviceProps.getKafka().getConsumerGroups());

      }
    }

    //Set up command
    StringBuilder amargs = new StringBuilder("--class ");
    amargs.append(((SparkJobConfiguration) job.getJobConfig()).
        getMainClass());

    if (jobType == JobType.PYSPARK) {
      amargs.append(" --primary-py-file ").append(appExecName);
      //Check if anaconda is enabled
//      builder.addToAppMasterEnvironment(Settings.SPARK_PYSPARK_PYTHON, "python");
//        jobHopsworksProps.put(Settings.SPARK_TF_ENV,
//            new ConfigProperty(
//                Settings.SPARK_TF_ENV,
//                HopsUtils.IGNORE,
//                "true"));
      jobHopsworksProps.put(Settings.SPARK_TF_GPUS_ENV,
          new ConfigProperty(
              Settings.SPARK_TF_GPUS_ENV,
              HopsUtils.IGNORE,
              Integer.toString(numOfGPUs)));
      //Add libs to PYTHONPATH
      builder.addToAppMasterEnvironment(Settings.SPARK_PYSPARK_PYTHON, serviceProps.getAnaconda().getEnvPath());

      builder.addToAppMasterEnvironment(Settings.SPARK_PYTHONPATH, pythonPath.toString());
      jobHopsworksProps.put(Settings.SPARK_EXECUTORENV_PYTHONPATH,
          new ConfigProperty(
              Settings.SPARK_EXECUTORENV_PYTHONPATH,
              HopsUtils.IGNORE,
              pythonPathExecs.toString()));
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
          jobHopsworksProps.put(property,
              new ConfigProperty(
                  property,
                  HopsUtils.OVERWRITE,
                  sparkProperties.getProperty(property).trim()));
        }
      }
    }

    //Create a string with system properties from extraJavaOptions
    StringBuilder extraJavaOptionsSb = new StringBuilder();
    //extraJavaOptionsSb.append("'-D"+Settings.SPARK_EXECUTOR_EXTRA_JAVA_OPTS+"=");
    for (String key : extraJavaOptions.keySet()) {
      extraJavaOptionsSb.append(" -D").append(key).append("=").append(extraJavaOptions.get(key)).append(" ");
    }

    jobHopsworksProps.put(Settings.SPARK_EXECUTOR_EXTRA_JAVA_OPTS,
        new ConfigProperty(
            Settings.SPARK_EXECUTOR_EXTRA_JAVA_OPTS,
            HopsUtils.APPEND_SPACE,
            extraJavaOptionsSb.toString().trim()));

    Map<String, String> userSparkProperties = null;
    try {
      userSparkProperties = HopsUtils.validateUserProperties(properties, settings.getSparkDir());
    } catch (Exception ex) {
      LOG.log(Level.WARNING,
          "There was an error while setting user-provided Spark properties:{0}", ex.getMessage());
      throw new IOException(
          "There was an error while setting user-provided Spark properties. Please check that the values conform to"
          + " the requested input format or that the property is allowed:" + ex.getMessage());
    }

    Map<String, String> finalJobProps = HopsUtils.mergeHopsworksAndUserParams(jobHopsworksProps, userSparkProperties,
        true);
    finalJobProps.remove("spark_user_defined_properties");
    for (String key : finalJobProps.keySet()) {
      addSystemProperty(key, finalJobProps.get(key));
    }

    for (String s : sysProps.keySet()) {
      String option = YarnRunner.escapeForShell("-D" + s + "=" + sysProps.get(s));
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
          "Maximum number of  executors cannot be greater than:"
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

//  public void setNumOfPs(int numOfPs) {
//    this.numOfPs = numOfPs;
//  }
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
