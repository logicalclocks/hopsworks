package se.kth.bbc.jobs;

import java.io.IOException;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.jobs.execution.HopsJob;
import se.kth.bbc.jobs.jobhistory.ExecutionFacade;
import se.kth.bbc.jobs.jobhistory.JobOutputFileFacade;
import se.kth.hopsworks.hdfs.fileoperations.DFSSingleton;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFileSystemOps;

/**
 * Utility class for executing a HopsJob asynchronously. Passing the Hopsjob to
 * the method startExecution() will start the HopsJob asynchronously. The
 * HobsJob is supposed to take care of all aspects of execution, such as
 * creating a JobHistory object or processing output.
 * <p/>
 * @author stig
 */
@Stateless
@LocalBean
public class AsynchronousJobExecutor {

  @EJB
  private ExecutionFacade executionFacade;
  @EJB
  private JobOutputFileFacade jobOutputFileFacade;
  @EJB
  private FileOperations fileOperations;
  @EJB
  private DFSSingleton dfs;

  @Asynchronous
  public void startExecution(HopsJob job) {
    job.execute();
  }

  public ExecutionFacade getExecutionFacade() {
    return executionFacade;
  }

  public JobOutputFileFacade getJobOutputFileFacade() {
    return jobOutputFileFacade;
  }

  public FileOperations getFileOperations() {
    return fileOperations;
  }

  public DistributedFileSystemOps getFileOperations(String hdfsUser) throws
          IOException {
    return dfs.getDfsOps(hdfsUser);
  }
}
