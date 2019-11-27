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

package io.hops.hopsworks.common.jobs.yarn;

import io.hops.hopsworks.common.dao.jobhistory.Execution;
import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;
import io.hops.hopsworks.common.dao.project.service.ProjectServiceEnum;
import io.hops.hopsworks.common.dao.project.service.ProjectServices;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.jobs.configuration.JobType;
import io.hops.hopsworks.common.jobs.jobhistory.JobState;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.yarn.YarnClientService;
import io.hops.hopsworks.common.yarn.YarnClientWrapper;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.exceptions.YarnException;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@DependsOn("Settings")
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class YarnExecutionFinalizer {

  private static final Logger LOGGER = Logger.getLogger(YarnExecutionFinalizer.class.getName());

  @EJB
  private ExecutionFacade executionFacade;
  @EJB
  private Settings settings;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private YarnClientService ycs;

  @Asynchronous
  public Future<Execution> copyLogs(Execution exec) {
    DistributedFileSystemOps udfso = dfs.getDfsOps(exec.getHdfsUser());
    ApplicationId applicationId = ApplicationId.fromString(exec.getAppId());
    YarnClientWrapper yarnClientWrapper = ycs.getYarnClientSuper(settings.getConfiguration());
    YarnMonitor monitor = new YarnMonitor(applicationId, yarnClientWrapper, ycs);

    try {
      String defaultOutputPath;
      switch (exec.getJob().getJobType()) {
        case SPARK:
        case PYSPARK:
          defaultOutputPath = Settings.SPARK_DEFAULT_OUTPUT_PATH;
          break;
        case FLINK:
          defaultOutputPath = Settings.FLINK_DEFAULT_OUTPUT_PATH;
          break;
        case YARN:
          defaultOutputPath = Settings.YARN_DEFAULT_OUTPUT_PATH;
          break;
        default:
          defaultOutputPath = "Logs/";
      }
      String stdOutFinalDestination =
          Utils.getProjectPath(exec.getJob().getProject().getName()) +
              defaultOutputPath;
      String stdErrFinalDestination =
          Utils.getProjectPath(exec.getJob().getProject().getName()) +
              defaultOutputPath;
  
      String stdOutPath = settings.getAggregatedLogPath(exec.getHdfsUser(), exec.getAppId());
      try {
        stdOutFinalDestination = stdOutFinalDestination + exec.getAppId() + File.separator + "stdout.log";
        String[] desiredOutLogTypes = {"out"};

        YarnLogUtil.copyAggregatedYarnLogs(udfso, stdOutPath, stdOutFinalDestination,
            desiredOutLogTypes, monitor);

        stdErrFinalDestination = stdErrFinalDestination + exec.getAppId() + File.separator + "stderr.log";
        String[] desiredErrLogTypes = {"err", ".log"};
        YarnLogUtil.copyAggregatedYarnLogs(udfso, stdOutPath, stdErrFinalDestination,
                desiredErrLogTypes, monitor);
      } catch (IOException | InterruptedException | YarnException ex) {
        LOGGER.log(Level.SEVERE,"error while aggregation logs" + ex.toString());
      }
      Execution execution = updateExecutionSTDPaths(stdOutFinalDestination, stdErrFinalDestination, exec);
      finalize(exec, exec.getState());
      return new AsyncResult<>(execution);
    } finally {
      dfs.closeDfsClient(udfso);
      monitor.close();
    }
  }

  @Asynchronous
  public void finalize(Execution exec, JobState jobState) {
    //The execution won't exist in the database, if the job has been deleted.
    if (executionFacade.findById(exec.getId()) != null) {
      long executionStop = System.currentTimeMillis();
      exec = executionFacade.updateExecutionStop(exec, executionStop);
      executionFacade.updateState(exec, jobState);
    }

    try {
      removeAllNecessary(exec);
    } catch (IOException ex) {
      LOGGER.log(Level.WARNING,
          "Exception while cleaning after job:{0}, with appId:{1}, some cleaning is probably needed {2}",
          new Object[]{exec.getJob().getName(), exec.getAppId(), ex.getMessage()});
    }

    if (exec.getJob().getJobType().equals(JobType.FLINK)) {
      cleanCerts(exec);
    }
  }

  private Execution updateExecutionSTDPaths(String stdoutPath, String stderrPath, Execution exec) {
    exec = executionFacade.updateStdErrPath(exec, stderrPath);
    return executionFacade.updateStdOutPath(exec, stdoutPath);
  }

  public void removeAllNecessary(Execution exec) throws IOException {
    List<String> filesToRemove = exec.getFilesToRemove();
    String appDir = "hdfs://" + settings.getHdfsTmpCertDir() + "/" + exec.getHdfsUser() + File.separator + exec.
        getAppId();
    filesToRemove.add(appDir);
    DistributedFileSystemOps dfso = dfs.getDfsOps();
    try {
      for (String s : filesToRemove) {
        Path path = new Path(s);
        String scheme = path.toUri().getScheme();
        if (scheme != null && (scheme.equals(dfso.getFilesystem().getScheme()) || scheme.equals(dfso.getFilesystem().
            getAlternativeScheme())) && dfso.getFilesystem().exists(path))  {
          dfso.getFilesystem().delete(new Path(s), true);
        } else {
          org.apache.commons.io.FileUtils.deleteQuietly(new File(s));
        }
      }
    } finally {
      dfs.closeDfsClient(dfso);
    }
  }

  private void cleanCerts(Execution exec) {
    //Remove local files required for the job (Kafka certs etc.)
    //Search for other jobs using Kafka in the same project. If any active
    //ones are found

    Collection<ProjectServices> projectServices = exec.getJob().getProject().getProjectServicesCollection();
    Iterator<ProjectServices> iter = projectServices.iterator();
    boolean removeKafkaCerts = true;
    while (iter.hasNext()) {
      ProjectServices projectService = iter.next();
      //If the project is of type KAFKA
      if (projectService.getProjectServicesPK().getService() == ProjectServiceEnum.KAFKA) {
        List<Execution> execs = executionFacade.findByProjectAndType(exec.getJob().getProject(), JobType.FLINK);
        if (execs != null) {
          execs.addAll(executionFacade.findByProjectAndType(exec.getJob().getProject(), JobType.SPARK));
        }
        //Find if this project has running jobs
        if (execs != null && !execs.isEmpty()) {
          for (Execution exe : execs) {
            if (!exe.getState().isFinalState()) {
              removeKafkaCerts = false;
              break;
            }
          }
        }
      }
    }
    if (removeKafkaCerts) {
      String k_certName = exec.getHdfsUser() + "__kstore.jks";
      String t_certName = exec.getHdfsUser() + "__tstore.jks";
      File k_cert = new File(settings.getHopsworksDomainDir() + "/domain1/config/" + k_certName);
      File t_cert = new File(settings.getHopsworksDomainDir() + "/domain1/config/" + t_certName);
      if (k_cert.exists()) {
        k_cert.delete();
      }
      if (t_cert.exists()) {
        t_cert.delete();
      }
    }
  }
}
