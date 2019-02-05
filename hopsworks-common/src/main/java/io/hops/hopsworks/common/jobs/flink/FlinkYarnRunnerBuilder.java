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

package io.hops.hopsworks.common.jobs.flink;

import io.hops.hopsworks.common.jobs.AsynchronousJobExecutor;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.jobs.yarn.LocalResourceDTO;
import io.hops.hopsworks.common.jobs.yarn.ServiceProperties;
import io.hops.hopsworks.common.jobs.yarn.YarnRunner;
import io.hops.hopsworks.common.util.Settings;
import java.io.File;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.yarn.api.records.LocalResource;
import org.apache.hadoop.yarn.api.records.LocalResourceType;
import org.apache.hadoop.yarn.api.records.LocalResourceVisibility;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.util.ConverterUtils;

/**
 * All classes in this package contain code taken from
 * https://github.com/apache/hadoop-common/blob/trunk/hadoop-yarn-project/
 * hadoop-yarn/hadoop-yarn-applications/hadoop-yarn-applications-distributedshell/
 * src/main/java/org/apache/hadoop/yarn/applications/distributedshell/Client.java?source=cc
 * and
 * https://github.com/hortonworks/simple-yarn-app
 * and
 * https://github.com/yahoo/storm-yarn/blob/master/src/main/java/com/yahoo/storm/yarn/StormOnYarn.java
 * <p>
 * The Flink jar is uploaded to HDFS by this client.
 * The application master and all the TaskManager containers get the jar file
 * downloaded
 * by YARN into their local fs.
 * <p>
 */
public class FlinkYarnRunnerBuilder {

  private static final Logger LOGGER = Logger.
          getLogger(FlinkYarnRunnerBuilder.class.getName());
  /**
   * Constants, all starting with ENV_ are used as environment variables to
   * pass values from the Client to the Application Master.
   */
  public final static String ENV_TM_MEMORY = "_CLIENT_TM_MEMORY";
  public final static String ENV_TM_COUNT = "_CLIENT_TM_COUNT";
  public final static String ENV_APP_ID = "_APP_ID";
  public final static String FLINK_JAR_PATH = "_FLINK_JAR_PATH"; // the Flink jar resource location (in HDFS).
  public static final String ENV_CLIENT_HOME_DIR = "_CLIENT_HOME_DIR";
  public static final String ENV_CLIENT_SHIP_FILES = "_CLIENT_SHIP_FILES";
  public static final String ENV_CLIENT_USERNAME = "_CLIENT_USERNAME";
  public static final String ENV_SLOTS = "_SLOTS";
  public static final String ENV_DETACHED = "_DETACHED";
  public static final String ENV_STREAMING_MODE = "_STREAMING_MODE";
  public static final String ENV_DYNAMIC_PROPERTIES = "_DYNAMIC_PROPERTIES";
  public static final String CONFIG_FILE_LOGBACK_NAME = "logback.xml";
  public static final String CONFIG_FILE_LOG4J_NAME = "log4j.properties";
  /**
   * Minimum memory requirements, checked by the Client.
   */
  private static final int MIN_JM_MEMORY = 768; // the minimum memory should be higher than the min heap cutoff
  private static final int MIN_TM_MEMORY = 768;

  //Jar paths for AM and app
  private String appJarPath;
  //Optional parameters
  private final List<String> jobArgs = new ArrayList<>();
  private List<LocalResourceDTO> extraFiles = new ArrayList<>();

  /**
   * If the user has specified a different number of slots, we store them here
   */
  private int taskManagerSlots = 1;
  private int jobManagerMemoryMb = 768;
  private String jobManagerQueue = "default";

  private int taskManagerMemoryMb = 1024;
  private int taskManagerCount = 1;
  private String configurationDirectory;
  private Path flinkConfigurationPath;
  private Path flinkLoggingConfigurationPath; // optional
  private Path flinkJarPath;
  private StringBuilder dynamicPropertiesEncoded;
  private boolean detached;
  private boolean streamingMode = true;
  private int parallelism;
  private String customName = null;
  private final Map<String, String> sysProps = new HashMap<>();
  private ServiceProperties serviceProps;

