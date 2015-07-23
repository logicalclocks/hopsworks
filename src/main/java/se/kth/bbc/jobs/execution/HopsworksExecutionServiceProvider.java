package se.kth.bbc.jobs.execution;

import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.jobs.jobhistory.ExecutionFacade;
import se.kth.bbc.jobs.jobhistory.JobOutputFileFacade;

/**
 * Provides container-managed service providers as Facades through its methods.
 * @author stig
 */
public interface HopsworksExecutionServiceProvider {
  
  public ExecutionFacade getExecutionFacade();
  public JobOutputFileFacade getJobOutputFileFacade();
  public FileOperations getFileOperations();
}
