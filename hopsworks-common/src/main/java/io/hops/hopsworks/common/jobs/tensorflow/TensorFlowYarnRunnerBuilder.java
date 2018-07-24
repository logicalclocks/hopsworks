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

package io.hops.hopsworks.common.jobs.tensorflow;

import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.jobs.AsynchronousJobExecutor;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.jobs.yarn.LocalResourceDTO;
import io.hops.hopsworks.common.jobs.yarn.ServiceProperties;
import io.hops.hopsworks.common.jobs.yarn.YarnRunner;
import io.hops.hopsworks.common.util.Settings;
import io.hops.tensorflow.Client;
import io.hops.tensorflow.LocalResourceInfo;
import org.apache.hadoop.yarn.client.api.YarnClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * <p>
 */
public class TensorFlowYarnRunnerBuilder {

  //Necessary parameters
  private final Jobs job;
  private final List<String> jobArgs = new ArrayList<>();
  private String jobName = "Untitled TensorFlow Job";
  private int numOfWorkers;
  private int numOfPs;

  private int workerMemory = 1024;
  private int workerVCores = 1;
  private int workerGPUs = 1;

  private int amMemory = 1024; // in MB
  private int amVCores = 1;
  private String queue;
  private ServiceProperties serviceProps;

  private List<LocalResourceDTO> extraFiles = new ArrayList<>();

  public TensorFlowYarnRunnerBuilder(Jobs job) {
    this.job = job;
    TensorFlowJobConfiguration jobConfig = (TensorFlowJobConfiguration) job.getJobConfig();

    if (jobConfig.getAppPath() == null || jobConfig.getAppPath().isEmpty()) {
      throw new IllegalArgumentException(
          "Path to application executable cannot be empty!");
    }
  }

  public YarnRunner getYarnRunner(String project, String tfUser,
      String jobUser,
      final DistributedFileSystemOps dfsClient, YarnClient yarnClient,
      AsynchronousJobExecutor services, Settings settings) throws IOException, Exception {

    YarnRunner.Builder builder = new YarnRunner.Builder(Settings.SPARK_AM_MAIN);
    JobType jobType = ((TensorFlowJobConfiguration) job.getJobConfig()).getType();
    builder.setJobType(jobType);
    builder.setYarnClient(yarnClient);
    builder.setDfsClient(dfsClient);
    Client client = new Client();

    //Add extra files to local resources, use filename as key
    for (LocalResourceDTO dto : extraFiles) {
      if (!dto.getName().equals(Settings.K_CERTIFICATE)
          && !dto.getName().equals(Settings.T_CERTIFICATE)
          && !dto.getName().equals(Settings.CRYPTO_MATERIAL_PASSWORD)) {
        String pathToResource = dto.getPath();
        pathToResource = pathToResource.replaceFirst("hdfs:/*Projects",
            "hdfs:///Projects");
        pathToResource = pathToResource.replaceFirst("hdfs:/*user",
            "hdfs:///user");
        client.addFile(pathToResource);
        client.getFilesInfo().put(pathToResource, new LocalResourceInfo(dto.getName(), pathToResource, dto.
            getVisibility(), dto.getType(), dto.getPattern()));
      }
    }

    client.setAmMemory(amMemory);
    client.setAmVCores(amVCores);
    client.setMemory(workerMemory);
    client.setVcores(workerVCores);
    client.setGpus(workerGPUs);
    client.setQueue(queue);
    client.setName(jobName);
    client.setNumPses(numOfPs);
    client.setNumWorkers(numOfWorkers);
    client.setTensorboard(true);
    client.addEnvironmentVariable(Settings.HADOOP_USER_NAME, jobUser);
    client.addEnvironmentVariable("HADOOP_HOME", settings.getHadoopSymbolicLinkDir());
    client.addEnvironmentVariable("HADOOP_VERSION", settings.getHadoopVersion());
    client.addEnvironmentVariable(Settings.LOGSTASH_JOB_INFO, project.toLowerCase() + "," + jobName + ","
        + job.getId() + "," + YarnRunner.APPID_PLACEHOLDER);
    client.addEnvironmentVariable(Settings.YARNTF_HOME_DIR,
        "hdfs:///Projects/" + project
        + "/" + Settings.PROJECT_STAGING_DIR + "/" + Settings.YARNTF_STAGING_DIR);
    String appPath = ((TensorFlowJobConfiguration) job.getJobConfig()).getAppPath();
    appPath = appPath.replaceFirst("hdfs:/*Projects",
        "hdfs:///Projects");
    client.setMain(appPath);
    client.setArguments(jobArgs.stream().toArray(String[]::new));
    client.setAmJar(settings.getTensorFlowJarPath(tfUser));
    //client.setPython(serviceProps.getAnaconda().getEnvPath());
    client.setAllocationTimeout(15000);
    client.setProjectDir("hdfs://" + Settings.DIR_ROOT + "/" +project);
    builder.setTfClient(client);
    return builder.build(null, JobType.TENSORFLOW, services);
  }

  public TensorFlowYarnRunnerBuilder addAllJobArgs(List<String> jobArgs) {
    this.jobArgs.addAll(jobArgs);
    return this;
  }

  public TensorFlowYarnRunnerBuilder addAllJobArgs(String[] jobArgs) {
    this.jobArgs.addAll(Arrays.asList(jobArgs));
    return this;
  }

  public TensorFlowYarnRunnerBuilder addJobArg(String jobArg) {
    jobArgs.add(jobArg);
    return this;
  }

  public void setExtraFiles(List<LocalResourceDTO> extraFiles) {
    if (extraFiles == null) {
      throw new IllegalArgumentException("Map of extra files cannot be null.");
    }
    this.extraFiles = extraFiles;
  }

  public void addExtraFile(LocalResourceDTO dto) {
    if (dto.getName() == null || dto.getName().isEmpty()) {
      throw new IllegalArgumentException(
          "Filename in extra file mapping cannot be null or empty.");
    }
    if (dto.getPath() == null || dto.getPath().isEmpty()) {
      throw new IllegalArgumentException(
          "Location in extra file mapping cannot be null or empty.");
    }
    this.extraFiles.add(dto);
  }

  public void addExtraFiles(
      List<LocalResourceDTO> projectLocalResources) {
    if (projectLocalResources != null && !projectLocalResources.isEmpty()) {
      this.extraFiles.addAll(projectLocalResources);
    }
  }

  public void setJobName(String jobName) {
    this.jobName = jobName;
  }

  public void setNumOfWorkers(int numOfWorkers) {
    this.numOfWorkers = numOfWorkers;
  }

  public void setNumOfPs(int numOfPs) {
    this.numOfPs = numOfPs;
  }

  public void setAmMemory(int amMemory) {
    this.amMemory = amMemory;
  }

  public void setAmVCores(int amVCores) {
    this.amVCores = amVCores;
  }

  public void setQueue(String queue) {
    this.queue = queue;
  }

  public void setServiceProps(ServiceProperties serviceProps) {
    this.serviceProps = serviceProps;
  }

  public void setWorkerMemory(int workerMemory) {
    this.workerMemory = workerMemory;
  }

  public void setWorkerVCores(int workerVCores) {
    this.workerVCores = workerVCores;
  }

  public void setWorkerGPUs(int workerGPUs) {
    this.workerGPUs = workerGPUs;
  }

}
