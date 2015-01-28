package se.kth.bbc.jobs.cuneiform;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.jobs.HopsJob;
import se.kth.bbc.jobs.jobhistory.JobHistory;
import se.kth.bbc.jobs.jobhistory.JobHistoryFacade;
import se.kth.bbc.jobs.jobhistory.JobOutputFile;
import se.kth.bbc.jobs.jobhistory.JobState;
import se.kth.bbc.jobs.yarn.YarnJob;
import se.kth.bbc.jobs.yarn.YarnRunner;
import se.kth.bbc.lims.Utils;

/**
 * Takes care of the execution of a Cuneiform job: run job, update history
 * object, copy logs and fetch output.
 * @author stig
 */
public final class CuneiformJob extends YarnJob {

  private static final Logger logger = Logger.getLogger(CuneiformJob.class.
          getName());

  private String summaryPath;

  public CuneiformJob(JobHistoryFacade facade, FileOperations fops,
          YarnRunner runner) {
    super(facade,runner,fops);
  }

  public void setSummaryPath(String summaryPath) {
    this.summaryPath = summaryPath;
  }

  @Override
  public HopsJob getInstance(JobHistory jh) throws IllegalArgumentException {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  protected void runJobInternal() {
    //Update job history object first
    super.updateArgs();

    //Can only be called if this job has a valid id.
    long startTime = System.currentTimeMillis();
    //Try to start the application master.
    boolean proceed = super.startJob();
    // If success: monitor running job
    if (!proceed) {
      return;
    }
    proceed = super.monitor();
    //If ok: copy logs
    if (!proceed) {
      return;
    }
    super.copyLogs();
    //If the application finished normally: process its output
    try {
      if (super.appFinishedSuccessfully()) {
        processOutput();
      }
      //Update execution time and final state
      long endTime = System.currentTimeMillis();
      long duration = endTime - startTime;
      getJobHistoryFacade().update(getJobId(),
              JobState.getJobState(getRunner().getApplicationState()),
              duration);
    } catch (YarnException | IOException ex) {
      logger.log(Level.SEVERE, "Error while getting final state for job "
              + getJobId() + ". Assuming failed", ex);
      getJobHistoryFacade().update(getJobId(),
              JobState.FRAMEWORK_FAILURE);
    }
  }

  private void processOutput() {
    try {
      String resultsPath = getRunner().getLocalResourcesBasePath() + File.separator
              + summaryPath;
      String json = getFileOperations().cat(resultsPath);
      JSONObject jobj = new JSONObject(json);
      JSONArray outputpaths = jobj.getJSONArray("output");
      for (int i = 0; i < outputpaths.length(); i++) {
        String outfile = outputpaths.getString(i);
        JobOutputFile file = new JobOutputFile(getJobId(), Utils.getFileName(
                outfile));
        file.setPath(outfile);
        getJobHistoryFacade().persist(file);
      }
    } catch (IOException | JSONException e) {
      logger.log(Level.SEVERE,
              "Failed to copy output files after running Cuneiform job "
              + getJobId(), e);
    }
  }
}
