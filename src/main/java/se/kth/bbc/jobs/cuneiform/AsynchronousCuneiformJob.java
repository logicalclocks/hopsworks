package se.kth.bbc.jobs.cuneiform;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.jobs.CancellableJob;
import se.kth.bbc.jobs.RunningJobTracker;
import se.kth.bbc.jobs.jobhistory.JobHistory;
import se.kth.bbc.jobs.jobhistory.JobHistoryFacade;
import se.kth.bbc.jobs.jobhistory.JobOutputFile;
import se.kth.bbc.jobs.jobhistory.JobOutputFileFacade;
import se.kth.bbc.jobs.yarn.AsynchronousYarnApplication;
import se.kth.bbc.jobs.yarn.YarnRunner;

/**
 * TODO: REMOVE THIS CLASS. VERY BADLY DESIGNED, IMPLEMENT DECORATOR INSTEAD
 * <p>
 * @author stig
 */
@Stateless
public class AsynchronousCuneiformJob {

  private static final Logger logger = Logger.getLogger(
          AsynchronousYarnApplication.class.getName());

  //TODO: get from some kind of configuration
  private static final int MAX_STATE_POLL_RETRIES = 10;
  private static final int POLL_TIMEOUT_INTERVAL = 1; //in seconds

  @EJB
  private RunningJobTracker jobTracker;

  @EJB
  private JobHistoryFacade jobHistoryFacade;

  @EJB
  private FileOperations fops;

  @EJB
  private JobOutputFileFacade jobOutputFacade;

  /**
   * Takes care of the execution of the job represented by YarnRunner.
   * Registers the running job and then asynchronously runs and monitors it.
   * <p>
   * @param runner
   */
  @Asynchronous
  //Netbeans shows an error here, but unrightly so.
  public void handleExecution(Long id, YarnRunner runner, String stdOutFinalPath,
          String stdErrFinalPath, String summaryFile) throws
          IllegalStateException {
    if (!jobTracker.isJobRunning(id)) {
      //The Asynchronous annotation must be on a public method, so calling a private
      // asynchronous method does not work. This makes that registerJob() has to be called
      // from the calling entity. Hence, assert it has been called and throw an IllegalStateException if not.
      throw new IllegalStateException(
              "Attempting to run job without registering first.");
    }
    //Run the job
    long startTime = System.currentTimeMillis();
    runAndMonitor(id, runner);
    //Check how long it took.
    long endTime = System.currentTimeMillis();
    long duration = endTime - startTime;
    //Update info
    try {
      jobHistoryFacade.update(id,
              runner.getApplicationReport().getYarnApplicationState().toString(),
              duration);
    } catch (YarnException | IOException ex) {
      logger.log(Level.SEVERE, "Failed to get final Yarn application state", ex);
      jobHistoryFacade.update(id, JobHistory.STATE_FRAMEWORK_FAILURE);
    }
    //Copy stdout and stderr to hdfs.
    try {
      copyStdOutNErr(id, runner.getStdOutPath(), stdOutFinalPath, runner.
              getStdErrPath(), stdErrFinalPath);
    } catch (IOException e) {
      //TODO: how should we handle this? Update state? Create file? Put entry in db saying
      //std was not copied?
      logger.
              log(Level.SEVERE,
                      "Error while copying out log files for yarn job.", e);
    }
    try {
      if (runner.getApplicationState() == YarnApplicationState.FINISHED) {
        //i.e. job finished
        String resultsPath = runner.getLocalResourcesBasePath() + "/"
                + summaryFile;
        parseAndHandleJsonOutput(id, resultsPath);
      }
    } catch (IOException | JSONException e) {
      logger.
              log(Level.SEVERE,
                      "Error while copying over result files. Aborting", e);
    } catch (YarnException e) {
      logger.log(Level.INFO,
              "Could not retrieve application state to copy result files. Assuming failed.",
              e);
    }
    unregisterJob(id);
    removeTempFiles(runner.getStdOutPath());
  }

  private void runAndMonitor(Long id, YarnRunner runner) {
    //TODO: remove local files, write stdout, stderr, outputfiles,...
    try {
      runner.startAppMaster();
    } catch (YarnException | IOException ex) {
      logger.log(Level.SEVERE, "Failed to start app master.", ex);
      //TODO: probably doesn't make much sense, check what should happen. (In relation to state changing in handleExecution().)
      jobHistoryFacade.update(id, JobHistory.STATE_FRAMEWORK_FAILURE);
      return;
    }
    monitor(runner);
  }

