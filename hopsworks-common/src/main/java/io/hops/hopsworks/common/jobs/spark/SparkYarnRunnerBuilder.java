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
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.jobs.AsynchronousJobExecutor;
import io.hops.hopsworks.common.jobs.configuration.JobType;
import io.hops.hopsworks.common.jobs.yarn.LocalResourceDTO;
import io.hops.hopsworks.common.jobs.yarn.ServiceProperties;
import io.hops.hopsworks.common.jobs.yarn.YarnRunner;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.util.SparkConfigurationUtil;
import io.hops.hopsworks.common.util.templates.ConfigProperty;
import org.apache.hadoop.yarn.api.records.LocalResourceType;
import org.apache.hadoop.yarn.api.records.LocalResourceVisibility;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.parquet.Strings;

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

/**
 * Builder class for a Spark YarnRunner. Implements the common logic needed
 * for any Spark job to be started and builds a YarnRunner instance.
 * <p>
 */
public class SparkYarnRunnerBuilder {

  //Necessary parameters
  private final Jobs job;
  private final SparkJobConfiguration sparkJobConfiguration;

  //Optional parameters
  private final List<String> jobArgs = new ArrayList<>();
  private String jobName = "Untitled Spark Job";
  private final List<LocalResourceDTO> extraFiles = new ArrayList<>();

  private final Map<String, String> sysProps = new HashMap<>();
  private ServiceProperties serviceProps;
  private SparkConfigurationUtil sparkConfigurationUtil = new SparkConfigurationUtil();

