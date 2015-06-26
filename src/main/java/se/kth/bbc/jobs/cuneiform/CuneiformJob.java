package se.kth.bbc.jobs.cuneiform;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.jobs.HopsJob;
import se.kth.bbc.jobs.jobhistory.JobHistory;
import se.kth.bbc.jobs.jobhistory.JobHistoryFacade;
import se.kth.bbc.jobs.jobhistory.JobOutputFile;
import se.kth.bbc.jobs.yarn.YarnJob;
import se.kth.bbc.jobs.yarn.YarnRunner;
import se.kth.bbc.lims.Utils;

/**
 * Takes care of the execution of a Cuneiform job: run job, update history
 * object, copy logs and fetch output.
 * <p>
 * @author stig
 */
public final class CuneiformJob extends YarnJob {

  private static final Logger logger = Logger.getLogger(CuneiformJob.class.
          getName());
  public static final String APPID_PLACEHOLDER = "$APPID";
  private static final String APPID_REGEX = "\\$APPID";

  private String summaryPath;
  private String stdOutPath;
  private String stdErrPath;

  public CuneiformJob(JobHistoryFacade facade, FileOperations fops,
          YarnRunner runner) {
    super(facade, runner, fops);
  }

  public void setSummaryPath(String summaryPath) {
    this.summaryPath = summaryPath;
  }

  /**
   * Set the path where Hiway finally copies the output logs to. Because of
   * Hi-WAY specific handling of logs, this is needed.
   * <p>
   * @param stdOutPath
   */
  public void setStdOutPath(String stdOutPath) {
    this.stdOutPath = stdOutPath;
  }

  /**
   * Set the path where Hiway finally copies the error logs to. Because of
   * Hi-WAY specific handling of logs, this is needed.
   * <p>
   * @param stdErrPath
   */
  public void setStdErrPath(String stdErrPath) {
    this.stdErrPath = stdErrPath;
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
    copyLogs();
    //If the application finished normally: process its output
    if (super.appFinishedSuccessfully()) {
      processOutput();
    }
    //Update execution time and final state
    long endTime = System.currentTimeMillis();
    long duration = endTime - startTime;
    updateHistory(null, getFinalState(), duration, null, null, null, null, null,
            null);
  }

  private void processOutput() {
    try {
      String resultsPath = getRunner().getLocalResourcesBasePath()
              + File.separator
              + summaryPath;
      String json = getFileOperations().cat(resultsPath);
      JSONObject jobj = new JSONObject(json);
      JSONArray outputpaths = jobj.getJSONArray("output");
      for (int i = 0; i < outputpaths.length(); i++) {
        String outfile = outputpaths.getString(i);
        JobOutputFile file = new JobOutputFile(getHistory().getId(), Utils.
                getFileName(
                        outfile));
        file.setPath(outfile);
        getJobHistoryFacade().persist(file);
      }
    } catch (IOException | JSONException e) {
      logger.log(Level.SEVERE,
              "Failed to copy output files after running Cuneiform job "
              + getHistory().getId(), e);
    }
  }

  /**
   * Updates the JobHistory object with the actual paths of the logs.
   */
  @Override
  protected void copyLogs() {
    try {
      stdOutPath = stdOutPath.replaceAll(APPID_REGEX, getHistory().getAppId());
      getFileOperations().renameInHdfs(stdOutPath, getStdOutFinalDestination());
      stdErrPath = stdErrPath.replaceAll(APPID_REGEX, getHistory().getAppId());
      getFileOperations().renameInHdfs(stdErrPath, getStdErrFinalDestination());
      updateHistory(null, null, -1, null, getStdOutFinalDestination(),
              getStdErrFinalDestination(), null, null, null);
    } catch (IOException ex) {
      logger.log(Level.SEVERE, "Error while copying logs for job "
              + getHistory().getId() + ".", ex);
    }
  }

}
