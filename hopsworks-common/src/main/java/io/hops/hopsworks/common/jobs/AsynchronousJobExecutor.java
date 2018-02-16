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

package io.hops.hopsworks.common.jobs;

import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import io.hops.hopsworks.common.jobs.execution.HopsJob;
import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;
import io.hops.hopsworks.common.dao.jobs.JobsHistoryFacade;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.jobs.yarn.YarnExecutionFinalizer;
import java.io.IOException;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import io.hops.hopsworks.common.security.BaseHadoopClientsService;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.yarn.YarnClientService;

/**
 * Utility class for executing a HopsJob asynchronously. Passing the Hopsjob to
 * the method startExecution() will start the HopsJob asynchronously. The
 * HobsJob is supposed to take care of all aspects of execution, such as
 * creating a JobHistory object or processing output.
 */
@Stateless
@LocalBean
public class AsynchronousJobExecutor {

  @EJB
  private ExecutionFacade executionFacade;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private InodeFacade inodeFacade;
  @EJB
  private JobsHistoryFacade jhf;
  @EJB
  private CertsFacade userCerts;
  @EJB
  private Settings settings;
  @EJB
  private YarnExecutionFinalizer yarnExecutionFinalizer;
  @EJB
  private YarnClientService yarnClientService;
  @EJB
  private CertificateMaterializer certificateMaterializer;
  @EJB
  private BaseHadoopClientsService baseHadoopClientsService;

  @Asynchronous
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void startExecution(HopsJob job) {
    job.execute();
  }

  public void stopExecution(HopsJob job, String appid) {
    job.stop(appid);
  }

  public ExecutionFacade getExecutionFacade() {
    return executionFacade;
  }

  public YarnExecutionFinalizer getYarnExecutionFinalizer(){
    return yarnExecutionFinalizer;
  }
  
  public DistributedFsService getFsService() {
    return dfs;
  }

  public InodeFacade getInodeFacade() {
    return inodeFacade;
  }
  
  public YarnClientService getYarnClientService() {
    return yarnClientService;
  }
  
  public DistributedFileSystemOps getFileOperations(String hdfsUser) throws
          IOException {
    return dfs.getDfsOps(hdfsUser);
  }

  public JobsHistoryFacade getJobsHistoryFacade() {
    return jhf;
  }

  public CertsFacade getUserCerts() {
    return userCerts;
  }

  public Settings getSettings() {
    return settings;
  }

  public CertificateMaterializer getCertificateMaterializer() {
    return certificateMaterializer;
  }
  
  public BaseHadoopClientsService getBaseHadoopClientsService() {
    return baseHadoopClientsService;
  }
}
