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
import io.hops.hopsworks.common.dao.jobs.JobOutputFileFacade;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.jobs.yarn.YarnExecutionFinalizer;
import java.io.IOException;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.common.security.BaseHadoopClientsService;
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
  private JobOutputFileFacade jobOutputFileFacade;
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
  
  public JobOutputFileFacade getJobOutputFileFacade() {
    return jobOutputFileFacade;
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
