package se.kth.bbc.jobs.adam;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.jobs.HopsJob;
import se.kth.bbc.jobs.jobhistory.JobHistory;
import se.kth.bbc.jobs.jobhistory.JobHistoryFacade;
import se.kth.bbc.jobs.jobhistory.JobOutputFile;
import se.kth.bbc.jobs.jobhistory.JobOutputFilePK;
import se.kth.bbc.jobs.yarn.YarnJob;
import se.kth.bbc.jobs.yarn.YarnRunner;
import se.kth.bbc.lims.Utils;

/**
 *
 * @author stig
 */
public class AdamJob extends YarnJob {

  private static final Logger logger = Logger.getLogger(AdamJob.class.getName());

  private final List<AdamInvocationArgument> invocationArguments;
  private final List<AdamInvocationOption> invocationOptions;

  public AdamJob(JobHistoryFacade facade, YarnRunner runner, FileOperations fops,
          List<AdamInvocationArgument> invocationArguments,
          List<AdamInvocationOption> invocationOptions) {
    super(facade, runner, fops);
    this.invocationArguments = invocationArguments;
    this.invocationOptions = invocationOptions;
  }

  @Override
  public HopsJob getInstance(JobHistory jh) throws IllegalArgumentException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  protected void runJobInternal() {
    //Update job history object
    super.updateArgs();

    //Keep track of time and start job
    long startTime = System.currentTimeMillis();
    //Try to start the AM
    boolean proceed = super.startJob();
    //If success: monitor running job
    if (!proceed) {
      return;
    }
    proceed = super.monitor();
    //If not ok: return
    if (!proceed) {
      return;
    }
    super.copyLogs();
    makeOutputAvailable();
    long endTime = System.currentTimeMillis();
    long duration = endTime - startTime;
    getJobHistoryFacade().update(getJobId(), getFinalState(), duration);
  }

  /**
   * For all the output files that were created, create an Inode for them and
   * create entries in the DB.
   */
  private void makeOutputAvailable() {
    for (AdamInvocationArgument aia : invocationArguments) {
      if (aia.getArg().isOutputPath() && aia.getArg().isRequired()) {
        try {
          boolean created = getFileOperations().createInodesIfNeeded(aia.
                  getValue());
          if (created) {
            getJobHistoryFacade().persist(new JobOutputFile(new JobOutputFilePK(
                    getJobId(), Utils.
                    getFileName(aia.getValue())), aia.getValue()));
          }
        } catch (IOException e) {
          logger.log(Level.SEVERE, "Failed to create Inodes for HDFS path "
                  + aia.getValue() + ".", e);
        }
      }
    }

    for (AdamInvocationOption aio : invocationOptions) {
      if (aio.getOpt().isOutputPath() && aio.getStringValue() != null && !aio.
              getStringValue().isEmpty()) {
        try {
          boolean created = getFileOperations().createInodesIfNeeded(aio.
                  getStringValue());
          if (created) {
            getJobHistoryFacade().persist(new JobOutputFile(new JobOutputFilePK(
                    getJobId(), Utils.
                    getFileName(aio.getStringValue())), aio.getStringValue()));
          }
        } catch (IOException e) {
          logger.log(Level.SEVERE, "Failed to create Inodes for HDFS path "
                  + aio.getStringValue() + ".", e);
        }
      }
    }
  }

}