  public boolean registerJob(Long id, CancellableJob job) {
    try {
      jobTracker.registerJob(id, job);
    } catch (IllegalStateException e) {
      logger.log(Level.SEVERE, "Failed to register job.", e);
      return false;
    }
    return true;
  }

  private boolean unregisterJob(Long id) {
    try {
      jobTracker.unregisterJob(id);
    } catch (IllegalStateException e) {
      logger.log(Level.SEVERE, "Failed to unregister job.", e);
      return false;
    }
    return true;
  }

  private void monitor(YarnRunner runner) {
    YarnApplicationState appState;
    int failures;
    try {
      appState = runner.getApplicationState();
      //count how many consecutive times the state could not be polled. Cancel if too much.
      failures = 0;
    } catch (YarnException | IOException ex) {
      logger.log(Level.SEVERE, null, ex);
      appState = null;
      failures = 1;
    }

    //Loop as long as the application is in a running/runnable state
    while (appState != YarnApplicationState.FAILED && appState
            != YarnApplicationState.FINISHED && appState
            != YarnApplicationState.KILLED && failures < MAX_STATE_POLL_RETRIES) {
      //wait to poll another time
      long startTime = System.currentTimeMillis();
      while ((System.currentTimeMillis() - startTime)
              < POLL_TIMEOUT_INTERVAL * 1000) {
        try {
          Thread.sleep(200);
        } catch (InterruptedException e) {
          //not much...
        }
      }

      try {
        appState = runner.getApplicationState();
        failures = 0;
      } catch (YarnException | IOException ex) {
        logger.log(Level.SEVERE, null, ex);
        failures++;
      }
    }

    if (failures >= MAX_STATE_POLL_RETRIES) {
      try {
        logger.log(Level.SEVERE,
                "Killing application because unable to poll for status.");
        runner.cancelJob();
      } catch (YarnException | IOException ex) {
        logger.log(Level.SEVERE,
                "Failed to cancel job after failing to poll for status.", ex);
      }
    }

  }

  private void copyStdOutNErr(Long id, String stdOutLocal,
          String stdOutFinalDestination, String stdErrLocal,
          String stdErrFinalDestination) throws
          IOException {
    if (stdOutFinalDestination != null && !stdOutFinalDestination.isEmpty()) {
      fops.copyToHDFSFromPath(stdOutLocal, stdOutFinalDestination, null);
      jobHistoryFacade.updateStdOutPath(id, stdOutFinalDestination);
    }
    if (stdErrFinalDestination != null && !stdErrFinalDestination.isEmpty()) {
      fops.copyToHDFSFromPath(stdErrLocal, stdErrFinalDestination, null);
      jobHistoryFacade.updateStdErrPath(id, stdErrFinalDestination);
    }
  }

  private void removeTempFiles(String outpath) {
    int lastslash = outpath.lastIndexOf("/");
    String folderpath = outpath.substring(0, lastslash);
    File tmpFolder = new File(folderpath);
    deleteFolder(tmpFolder);
  }

  private void deleteFolder(File folder) {
    for (File f : folder.listFiles()) {
      if (f.isDirectory()) {
        deleteFolder(f);
      } else {
        f.delete();
      }
    }
    folder.delete();
  }

  private void parseAndHandleJsonOutput(Long jobId, String hdfsPath) throws
          IOException, JSONException {
    String json = fops.cat(hdfsPath);
    JSONObject jobj = new JSONObject(json);
    JSONArray outputpaths = jobj.getJSONArray("output");
    for (int i = 0; i < outputpaths.length(); i++) {
      String outfile = outputpaths.getString(i);
      JobOutputFile file = new JobOutputFile(jobId, getFileName(outfile));
      file.setPath(outfile);
      jobOutputFacade.persist(file);
    }
  }

  //TODO: move this method to a Utils class (similar method is used elsewhere)
  private static String getFileName(String path) {
    int lastSlash = path.lastIndexOf("/");
    int startName = (lastSlash > -1) ? lastSlash + 1 : 0;
    return path.substring(startName);
  }

}