  public FlinkYarnRunnerBuilder(String appJarPath, String mainClass) {
    if (appJarPath == null || appJarPath.isEmpty()) {
      throw new IllegalArgumentException(
              "Path to application jar cannot be empty!");
    }
    if (mainClass == null || mainClass.isEmpty()) {
      throw new IllegalArgumentException(
              "Name of the main class cannot be empty!");
    }
    this.appJarPath = appJarPath;
  }

  public FlinkYarnRunnerBuilder addAllJobArgs(String[] jobArgs) {
    this.jobArgs.addAll(Arrays.asList(jobArgs));
    return this;
  }
  
  //@Override
  public void setAppJarPath(String appJarPath) {
    this.appJarPath = appJarPath;
  }

  public void setJobManagerMemory(int memoryMb) {
    if (memoryMb < MIN_JM_MEMORY) {
      throw new IllegalArgumentException("The JobManager memory (" + memoryMb
              + ") is below the minimum required memory amount "
              + "of " + MIN_JM_MEMORY + " MB");
    }
    this.jobManagerMemoryMb = memoryMb;
  }

  public void setJobManagerQueue(String queue) {
    this.jobManagerQueue = queue;
  }

  public void setTaskManagerMemory(int memoryMb) {
    if (memoryMb < MIN_TM_MEMORY) {
      throw new IllegalArgumentException("The TaskManager memory (" + memoryMb
              + ") is below the minimum required memory amount "
              + "of " + MIN_TM_MEMORY + " MB");
    }
    this.taskManagerMemoryMb = memoryMb;
  }

  public void setTaskManagerSlots(int slots) {
    if (slots <= 0) {
      throw new IllegalArgumentException(
              "Number of TaskManager slots must be positive");
    }
    this.taskManagerSlots = slots;
  }

  public int getTaskManagerSlots() {
    return this.taskManagerSlots;
  }

  public void setQueue(String queue) {
    this.jobManagerQueue = queue;
  }

  public void setConfigurationFilePath(Path confPath) {
    flinkConfigurationPath = confPath;
  }

  public void setConfigurationDirectory(String configurationDirectory) {
    this.configurationDirectory = configurationDirectory;
  }

  public void setFlinkLoggingConfigurationPath(Path logConfPath) {
    flinkLoggingConfigurationPath = logConfPath;
  }

  public Path getFlinkLoggingConfigurationPath() {
    return flinkLoggingConfigurationPath;
  }

  public void setTaskManagerCount(int tmCount) {
    if (tmCount < 1) {
      throw new IllegalArgumentException(
              "The TaskManager count has to be at least 1.");
    }
    this.taskManagerCount = tmCount;
  }

  public int getTaskManagerCount() {
    return this.taskManagerCount;
  }

  public boolean isStreamingMode() {
    return streamingMode;
  }

  public void setStreamingMode(boolean streamingMode) {
    this.streamingMode = streamingMode;
  }

  public void setDynamicPropertiesEncoded(StringBuilder dynamicPropertiesEncoded) {
    this.dynamicPropertiesEncoded = dynamicPropertiesEncoded;
  }

  public StringBuilder getDynamicPropertiesEncoded() {
    return this.dynamicPropertiesEncoded;
  }

  public void setName(String name) {
    if (name == null) {
      throw new IllegalArgumentException("The passed name is null");
    }
    customName = name;
  }

  public FlinkYarnRunnerBuilder setExtraFiles(List<LocalResourceDTO> extraFiles) {
    if (extraFiles == null) {
      throw new IllegalArgumentException("Map of extra files cannot be null.");
    }
    this.extraFiles = extraFiles;
    return this;
  }

