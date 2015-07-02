package se.kth.bbc.jobs.adam;

import java.io.IOException;
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

  private final AdamArgumentDTO[] invocationArguments;
  private final AdamOptionDTO[] invocationOptions;

  public AdamJob(JobHistoryFacade facade, YarnRunner runner, FileOperations fops,
          AdamArgumentDTO[] invocationArguments,
          AdamOptionDTO[] invocationOptions) {
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
    updateHistory(null, getFinalState(), duration, null, null, null, null, null,
            null);
  }

  /**
   * For all the output files that were created, create an Inode for them and
   * create entries in the DB.
   */
  private void makeOutputAvailable() {
    for (AdamArgumentDTO arg : invocationArguments) {
      if (arg.isOutputPath() && !(arg.getValue() == null || arg.getValue().
              isEmpty())) {
        try {
          if (getFileOperations().exists(arg.getValue())) {
            getJobHistoryFacade().persist(new JobOutputFile(new JobOutputFilePK(
                    getHistory().getId(), Utils.
                    getFileName(arg.getValue())), arg.getValue()));
          }
        } catch (IOException e) {
          logger.log(Level.SEVERE, "Failed to create Inodes for HDFS path "
                  + arg.getValue() + ".", e);
        }
      }
    }

    for (AdamOptionDTO opt : invocationOptions) {
      if (opt.isOutputPath() && opt.getValue() != null && !opt.getValue().
              isEmpty()) {
        try {
          if (getFileOperations().exists(opt.getValue())) {
            getJobHistoryFacade().persist(new JobOutputFile(new JobOutputFilePK(
                    getHistory().getId(), Utils.
                    getFileName(opt.getValue())), opt.getValue()));
          }
        } catch (IOException e) {
          logger.log(Level.SEVERE, "Failed to create Inodes for HDFS path "
                  + opt.getValue() + ".", e);
        }
      }
    }
  }

}