  public SparkYarnRunnerBuilder(Jobs job) {
    this.job = job;
    this.sparkJobConfiguration = (SparkJobConfiguration) job.
        getJobConfig();

    if (sparkJobConfiguration.getAppPath() == null || sparkJobConfiguration.getAppPath().isEmpty()) {
      throw new IllegalArgumentException(
          "Path to application executable cannot be empty!");
    }
    if (sparkJobConfiguration.getMainClass() == null || sparkJobConfiguration.getMainClass().isEmpty()) {
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
  public YarnRunner getYarnRunner(Project project,
      String jobUser, String usersFullName, AsynchronousJobExecutor services,
      final DistributedFileSystemOps dfsClient, final YarnClient yarnClient,
      Settings settings) throws IOException {

    Map<String, ConfigProperty> jobHopsworksProps = new HashMap<>();
    JobType jobType = job.getJobConfig().getJobType();
    String appPath = ((SparkJobConfiguration) job.getJobConfig()).getAppPath();
    //Create a builder
    YarnRunner.Builder builder = new YarnRunner.Builder(Settings.SPARK_AM_MAIN);
    builder.setJobType(jobType);
    builder.setYarnClient(yarnClient);
    builder.setDfsClient(dfsClient);

    /**
     * * 1. Set stagingPath **
     */
    String stagingPath = "/Projects/" + project.getName() + "/" + Settings.PROJECT_STAGING_DIR + "/.sparkjobstaging-"
      + YarnRunner.APPID_PLACEHOLDER;
    builder.localResourcesBasePath(stagingPath);
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * * 2. Set job local resources, i.e. project certificates, job jar etc. **
     */
    //Add hdfs prefix so the monitor knows it should find it there
    builder.addFileToRemove("hdfs://" + stagingPath);

    //Add app file
    String appExecName = null;
    if (jobType == JobType.SPARK) {
      appExecName = Settings.SPARK_LOCRSC_APP_JAR;
    } else if (jobType == JobType.PYSPARK) {

      appExecName = appPath.substring(appPath.lastIndexOf(File.separator) + 1);
    }

    builder.addLocalResource(new LocalResourceDTO(
        appExecName, appPath,
        LocalResourceVisibility.APPLICATION.toString(),
        LocalResourceType.FILE.toString(), null), dfsClient);

    builder.addToAppMasterEnvironment(YarnRunner.KEY_CLASSPATH,
        Settings.SPARK_LOCRSC_APP_JAR
    );

    //Set executor extraJavaOptions to make parameters available to executors
    Map<String, String> extraJavaOptions = new HashMap<>();

    //These properties are set so that spark history server picks them up
    jobHopsworksProps.put(Settings.SPARK_DRIVER_STAGINGDIR_ENV,
          new ConfigProperty(
                  Settings.SPARK_DRIVER_STAGINGDIR_ENV,
                  HopsUtils.IGNORE,
                  stagingPath));
    jobHopsworksProps.put(Settings.HOPSWORKS_APPID_PROPERTY,
          new ConfigProperty(
                  Settings.HOPSWORKS_APPID_PROPERTY,
                  HopsUtils.IGNORE,
                  YarnRunner.APPID_PLACEHOLDER));

    extraJavaOptions.put(Settings.HOPSWORKS_APPID_PROPERTY, YarnRunner.APPID_PLACEHOLDER);
    extraJavaOptions.put(Settings.LOGSTASH_JOB_INFO,
            project.getName().toLowerCase() + "," + jobName + "," + job.getId() + ","
                    + YarnRunner.APPID_PLACEHOLDER);
    //Set up command
    StringBuilder amargs = new StringBuilder("--class ");
    amargs.append(((SparkJobConfiguration) job.getJobConfig()).
        getMainClass());

    if (jobType == JobType.PYSPARK) {
      amargs.append(" --primary-py-file ").append(appExecName);
      //Add libs to PYTHONPATH
      if (!serviceProps.isAnacondaEnabled()) {
        //Throw error in Hopswors UI to notify user to enable Anaconda
        throw new IOException("PySpark job needs to have Python Anaconda environment enabled");
      }
    }

    String tfLibraryPath = services.getTfLibMappingUtil().getTfLdLibraryPath(project);

    Map<String, String> finalJobProps = new HashMap<>();
    finalJobProps.putAll(sparkConfigurationUtil.getFrameworkProperties(project, job.getJobConfig(), settings,
            jobUser, usersFullName, tfLibraryPath, extraJavaOptions));

    finalJobProps.put(Settings.SPARK_YARN_APPMASTER_ENV + "SPARK_USER", jobUser);
    finalJobProps.put(Settings.SPARK_EXECUTOR_ENV + "SPARK_USER", jobUser);
    finalJobProps.put(Settings.SPARK_YARN_APPMASTER_ENV + "SPARK_YARN_MODE", "true");
    finalJobProps.put(Settings.SPARK_YARN_APPMASTER_ENV + "SPARK_YARN_STAGING_DIR", stagingPath);

    //Parse properties from Spark config file
    Properties sparkProperties = new Properties();
    try (InputStream is = new FileInputStream(settings.getSparkDir() + "/" + Settings.SPARK_CONFIG_FILE)) {
      sparkProperties.load(is);
      //For every property that is in the spark configuration file but is not already set, create a system property.
      for (String property : sparkProperties.stringPropertyNames()) {
        if (!finalJobProps.containsKey(property)) {
          finalJobProps.put(property,
                  sparkProperties.getProperty(property).trim());
        }
      }
    }
    
    for (String jvmOption : finalJobProps.get(Settings.SPARK_DRIVER_EXTRA_JAVA_OPTIONS).split(" +")) {
      builder.addJavaOption(jvmOption);
    }

    for (String key : finalJobProps.keySet()) {
      if(key.startsWith("spark.yarn.appMasterEnv.")) {
        builder.addToAppMasterEnvironment(key.replace("spark.yarn.appMasterEnv.", ""),
                finalJobProps.get(key));
      }
      addSystemProperty(key, finalJobProps.get(key));
    }

    builder.addToAppMasterEnvironment("CLASSPATH", finalJobProps.get(Settings.SPARK_DRIVER_EXTRACLASSPATH));

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
    builder.amMemory(sparkJobConfiguration.getAmMemory());
    builder.amVCores(sparkJobConfiguration.getAmVCores());
    builder.amQueue(sparkJobConfiguration.getAmQueue());

    //pyfiles, jars and files are distributed as spark.yarn.dist.files
    String hopsFiles = finalJobProps.get("spark.yarn.dist.files");
    if(!Strings.isNullOrEmpty(hopsFiles)) {
      for (String filePath : hopsFiles.split(",")) {
        String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
        if(filePath.contains("#")) {
          fileName = filePath.split("#")[1];
          filePath = filePath.substring(0, filePath.indexOf("#"));
        }
        builder.addLocalResource(new LocalResourceDTO(
                fileName, filePath,
                LocalResourceVisibility.APPLICATION.toString(),
                LocalResourceType.FILE.toString(), null), dfsClient);
      }
    }

    String archives = finalJobProps.get("spark.yarn.dist.archives");
    if(!Strings.isNullOrEmpty(archives)) {
      for (String archivePath : archives.split(",")) {
        String fileName = archivePath.substring(archivePath.lastIndexOf("/") + 1);
        if(archivePath.contains("#")) {
          fileName = archivePath.split("#")[1];
          archivePath = archivePath.substring(0, archivePath.indexOf("#"));
        }
        builder.addLocalResource(new LocalResourceDTO(
          fileName, archivePath,
          LocalResourceVisibility.APPLICATION.toString(),
          LocalResourceType.ARCHIVE.toString(), null), dfsClient);
      }
    }

    //Set app name
    builder.appName(jobName);

    return builder.build(settings.getSparkDir(), JobType.SPARK, services);
  }

  public SparkYarnRunnerBuilder setJobName(String jobName) {
    this.jobName = jobName;
    return this;
  }

  public SparkYarnRunnerBuilder addAllJobArgs(String[] jobArgs) {
    this.jobArgs.addAll(Arrays.asList(jobArgs));
    return this;
  }

  public SparkYarnRunnerBuilder addExtraFiles(List<LocalResourceDTO> projectLocalResources) {
    if (projectLocalResources != null && !projectLocalResources.isEmpty()) {
      this.extraFiles.addAll(projectLocalResources);
    }
    return this;
  }

  public void setServiceProps(ServiceProperties serviceProps) {
    this.serviceProps = serviceProps;
  }

  public SparkYarnRunnerBuilder addSystemProperty(String name, String value) {
    sysProps.put(name, value);
    return this;
  }
}