  public FlinkYarnRunnerBuilder addExtraFile(LocalResourceDTO dto) {
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

  public FlinkYarnRunnerBuilder addExtraFiles(
          List<LocalResourceDTO> projectLocalResources) {
    if (projectLocalResources != null && !projectLocalResources.isEmpty()) {
      this.extraFiles.addAll(projectLocalResources);
    }
    return this;
  }

  public FlinkYarnRunnerBuilder addSystemProperty(String name, String value) {
    sysProps.put(name, value);
    return this;
  }

  public void setServiceProps(ServiceProperties serviceProps) {
    this.serviceProps = serviceProps;
  }

  public void isReadyForDeployment() throws YarnDeploymentException {
    if (taskManagerCount <= 0) {
      throw new YarnDeploymentException("Taskmanager count must be positive");
    }
    if (this.flinkJarPath == null) {
      throw new YarnDeploymentException("The Flink jar path is null");
    }
    if (this.configurationDirectory == null) {
      throw new YarnDeploymentException("Configuration directory not set");
    }
    if (this.flinkConfigurationPath == null) {
      throw new YarnDeploymentException("Configuration path not set");
    }

    // check if required Hadoop environment variables are set. If not, warn user
    if (System.getenv(Settings.ENV_KEY_HADOOP_CONF_DIR) == null
            && System.getenv(Settings.ENV_KEY_YARN_CONF) == null) {
      LOGGER.log(Level.WARNING,
              "Neither the HADOOP_CONF_DIR nor the YARN_CONF_DIR environment variable is set."
              + "The Flink YARN Client needs one of these to be set to properly load the Hadoop "
              + "configuration for accessing YARN.");
    }
  }

  public static boolean allocateResource(int[] nodeManagers, int toAllocate) {
    for (int i = 0; i < nodeManagers.length; i++) {
      if (nodeManagers[i] >= toAllocate) {
        nodeManagers[i] -= toAllocate;
        return true;
      }
    }
    return false;
  }

  public void setDetachedMode(boolean detachedMode) {
    this.detached = detachedMode;
  }

  public boolean isDetached() {
    return detached;
  }

  public int getParallelism() {
    return parallelism;
  }

  public void setParallelism(int parallelism) {
    this.parallelism = parallelism;
  }
 
  /**
   * This method will block until the ApplicationMaster/JobManager have been
   * deployed on YARN.
   *
   * @param project
   * @param hadoopDir
   * @param jobUser
   * @param flinkUser
   * @param flinkDir
   * @param flinkConfDir
   * @param flinkConfFile
   * @param certsDir
   * @param services
   * @return
   * @throws java.io.IOException
   */
  protected YarnRunner getYarnRunner(String project, final String flinkUser,
          String jobUser, String hadoopDir, final String flinkDir,
          final String flinkConfDir, final String flinkConfFile,
          DistributedFileSystemOps dfsClient,
          YarnClient yarnClient, final String certsDir,
          AsynchronousJobExecutor services) throws IOException {

    //Create the YarnRunner builder for Flink, proceed with setting values
    YarnRunner.Builder builder = new YarnRunner.Builder(Settings.FLINK_AM_MAIN);
    YarnClusterDescriptor cluster = new YarnClusterDescriptor();
    //TODO: Change the cluster to use files from hdfs
    cluster.setConfigurationDirectory(flinkConfDir);
    cluster.setConfigurationFilePath(new Path(flinkConfFile));
    cluster.setDetachedMode(detached);

    org.apache.flink.configuration.Configuration flinkConf
            = new org.apache.flink.configuration.Configuration();
    cluster.setFlinkConfiguration(flinkConf);
    cluster.setJobManagerMemory(jobManagerMemoryMb);
    cluster.setTaskManagerCount(taskManagerCount);
    cluster.setTaskManagerMemory(taskManagerMemoryMb);
    cluster.setTaskManagerSlots(taskManagerSlots);
    cluster.setQueue(jobManagerQueue);
    cluster.setLocalJarPath(new Path("file://" + flinkDir + "/flink.jar"));

    builder.setYarnClient(yarnClient);
    builder.setDfsClient(dfsClient);
    builder.setJobUser(jobUser);
    builder.setFlinkCluster(cluster);
    
    String stagingPath = File.separator + "Projects" + File.separator + project
            + File.separator
            + Settings.PROJECT_STAGING_DIR;
    builder.localResourcesBasePath(stagingPath);
    
    //Add extra files to local resources, use filename as key
    //Get filesystem
    if (!extraFiles.isEmpty()) {
      if (null == dfsClient) {
        throw new YarnDeploymentException("Could not connect to filesystem");
      }
      FileSystem fs = dfsClient.getFilesystem();
      for (LocalResourceDTO dto : extraFiles) {
        String pathToResource = dto.getPath();
        pathToResource = pathToResource.replaceFirst("hdfs:/*Projects",
                "hdfs:///Projects");
        pathToResource = pathToResource.replaceFirst("hdfs:/*user",
                "hdfs:///user");
        Path src = new Path(pathToResource);
        FileStatus scFileStat = fs.getFileStatus(src);
        LocalResource resource = LocalResource.newInstance(ConverterUtils.
                getYarnUrlFromPath(src),
                LocalResourceType.valueOf(dto.getType().toUpperCase()),
                LocalResourceVisibility.valueOf(dto.getVisibility().
                        toUpperCase()),
                scFileStat.getLen(),
                scFileStat.getModificationTime(),
                dto.getPattern());
        cluster.addHopsworksResource(dto.getName(), resource);
      }
    }
    addSystemProperty(Settings.HOPSWORKS_REST_ENDPOINT_PROPERTY, serviceProps.getRestEndpoint());
    if (serviceProps.getKafka() != null) {
      
      addSystemProperty(Settings.KAFKA_BROKERADDR_PROPERTY, serviceProps.getKafka().getBrokerAddresses());
      addSystemProperty(Settings.KAFKA_JOB_TOPICS_PROPERTY, serviceProps.getKafka().getTopics());
      addSystemProperty(Settings.HOPSWORKS_PROJECTID_PROPERTY, Integer.toString(serviceProps.getProjectId()));
      if (serviceProps.getKafka().getConsumerGroups() != null) {
        addSystemProperty(Settings.KAFKA_CONSUMER_GROUPS,serviceProps.getKafka().getConsumerGroups());
      }
    }
    if (!sysProps.isEmpty()) {
      dynamicPropertiesEncoded = new StringBuilder();
      for (String s : sysProps.keySet()) {
        String option = YarnRunner.escapeForShell("-D" + s + "=" + sysProps.get(s));
        builder.addJavaOption(option);
        cluster.addHopsworksParam(option);
        dynamicPropertiesEncoded.append(s).append("=").append(sysProps.get(s)).
                append("@@");
      }

      /*
       * Split propertes with "@@"
       * https://github.com/apache/flink/blob/b410c393c960f55c09fadd4f22732d06f801b938/
       * flink-yarn/src/main/java/org/apache/flink/yarn/cli/FlinkYarnSessionCli.java
       */
      if (dynamicPropertiesEncoded.length() > 0) {
        cluster.setDynamicPropertiesEncoded(dynamicPropertiesEncoded.
                substring(0,
                        dynamicPropertiesEncoded.
                                lastIndexOf("@@")));
      }
    }

    builder.setJobType(JobType.FLINK);
    builder.setAppJarPath(appJarPath);
    builder.setParallelism(parallelism);

    String name;
    if (customName == null) {
      name = "Flink session with " + taskManagerCount + " TaskManagers";
      if (detached) {
        name += " (detached)";
      }
    } else {
      name = customName;
    }
    cluster.setName(name);
    //Set up command
    StringBuilder amargs = new StringBuilder("");
    //Pass job arguments
    for (String s : jobArgs) {
      amargs.append(" ").append(s);
    }
    if (!amargs.toString().equals("")) {
      builder.amArgs(amargs.toString());
    }
    return builder.build(flinkDir, JobType.FLINK,services);
  }

  public static class YarnDeploymentException extends RuntimeException {

    private static final long serialVersionUID = -812040641215388943L;

    public YarnDeploymentException() {
    }

    public YarnDeploymentException(String message) {
      super(message);
    }

    public YarnDeploymentException(String message, Throwable cause) {
      super(message, cause);
    }
  }

}
