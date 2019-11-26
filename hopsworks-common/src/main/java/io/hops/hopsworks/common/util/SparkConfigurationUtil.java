/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
 */

package io.hops.hopsworks.common.util;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.jobs.configuration.JobConfiguration;
import io.hops.hopsworks.common.jobs.configuration.JobType;
import io.hops.hopsworks.common.jobs.spark.DistributionStrategy;
import io.hops.hopsworks.common.jobs.spark.ExperimentType;
import io.hops.hopsworks.common.jobs.spark.SparkJobConfiguration;
import io.hops.hopsworks.common.util.templates.ConfigProperty;
import io.hops.hopsworks.common.util.templates.ConfigReplacementPolicy;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SparkConfigurationUtil extends ConfigurationUtil {

  public Map<String, String> getFrameworkProperties(Project project, JobConfiguration jobConfiguration,
                                                            Settings settings, String hdfsUser, String usersFullName,
                                                            String tfLdLibraryPath, Map<String,
                                                            String> extraJavaOptions) throws IOException {
    SparkJobConfiguration sparkJobConfiguration = (SparkJobConfiguration)jobConfiguration;
    ExperimentType experimentType = sparkJobConfiguration.getExperimentType();
    DistributionStrategy distributionStrategy = sparkJobConfiguration.getDistributionStrategy();

    Map<String, ConfigProperty> sparkProps = new HashMap<>();

    if(jobConfiguration.getAppName() != null) {
      sparkProps.put(Settings.SPARK_APP_NAME_ENV,
        new ConfigProperty(
          Settings.SPARK_APP_NAME_ENV,
          HopsUtils.OVERWRITE,
          sparkJobConfiguration.getAppName()));
    }

    if(sparkJobConfiguration.getJobType() != null && sparkJobConfiguration.getJobType() == JobType.PYSPARK) {
      sparkProps.put(Settings.SPARK_YARN_IS_PYTHON_ENV,
        new ConfigProperty(
          Settings.SPARK_YARN_IS_PYTHON_ENV,
          HopsUtils.OVERWRITE,
          "true"));
    }

    addToSparkEnvironment(sparkProps, "PATH", "{{PWD}}" + File.pathSeparator +
        settings.getAnacondaProjectDir(project) + "/bin:" + settings.getHadoopSymbolicLinkDir() + "/bin" +
        ":/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin", HopsUtils.APPEND_PATH);

    sparkProps.put(Settings.SPARK_PYSPARK_PYTHON_OPTION, new ConfigProperty(
      Settings.SPARK_PYSPARK_PYTHON_OPTION, HopsUtils.IGNORE,
      settings.getAnacondaProjectDir(project) + "/bin/python"));


    //https://docs.nvidia.com/cuda/cuda-c-programming-guide/index.html
    //Needs to be set for CUDA libraries to not initialize GPU context
    sparkProps.put(Settings.SPARK_YARN_APPMASTER_ENV + "CUDA_VISIBLE_DEVICES",
            new ConfigProperty(Settings.SPARK_YARN_APPMASTER_ENV + "CUDA_VISIBLE_DEVICES",
                    HopsUtils.IGNORE, ""));

    //https://rocm-documentation.readthedocs.io/en/latest/Other_Solutions/Other-Solutions.html
    //Needs to be set for ROCm libraries to not initialize GPU context
    sparkProps.put(Settings.SPARK_YARN_APPMASTER_ENV + "HIP_VISIBLE_DEVICES",
            new ConfigProperty(Settings.SPARK_YARN_APPMASTER_ENV + "HIP_VISIBLE_DEVICES",
                    HopsUtils.IGNORE, "-1"));

    sparkProps.put(Settings.SPARK_YARN_APPMASTER_ENV + "EXECUTOR_GPUS",
        new ConfigProperty(Settings.SPARK_YARN_APPMASTER_ENV + "EXECUTOR_GPUS",
            HopsUtils.IGNORE, "0"));

    sparkProps.put(Settings.SPARK_EXECUTOR_ENV + "EXECUTOR_GPUS",
        new ConfigProperty(Settings.SPARK_EXECUTOR_ENV + "EXECUTOR_GPUS",
            HopsUtils.IGNORE, Integer.toString(sparkJobConfiguration.getExecutorGpus())));

    sparkProps.put(Settings.SPARK_SUBMIT_DEPLOYMODE, new ConfigProperty(Settings.SPARK_SUBMIT_DEPLOYMODE,
      HopsUtils.OVERWRITE,"cluster"));

    if(experimentType != null) {
      if(sparkJobConfiguration.getExecutorGpus() == 0) {
        addToSparkEnvironment(sparkProps, "HIP_VISIBLE_DEVICES", "-1", HopsUtils.IGNORE);
        addToSparkEnvironment(sparkProps, "CUDA_VISIBLE_DEVICES", "", HopsUtils.IGNORE);
      }
      if (sparkJobConfiguration.getExecutorGpus() > 0) {
        sparkProps.put(Settings.SPARK_TF_GPUS_ENV,
          new ConfigProperty(
            Settings.SPARK_TF_GPUS_ENV,
            HopsUtils.OVERWRITE,
            Integer.toString(sparkJobConfiguration.getExecutorGpus())));
        sparkProps.put(Settings.SPARK_TENSORFLOW_APPLICATION,
          new ConfigProperty(
            Settings.SPARK_TENSORFLOW_APPLICATION,
            HopsUtils.OVERWRITE,
            "true"));
      } else {
        sparkProps.put(Settings.SPARK_TENSORFLOW_APPLICATION,
          new ConfigProperty(
            Settings.SPARK_TENSORFLOW_APPLICATION,
            HopsUtils.OVERWRITE,
            "false"));
      }
    }

    addToSparkEnvironment(sparkProps, "SPARK_HOME", settings.getSparkDir(), HopsUtils.IGNORE);
    addToSparkEnvironment(sparkProps, "SPARK_CONF_DIR", settings.getSparkConfDir(), HopsUtils.IGNORE);
    addToSparkEnvironment(sparkProps, "ELASTIC_ENDPOINT", settings.getElasticRESTEndpoint(), HopsUtils.IGNORE);
    addToSparkEnvironment(sparkProps, "HADOOP_VERSION", settings.getHadoopVersion(), HopsUtils.IGNORE);
    addToSparkEnvironment(sparkProps, "HOPSWORKS_VERSION", settings.getHopsworksVersion(), HopsUtils.IGNORE);
    addToSparkEnvironment(sparkProps, "CUDA_VERSION", settings.getCudaVersion(), HopsUtils.IGNORE);
    addToSparkEnvironment(sparkProps, "TENSORFLOW_VERSION", settings.getTensorflowVersion(), HopsUtils.IGNORE);
    addToSparkEnvironment(sparkProps, "KAFKA_VERSION", settings.getKafkaVersion(),HopsUtils.IGNORE);
    addToSparkEnvironment(sparkProps, "SPARK_VERSION", settings.getSparkVersion(), HopsUtils.IGNORE);
    addToSparkEnvironment(sparkProps, "LIVY_VERSION", settings.getLivyVersion(), HopsUtils.IGNORE);
    addToSparkEnvironment(sparkProps, "HADOOP_HOME", settings.getHadoopSymbolicLinkDir(), HopsUtils.IGNORE);
    addToSparkEnvironment(sparkProps, "HADOOP_HDFS_HOME", settings.getHadoopSymbolicLinkDir(),
            HopsUtils.IGNORE);
    addToSparkEnvironment(sparkProps, "HADOOP_USER_NAME", hdfsUser, HopsUtils.IGNORE);
    addToSparkEnvironment(sparkProps, "LD_LIBRARY_PATH", settings.getJavaHome() +
      "/jre/lib/amd64/server" + File.pathSeparator + tfLdLibraryPath +
      settings.getHadoopSymbolicLinkDir() + "/lib/native" + File.pathSeparator + settings.getAnacondaProjectDir(project)
        + "/lib/", HopsUtils.APPEND_PATH);
    if(!Strings.isNullOrEmpty(sparkJobConfiguration.getAppName())) {
      addToSparkEnvironment(sparkProps, "HOPSWORKS_JOB_NAME", sparkJobConfiguration.getAppName(),
              HopsUtils.IGNORE);
    }
    if(!Strings.isNullOrEmpty(settings.getKafkaBrokersStr())) {
      addToSparkEnvironment(sparkProps, "KAFKA_BROKERS", settings.getKafkaBrokersStr(), HopsUtils.IGNORE);
    }
    addToSparkEnvironment(sparkProps, "LIBHDFS_OPTS", "-Xmx96m -Dlog4j.configuration=" +
            settings.getHadoopSymbolicLinkDir() +"/etc/hadoop/log4j.properties -Dhadoop.root.logger=ERROR,RFA",
      HopsUtils.APPEND_SPACE);
    addToSparkEnvironment(sparkProps, "REST_ENDPOINT", settings.getRestEndpoint(), HopsUtils.IGNORE);
    addToSparkEnvironment(sparkProps,"HOPSWORKS_USER", usersFullName, HopsUtils.IGNORE);
    addToSparkEnvironment(sparkProps,
      Settings.SPARK_PYSPARK_PYTHON, settings.getAnacondaProjectDir(project) + "/bin/python",
            HopsUtils.IGNORE);
    addToSparkEnvironment(sparkProps, "HOPSWORKS_PROJECT_ID", Integer.toString(project.getId()),
            HopsUtils.IGNORE);
    addToSparkEnvironment(sparkProps, "FLINK_CONF_DIR", settings.getFlinkConfDir(),
      HopsUtils.IGNORE);
    addToSparkEnvironment(sparkProps, "REQUESTS_VERIFY", String.valueOf(settings.getRequestsVerify()),
      HopsUtils.IGNORE);
    addToSparkEnvironment(sparkProps, "DOMAIN_CA_TRUSTSTORE_PEM",
      settings.getSparkConfDir() + File.separator + Settings.DOMAIN_CA_TRUSTSTORE_PEM, HopsUtils.IGNORE);
  
    //If DynamicExecutors are not enabled, set the user defined number
    //of executors

    //Force dynamic allocation if we are running a DL experiment (we never want users to lock up GPUs)
    sparkProps.put(Settings.SPARK_DYNAMIC_ALLOC_ENV,
                  new ConfigProperty(
                          Settings.SPARK_DYNAMIC_ALLOC_ENV,
                          HopsUtils.OVERWRITE,
                          String.valueOf(sparkJobConfiguration.isDynamicAllocationEnabled() ||
                            experimentType != null)));

    if(experimentType != null) {
      //Dynamic executors requires the shuffle service to be enabled
      sparkProps.put(Settings.SPARK_SHUFFLE_SERVICE,
        new ConfigProperty(
          Settings.SPARK_SHUFFLE_SERVICE,
          HopsUtils.OVERWRITE,
          "true"));
      //To avoid deadlock in resource allocation this configuration is needed
      if(experimentType == ExperimentType.DISTRIBUTED_TRAINING) {
        if(distributionStrategy == DistributionStrategy.COLLECTIVE_ALL_REDUCE ||
          distributionStrategy == DistributionStrategy.MIRRORED) {
          sparkProps.put(Settings.SPARK_DYNAMIC_ALLOC_MIN_EXECS_ENV,
            new ConfigProperty(
              Settings.SPARK_DYNAMIC_ALLOC_MIN_EXECS_ENV,
              HopsUtils.OVERWRITE,
              "0"));
          sparkProps.put(Settings.SPARK_DYNAMIC_ALLOC_MAX_EXECS_ENV,
            new ConfigProperty(
              Settings.SPARK_DYNAMIC_ALLOC_MAX_EXECS_ENV,
              HopsUtils.OVERWRITE,
              String.valueOf(sparkJobConfiguration.getDynamicAllocationMaxExecutors())));
          sparkProps.put(Settings.SPARK_DYNAMIC_ALLOC_INIT_EXECS_ENV,
            new ConfigProperty(
              Settings.SPARK_DYNAMIC_ALLOC_INIT_EXECS_ENV,
              HopsUtils.OVERWRITE,
              String.valueOf(sparkJobConfiguration.getDynamicAllocationMaxExecutors())));
        } else if(distributionStrategy == DistributionStrategy.PARAMETER_SERVER) {
          sparkProps.put(Settings.SPARK_TENSORFLOW_NUM_PS,
            new ConfigProperty(
              Settings.SPARK_TENSORFLOW_NUM_PS,
              HopsUtils.OVERWRITE,
              Integer.toString(sparkJobConfiguration.getNumPs())));
          sparkProps.put(Settings.SPARK_DYNAMIC_ALLOC_MIN_EXECS_ENV,
            new ConfigProperty(
              Settings.SPARK_DYNAMIC_ALLOC_MIN_EXECS_ENV,
              HopsUtils.OVERWRITE,
              "0"));
          sparkProps.put(Settings.SPARK_DYNAMIC_ALLOC_MAX_EXECS_ENV,
            new ConfigProperty(
              Settings.SPARK_DYNAMIC_ALLOC_MAX_EXECS_ENV,
              HopsUtils.OVERWRITE,
              String.valueOf(sparkJobConfiguration.getDynamicAllocationMaxExecutors() +
                sparkJobConfiguration.getNumPs())));
          sparkProps.put(Settings.SPARK_DYNAMIC_ALLOC_INIT_EXECS_ENV,
            new ConfigProperty(
              Settings.SPARK_DYNAMIC_ALLOC_INIT_EXECS_ENV,
              HopsUtils.OVERWRITE,
              String.valueOf(sparkJobConfiguration.getDynamicAllocationMaxExecutors() +
                sparkJobConfiguration.getNumPs())));
        }
      } else if(experimentType == ExperimentType.PARALLEL_EXPERIMENTS) {
        sparkProps.put(Settings.SPARK_DYNAMIC_ALLOC_MIN_EXECS_ENV,
          new ConfigProperty(
            Settings.SPARK_DYNAMIC_ALLOC_MIN_EXECS_ENV,
            HopsUtils.OVERWRITE,
            "0"));
        sparkProps.put(Settings.SPARK_DYNAMIC_ALLOC_MAX_EXECS_ENV,
          new ConfigProperty(
            Settings.SPARK_DYNAMIC_ALLOC_MAX_EXECS_ENV,
            HopsUtils.OVERWRITE,
            String.valueOf(sparkJobConfiguration.getDynamicAllocationMaxExecutors())));
        sparkProps.put(Settings.SPARK_DYNAMIC_ALLOC_INIT_EXECS_ENV,
          new ConfigProperty(
            Settings.SPARK_DYNAMIC_ALLOC_INIT_EXECS_ENV,
            HopsUtils.OVERWRITE,
            "0"));
      } else { //EXPERIMENT
        sparkProps.put(Settings.SPARK_DYNAMIC_ALLOC_MIN_EXECS_ENV,
          new ConfigProperty(
            Settings.SPARK_DYNAMIC_ALLOC_MIN_EXECS_ENV,
            HopsUtils.OVERWRITE,
            "0"));
        sparkProps.put(Settings.SPARK_DYNAMIC_ALLOC_MAX_EXECS_ENV,
          new ConfigProperty(
            Settings.SPARK_DYNAMIC_ALLOC_MAX_EXECS_ENV,
            HopsUtils.OVERWRITE,
            "1"));
        sparkProps.put(Settings.SPARK_DYNAMIC_ALLOC_INIT_EXECS_ENV,
          new ConfigProperty(
            Settings.SPARK_DYNAMIC_ALLOC_INIT_EXECS_ENV,
            HopsUtils.OVERWRITE,
            "0"));
      }
    } else if(sparkJobConfiguration.isDynamicAllocationEnabled()) {
      //Spark dynamic
      sparkProps.put(Settings.SPARK_SHUFFLE_SERVICE,
        new ConfigProperty(
          Settings.SPARK_SHUFFLE_SERVICE,
          HopsUtils.OVERWRITE,
          "true"));

      // To avoid users creating erroneous configurations for the initialExecutors field
      // Initial executors should not be greater than MaxExecutors
      if(sparkJobConfiguration.getDynamicAllocationInitialExecutors() >
              sparkJobConfiguration.getDynamicAllocationMaxExecutors()) {
        sparkProps.put(Settings.SPARK_DYNAMIC_ALLOC_INIT_EXECS_ENV,
          new ConfigProperty(
          Settings.SPARK_DYNAMIC_ALLOC_INIT_EXECS_ENV,
          HopsUtils.OVERWRITE,
          String.valueOf(sparkJobConfiguration.getDynamicAllocationMaxExecutors())));
      // Initial executors should not be less than MinExecutors
      } else if(sparkJobConfiguration.getDynamicAllocationInitialExecutors() <
              sparkJobConfiguration.getDynamicAllocationMinExecutors()) {
        sparkProps.put(Settings.SPARK_DYNAMIC_ALLOC_INIT_EXECS_ENV,
          new ConfigProperty(
          Settings.SPARK_DYNAMIC_ALLOC_INIT_EXECS_ENV,
          HopsUtils.OVERWRITE,
          String.valueOf(sparkJobConfiguration.getDynamicAllocationMinExecutors())));
      } else {
      // User set it to a valid value
        sparkProps.put(Settings.SPARK_DYNAMIC_ALLOC_INIT_EXECS_ENV,
          new ConfigProperty(
          Settings.SPARK_DYNAMIC_ALLOC_INIT_EXECS_ENV,
          HopsUtils.OVERWRITE,
          String.valueOf(sparkJobConfiguration.getDynamicAllocationInitialExecutors())));
      }
      sparkProps.put(Settings.SPARK_DYNAMIC_ALLOC_MIN_EXECS_ENV,
        new ConfigProperty(
          Settings.SPARK_DYNAMIC_ALLOC_MIN_EXECS_ENV,
          HopsUtils.OVERWRITE,
          String.valueOf(sparkJobConfiguration.getDynamicAllocationMinExecutors())));
      sparkProps.put(Settings.SPARK_DYNAMIC_ALLOC_MAX_EXECS_ENV,
        new ConfigProperty(
          Settings.SPARK_DYNAMIC_ALLOC_MAX_EXECS_ENV,
          HopsUtils.OVERWRITE,
          String.valueOf(sparkJobConfiguration.getDynamicAllocationMaxExecutors())));
      sparkProps.put(Settings.SPARK_NUMBER_EXECUTORS_ENV,
        new ConfigProperty(
          Settings.SPARK_NUMBER_EXECUTORS_ENV,
          HopsUtils.OVERWRITE,
          Integer.toString(sparkJobConfiguration.getDynamicAllocationMinExecutors())));
    } else {
      //Spark Static
      sparkProps.put(Settings.SPARK_NUMBER_EXECUTORS_ENV,
          new ConfigProperty(
            Settings.SPARK_NUMBER_EXECUTORS_ENV,
            HopsUtils.OVERWRITE,
            Integer.toString(sparkJobConfiguration.getExecutorInstances())));
    }
    sparkProps.put(Settings.SPARK_DRIVER_MEMORY_ENV,
      new ConfigProperty(
        Settings.SPARK_DRIVER_MEMORY_ENV,
        HopsUtils.OVERWRITE,
        sparkJobConfiguration.getAmMemory() + "m"));
    sparkProps.put(Settings.SPARK_DRIVER_CORES_ENV,
      new ConfigProperty(
        Settings.SPARK_DRIVER_CORES_ENV,
        HopsUtils.OVERWRITE,
        Integer.toString(experimentType != null ? 1 : sparkJobConfiguration.getExecutorCores())));
    sparkProps.put(Settings.SPARK_EXECUTOR_MEMORY_ENV,
      new ConfigProperty(
        Settings.SPARK_EXECUTOR_MEMORY_ENV,
        HopsUtils.OVERWRITE,
        sparkJobConfiguration.getExecutorMemory() + "m"));
    sparkProps.put(Settings.SPARK_EXECUTOR_CORES_ENV,
      new ConfigProperty(
        Settings.SPARK_EXECUTOR_CORES_ENV,
        HopsUtils.OVERWRITE,
        Integer.toString(experimentType != null ? 1 : sparkJobConfiguration.getExecutorCores())));

    StringBuilder sparkFiles = new StringBuilder();
    sparkFiles
            .append(settings.getSparkMetricsPath())
            .append(",")
            //Log4j.properties
            .append(settings.getSparkLog4JPath())
            .append(",")
            // Add Hive-site.xml for SparkSQL
            .append(settings.getHiveSiteSparkHdfsPath());

    StringBuilder extraClassPath = new StringBuilder();
    extraClassPath
      .append("{{PWD}}")
      .append(File.pathSeparator)
      .append(settings.getHopsLeaderElectionJarPath())
      .append(File.pathSeparator)
      .append(settings.getSparkDir())
      .append("/jars/*")
      .append(File.pathSeparator)
      .append(settings.getSparkDir())
      .append("/hopsworks-jars/*");

    String applicationsJars = sparkJobConfiguration.getJars();
    if(!Strings.isNullOrEmpty(applicationsJars)) {
      applicationsJars = formatResources(applicationsJars);
      for(String jar: applicationsJars.split(",")) {
        String name = jar.substring(jar.lastIndexOf("/") + 1);
        extraClassPath.append(File.pathSeparator).append(name);
      }
      applicationsJars = formatResources(applicationsJars);
      sparkFiles.append(",").append(applicationsJars);
    }

    String applicationArchives = sparkJobConfiguration.getArchives();
    if(!Strings.isNullOrEmpty(applicationArchives)) {
      applicationArchives = formatResources(applicationArchives);
      sparkProps.put(Settings.SPARK_YARN_DIST_ARCHIVES, new ConfigProperty(
          Settings.SPARK_YARN_DIST_ARCHIVES, HopsUtils.APPEND_COMMA,
          applicationArchives));
    }

    // If Hops RPC TLS is enabled, password file would be injected by the
    // NodeManagers. We don't need to add it as LocalResource
    if (!settings.getHopsRpcTls()) {
      sparkFiles
              // Keystore
              .append(",hdfs://").append(settings.getHdfsTmpCertDir()).append(File.separator)
              .append(hdfsUser).append(File.separator).append(hdfsUser)
              .append("__kstore.jks#").append(Settings.K_CERTIFICATE)
              .append(",")
              // TrustStore
              .append("hdfs://").append(settings.getHdfsTmpCertDir()).append(File.separator)
              .append(hdfsUser).append(File.separator).append(hdfsUser)
              .append("__tstore.jks#").append(Settings.T_CERTIFICATE)
              .append(",")
              // File with crypto material password
              .append("hdfs://").append(settings.getHdfsTmpCertDir()).append(File.separator)
              .append(hdfsUser).append(File.separator).append(hdfsUser)
              .append("__cert.key#").append(Settings.CRYPTO_MATERIAL_PASSWORD);
    }

    String applicationFiles = sparkJobConfiguration.getFiles();
    if(!Strings.isNullOrEmpty(applicationFiles)) {
      applicationFiles = formatResources(applicationFiles);
      sparkFiles.append(",").append(applicationFiles);
    }

    String applicationPyFiles = sparkJobConfiguration.getPyFiles();
    if(!Strings.isNullOrEmpty(applicationPyFiles)) {
      StringBuilder pythonPath = new StringBuilder();
      applicationPyFiles = formatResources(applicationPyFiles);
      for(String pythonDep: applicationPyFiles.split(",")) {
        String name = pythonDep.substring(pythonDep.lastIndexOf("/") + 1);
        pythonPath.append("{{PWD}}/" + name + File.pathSeparator);
      }
      addToSparkEnvironment(sparkProps,"PYTHONPATH", pythonPath.toString(), HopsUtils.APPEND_PATH);
      sparkFiles.append(",").append(applicationPyFiles);
    }

    applicationFiles = formatResources(sparkFiles.toString());
    sparkProps.put(Settings.SPARK_YARN_DIST_FILES, new ConfigProperty(
            Settings.SPARK_YARN_DIST_FILES, HopsUtils.APPEND_COMMA,
            applicationFiles));

    sparkProps.put(Settings.SPARK_DRIVER_EXTRACLASSPATH, new ConfigProperty(
            Settings.SPARK_DRIVER_EXTRACLASSPATH, HopsUtils.APPEND_PATH, extraClassPath.toString()));

    sparkProps.put(Settings.SPARK_EXECUTOR_EXTRACLASSPATH, new ConfigProperty(
            Settings.SPARK_EXECUTOR_EXTRACLASSPATH, HopsUtils.APPEND_PATH, extraClassPath.toString()));

    //We do not support fault-tolerance for distributed training
    if(experimentType == ExperimentType.DISTRIBUTED_TRAINING)
    {
      sparkProps.put(Settings.SPARK_BLACKLIST_ENABLED, new ConfigProperty(
        Settings.SPARK_BLACKLIST_ENABLED, HopsUtils.OVERWRITE,
        "false"));
    } else if(sparkJobConfiguration.isBlacklistingEnabled()) {
      sparkProps.put(Settings.SPARK_BLACKLIST_ENABLED, new ConfigProperty(
        Settings.SPARK_BLACKLIST_ENABLED, HopsUtils.OVERWRITE,
        Boolean.toString(sparkJobConfiguration.isBlacklistingEnabled())));

      // If any task fails on an executor - kill it instantly (need fresh working directory for each task)
      sparkProps.put(Settings.SPARK_BLACKLIST_MAX_TASK_ATTEMPTS_PER_EXECUTOR, new ConfigProperty(
        Settings.SPARK_BLACKLIST_MAX_TASK_ATTEMPTS_PER_EXECUTOR, HopsUtils.OVERWRITE, "1"));

      // Blacklist node after 2 tasks fails on it
      sparkProps.put(Settings.SPARK_BLACKLIST_MAX_TASK_ATTEMPTS_PER_NODE, new ConfigProperty(
        Settings.SPARK_BLACKLIST_MAX_TASK_ATTEMPTS_PER_NODE, HopsUtils.OVERWRITE, "2"));

      // If any task fails on an executor within a stage - blacklist it
      sparkProps.put(Settings.SPARK_BLACKLIST_STAGE_MAX_FAILED_TASKS_PER_EXECUTOR, new ConfigProperty(
        Settings.SPARK_BLACKLIST_STAGE_MAX_FAILED_TASKS_PER_EXECUTOR, HopsUtils.OVERWRITE, "1"));

      // Blacklist node after 2 tasks within a stage fails on it
      sparkProps.put(Settings.SPARK_BLACKLIST_STAGE_MAX_FAILED_TASKS_PER_NODE, new ConfigProperty(
        Settings.SPARK_BLACKLIST_STAGE_MAX_FAILED_TASKS_PER_NODE, HopsUtils.OVERWRITE, "2"));

      // If any task fails on an executor within an application - blacklist it
      sparkProps.put(Settings.SPARK_BLACKLIST_APPLICATION_MAX_FAILED_TASKS_PER_EXECUTOR, new ConfigProperty(
        Settings.SPARK_BLACKLIST_APPLICATION_MAX_FAILED_TASKS_PER_EXECUTOR, HopsUtils.OVERWRITE, "1"));

      // If 2 task fails on a node within an application - blacklist it
      sparkProps.put(Settings.SPARK_BLACKLIST_APPLICATION_MAX_FAILED_TASKS_PER_NODE, new ConfigProperty(
        Settings.SPARK_BLACKLIST_APPLICATION_MAX_FAILED_TASKS_PER_NODE, HopsUtils.OVERWRITE, "2"));

      // Always kill the blacklisted executors (further failures could be results of local files from the failed task)
      sparkProps.put(Settings.SPARK_BLACKLIST_KILL_BLACKLISTED_EXECUTORS, new ConfigProperty(
        Settings.SPARK_BLACKLIST_KILL_BLACKLISTED_EXECUTORS, HopsUtils.OVERWRITE, "true"));
    }

    //These settings are very important, a DL experiment should not be retried unless fault tolerance is enabled
    //If fault tolerance is enabled then TASK_MAX_FAILURES needs to be set to 3 to match the blacklisting
    // settings above
    if(experimentType != null) {
      // Blacklisting is enabled and we are dealing with an Experiment/Parallel Experiment
      if (sparkJobConfiguration.isBlacklistingEnabled() &&
              (experimentType == ExperimentType.EXPERIMENT || experimentType == ExperimentType.PARALLEL_EXPERIMENTS)) {
        sparkProps.put(Settings.SPARK_TASK_MAX_FAILURES, new ConfigProperty(
                Settings.SPARK_TASK_MAX_FAILURES, HopsUtils.OVERWRITE, "3"));
      // All other configurations should not retry to avoid wasting time during development (syntax errors etc)
      } else {
        sparkProps.put(Settings.SPARK_TASK_MAX_FAILURES, new ConfigProperty(
                Settings.SPARK_TASK_MAX_FAILURES, HopsUtils.OVERWRITE, "1"));
      }
    }

    extraJavaOptions.put(Settings.JOB_LOG4J_CONFIG, Settings.JOB_LOG4J_PROPERTIES);
    extraJavaOptions.put(Settings.HOPSWORKS_REST_ENDPOINT_PROPERTY, settings.getRestEndpoint());
    extraJavaOptions.put(Settings.HOPSUTIL_INSECURE_PROPERTY, String.valueOf(settings.isHopsUtilInsecure()));
    extraJavaOptions.put(Settings.SERVER_TRUSTSTORE_PROPERTY, Settings.SERVER_TRUSTSTORE_PROPERTY);
    extraJavaOptions.put(Settings.HOPSWORKS_ELASTIC_ENDPOINT_PROPERTY, settings.getElasticRESTEndpoint());
    extraJavaOptions.put(Settings.HOPSWORKS_PROJECTID_PROPERTY, Integer.toString(project.getId()));
    extraJavaOptions.put(Settings.HOPSWORKS_PROJECTNAME_PROPERTY, project.getName());
    extraJavaOptions.put(Settings.SPARK_JAVA_LIBRARY_PROP, settings.getHadoopSymbolicLinkDir() + "/lib/native/");
    extraJavaOptions.put(Settings.HOPSWORKS_PROJECTUSER_PROPERTY, hdfsUser);
    extraJavaOptions.put(Settings.KAFKA_BROKERADDR_PROPERTY, settings.getKafkaBrokersStr());
    extraJavaOptions.put(Settings.HOPSWORKS_JOBTYPE_PROPERTY, JobType.SPARK.name());
    extraJavaOptions.put(Settings.HOPSWORKS_DOMAIN_CA_TRUSTSTORE_PROPERTY,
        settings.getSparkConfDir() + File.separator + Settings.DOMAIN_CA_TRUSTSTORE);
    if(jobConfiguration.getAppName() != null) {
      extraJavaOptions.put(Settings.HOPSWORKS_JOBNAME_PROPERTY, jobConfiguration.getAppName());
    }

    StringBuilder extraJavaOptionsSb = new StringBuilder();
    for (String key : extraJavaOptions.keySet()) {
      extraJavaOptionsSb.append(" -D").append(key).append("=").append(extraJavaOptions.get(key));
    }

    sparkProps.put(Settings.SPARK_EXECUTOR_EXTRA_JAVA_OPTS, new ConfigProperty(
            Settings.SPARK_EXECUTOR_EXTRA_JAVA_OPTS, HopsUtils.APPEND_SPACE, extraJavaOptionsSb.toString()));

    sparkProps.put(Settings.SPARK_DRIVER_EXTRA_JAVA_OPTIONS, new ConfigProperty(
            Settings.SPARK_DRIVER_EXTRA_JAVA_OPTIONS, HopsUtils.APPEND_SPACE, extraJavaOptionsSb.toString()));

    String userSparkProperties = sparkJobConfiguration.getProperties();
    Map<String, String> validatedSparkProperties = HopsUtils.validateUserProperties(userSparkProperties,
      settings.getSparkDir());
    // Merge system and user defined properties
    Map<String, String> sparkParamsAfterMerge = HopsUtils.mergeHopsworksAndUserParams(sparkProps,
      validatedSparkProperties);
    return sparkParamsAfterMerge;

  }

  private void addToSparkEnvironment(Map<String, ConfigProperty> sparkProps, String envName,
                                     String value, ConfigReplacementPolicy replacementPolicy) {
    sparkProps.put(Settings.SPARK_EXECUTOR_ENV + envName,
            new ConfigProperty(Settings.SPARK_EXECUTOR_ENV + envName,
                    replacementPolicy, value));
    sparkProps.put(Settings.SPARK_YARN_APPMASTER_ENV + envName,
            new ConfigProperty(Settings.SPARK_YARN_APPMASTER_ENV + envName,
                    replacementPolicy, value));
  }

  //Clean comma-separated resource string
  private String formatResources(String commaSeparatedResources) {
    String[] resourceArr = commaSeparatedResources.split(",");
    StringBuilder resourceBuilder = new StringBuilder();
    for(String resource: resourceArr) {
      if(!resource.equals(",") && !resource.equals("")) {
        resourceBuilder.append(resource.trim()).append(",");
      }
    }
    if(resourceBuilder.charAt(resourceBuilder.length()-1) == ',') {
      resourceBuilder.deleteCharAt(resourceBuilder.length()-1);
    }
    return resourceBuilder.toString();
  }
}
