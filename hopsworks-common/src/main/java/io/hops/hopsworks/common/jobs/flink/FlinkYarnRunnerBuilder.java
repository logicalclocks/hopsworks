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

import com.google.common.base.Strings;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.common.jobs.AsynchronousJobExecutor;
import io.hops.hopsworks.common.jobs.yarn.YarnRunner;
import io.hops.hopsworks.common.serving.ServingConfig;
import io.hops.hopsworks.common.util.FlinkConfigurationUtil;
import io.hops.hopsworks.common.util.ProjectUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.persistence.entity.jobs.configuration.JobType;
import io.hops.hopsworks.persistence.entity.jobs.configuration.flink.FlinkJobConfiguration;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import org.apache.flink.client.deployment.ClusterSpecification;
import org.apache.flink.yarn.YarnClientYarnClusterInformationRetriever;
import org.apache.flink.yarn.YarnClusterDescriptor;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.conf.YarnConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.flink.configuration.GlobalConfiguration.loadConfiguration;

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
  
  //Necessary parameters
  private final Jobs job;
  private final FlinkJobConfiguration flinkJobConfiguration;
  private FlinkConfigurationUtil flinkConfigurationUtil = new FlinkConfigurationUtil();

  private final Map<String, String> dynamicProperties = new HashMap<>();
  
  FlinkYarnRunnerBuilder(Jobs job) {
    this.job = job;
    this.flinkJobConfiguration = (FlinkJobConfiguration) job.
      getJobConfig();
    
  }

  void addDynamicProperty(String name, String value) {
    dynamicProperties.put(name, value);
  }

  YarnRunner getYarnRunner(Project project, String jobUser, Users hopsworksUser, DistributedFileSystemOps dfsClient,
                           YarnClient yarnClient, AsynchronousJobExecutor services, Settings settings,
                           String kafkaBrokersString, String hopsworksRestEndpoint, ServingConfig servingConfig,
                           ServiceDiscoveryController serviceDiscoveryController)
      throws IOException, ServiceDiscoveryException {
  
    String stagingPath = File.separator + "Projects" + File.separator + project.getName() + File.separator
      + Settings.PROJECT_STAGING_DIR +  "/.flinkStaging/" +  hopsworksUser.getUsername();
  
    Configuration conf = services.getSettings().getConfiguration();
    //Create the YarnRunner builder for Flink, proceed with setting values
    YarnRunner.Builder builder = new YarnRunner.Builder(Settings.FLINK_AM_MAIN);
  
    org.apache.flink.configuration.Configuration flinkConf
      = loadConfiguration(settings.getFlinkConfDir());
    YarnConfiguration yarnConf = new YarnConfiguration(conf);
  
    try {
      yarnConf
        .addResource(new File(settings.getHadoopConfDir() + "/" + Settings.DEFAULT_YARN_CONFFILE_NAME).toURI().toURL());
    } catch (MalformedURLException t) {
      throw new RuntimeException("Error", t);
    }
  
    Map<String, String> extraJavaOptions = new HashMap<>();
    extraJavaOptions.put(Settings.LOGSTASH_JOB_INFO,
      project.getName().toLowerCase() + "," + job.getName() + "," + job.getId() + "," + YarnRunner.APPID_PLACEHOLDER);
  
    Map<String, String> finalJobProps = flinkConfigurationUtil
      .setFrameworkProperties(project, job.getJobConfig(), settings, jobUser, hopsworksUser, extraJavaOptions,
        kafkaBrokersString, hopsworksRestEndpoint, servingConfig, serviceDiscoveryController);

    addDynamicProperty(Settings.LOGSTASH_JOB_INFO,
      project.getName().toLowerCase() + "," + job.getName() + "," + job.getId() + "," + YarnRunner.APPID_PLACEHOLDER);
  
    for (String key : finalJobProps.keySet()) {
      flinkConf.setString(key, finalJobProps.get(key));
    }
  
    if (!dynamicProperties.isEmpty()) {
      for (String s : dynamicProperties.keySet()) {
        flinkConf.setString(s, dynamicProperties.get(s));
      }
    }
  
    if (Strings.isNullOrEmpty(flinkJobConfiguration.getAppName())) {
      flinkJobConfiguration
        .setAppName("Flink session with " + flinkJobConfiguration.getNumberOfTaskSlots() + " " + "NumberOfTaskSlots");
    }
    flinkConf.setString("containerized.master.env.HOPSWORKS_JOB_NAME", flinkJobConfiguration.getAppName());

    //Create dynamicProperties from finalJobProps
    YarnClusterDescriptor cluster = new YarnClusterDescriptor(flinkConf,
            yarnConf, yarnClient, YarnClientYarnClusterInformationRetriever.create(yarnClient), true);
    
    ClusterSpecification clusterSpecification = new ClusterSpecification.ClusterSpecificationBuilder()
                 .setMasterMemoryMB(flinkJobConfiguration.getJobManagerMemory())
                 .setTaskManagerMemoryMB(flinkJobConfiguration.getTaskManagerMemory())
                 .setSlotsPerTaskManager(flinkJobConfiguration.getNumberOfTaskSlots())
                 .createClusterSpecification();
  
    List<File> shipFiles = new ArrayList<>();
    shipFiles.add(new File(settings.getFlinkDir(), "lib"));
    cluster.addShipFiles(shipFiles);
    
    cluster.setLocalJarPath(new Path(settings.getLocalFlinkJarPath()));
    cluster.setDocker(ProjectUtils.getFullDockerImageName(project, settings, serviceDiscoveryController, true),
        settings.getDockerMounts());

    builder.setYarnClient(yarnClient);
    builder.setDfsClient(dfsClient);
    builder.setFlinkCluster(cluster);
    builder.setFlinkClusterSpecification(clusterSpecification);
    builder.localResourcesBasePath(stagingPath);
    
    builder.setJobType(JobType.FLINK);

    return builder.build(settings.getFlinkDir(), JobType.FLINK,services);
  }
}
