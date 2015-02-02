package se.kth.bbc.jobs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.UploadedFile;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.jobs.jobhistory.JobHistory;
import se.kth.bbc.jobs.jobhistory.JobHistoryFacade;
import se.kth.bbc.jobs.jobhistory.JobState;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.lims.Utils;

/**
 *
 * @author stig
 */
public abstract class JobController implements Serializable {

  private static final Logger logger = Logger.getLogger(JobController.class.
          getName());

  private static final String KEY_MAIN_FILE = "MAIN_FILE";

  private final Map<String, String> files = new HashMap<>();
  private final Map<String, String> variables = new HashMap<>();

  private String basePath = null;

  //Used to track job that was last executed or selected
  private Long jobhistoryid;

  private JobHistoryFacade history = null;
  private FileOperations fops;

  /**
   * Method called after the main execution file has been uploaded to the
   * server.
   */
  protected abstract void afterUploadMainFile(FileUploadEvent event);

  /**
   * Method called after an extra file (e.g. input) has been uploaded to the
   * server.
   */
  protected abstract void afterUploadExtraFile(FileUploadEvent event);

  /**
   * Get the channel to subscribe to to receive Primefaces Push updates.
   * @return 
   */
  public abstract String getPushChannel();
  
  public void uploadMainFile(FileUploadEvent event) {
    files.clear();
    variables.clear();
    try {
      uploadFile(KEY_MAIN_FILE, event);
      MessagesController.addInfoMessage("Success.", getUserMessage(
              JobControllerEvent.MAIN_UPLOAD_SUCCESS, event.getFile().
              getFileName()));
      afterUploadMainFile(event);
    } catch (Exception ex) {
      logger.log(Level.SEVERE, getLogMessage(
              JobControllerEvent.MAIN_UPLOAD_FAILURE, event.getFile().
              getFileName()), ex);
      MessagesController.addErrorMessage(getUserMessage(
              JobControllerEvent.MAIN_UPLOAD_FAILURE, event.getFile().
              getFileName()));
    }
  }

  public void uploadExtraFile(FileUploadEvent event) {
    try {
      uploadFile(event.getFile().getFileName(), event);
      MessagesController.addInfoMessage("Success.", getUserMessage(
              JobControllerEvent.EXTRA_FILE_SUCCESS, event.getFile().
              getFileName()));
      afterUploadExtraFile(event);
    } catch (Exception e) {
      logger.log(Level.SEVERE, getLogMessage(
              JobControllerEvent.EXTRA_FILE_FAILURE, event.getFile().
              getFileName()), e);
      MessagesController.addErrorMessage(getUserMessage(
              JobControllerEvent.EXTRA_FILE_FAILURE, event.getFile().
              getFileName()));
    }
  }

  private void uploadFile(String key, FileUploadEvent event) throws Exception {
    if (basePath == null) {
      throw new IllegalStateException("Basepath has not been set!");
    }
    UploadedFile file = event.getFile();
    String uploadPath = basePath + file.getFileName();
    file.write(uploadPath);
    files.put(key, uploadPath);
  }

  /**
   * Provide a message to display to the user on the occurrence of the given
   * event. It is recommended to implement the method with a switch statement,
   * defaulting to the superclass method.
   * <p>
   * @param event The event that occurred.
   * @param extraInfo Single string containing extra info about the event, such
   * as a filename.
   * @return
   */
  protected String getUserMessage(JobControllerEvent event, String extraInfo) {
    switch (event) {
      case MAIN_UPLOAD_FAILURE:
        return "Failed to upload file " + extraInfo + ".";
      case MAIN_UPLOAD_SUCCESS:
        return "File " + extraInfo + " successfully uploaded.";
      case EXTRA_FILE_FAILURE:
        return "Failed to upload file " + extraInfo + ".";
      case EXTRA_FILE_SUCCESS:
        return "File " + extraInfo + " successfully uploaded.";
      case STDOUT_DOWNLOAD_FAILURE:
        return "An error occurred while trying to download stdout.";
      case STDERR_DOWNLOAD_FAILURE:
        return "An error occurred while trying to download stderr.";
      default:
        throw new IllegalStateException("Enum value unaccounted for!");
    }
  }

  /**
   * Provide a message to log on the occurrence of the given event. It is
   * recommended to implement the method with a switch statement, defaulting to
   * the superclass method.
   * <p>
   * @param event
   * @return
   */
  protected String getLogMessage(JobControllerEvent event, String extraInfo) {
    switch (event) {
      case MAIN_UPLOAD_FAILURE:
        return "Failed to upload file " + extraInfo + ".";
      case MAIN_UPLOAD_SUCCESS:
        return ""; //Will not be called
      case EXTRA_FILE_FAILURE:
        return "Failed to upload file " + extraInfo + ".";
      case EXTRA_FILE_SUCCESS:
        return ""; //Will not be called
      case STDOUT_DOWNLOAD_FAILURE:
        return "Failed to download stdout from path " + extraInfo + ".";
      case STDERR_DOWNLOAD_FAILURE:
        return "Failed to download stderr from path " + extraInfo + ".";
      default:
        throw new IllegalStateException("Enum value unaccounted for!");
    }
  }

  public void setBasePath(String basepath) throws IOException {
    if (basepath.endsWith(File.separator)) {
      basePath = basepath;
    } else {
      basePath = basepath + File.separator;
    }

    Path p = Paths.get(basePath);
    boolean success = p.toFile().mkdirs();
    if (!success && !p.toFile().exists()) {
      throw new IOException(
              "Failed to create staging folder for uploading files.");
    }
  }

  private void checkIfHistorySet() {
    if (history == null) {
      throw new IllegalStateException("JobHistoryFacade has not been set.");
    }
  }
  
  private void checkIfFopsSet(){
    if(fops == null){
      throw new IllegalStateException("FileOperations bean has not been set.");
    }
  }

  public void setJobHistoryFacade(JobHistoryFacade facade) {
    this.history = facade;
  }
  
  public void setFileOperations(FileOperations fops){
    this.fops = fops;
  }

  public void setJobId(Long jobId) {
    this.jobhistoryid = jobId;
  }

  public Long getJobId() {
    return jobhistoryid;
  }

  public JobHistory getSelectedJob() {
    checkIfHistorySet();
    if (jobhistoryid == null) {
      return null;
    } else {
      return history.findById(jobhistoryid);
    }
  }

  public boolean isJobSelected() {
    return jobhistoryid != null;
  }

  public boolean isSelectedJobRunning() {
    if (jobhistoryid == null) {
      return false;
    } else {
      return !jobHasFinishedState();
    }
  }

  public boolean isSelectedJobHasFinished() {
    if (jobhistoryid == null) {
      return false;
    } else {
      return jobHasFinishedState();
    }
  }

  private boolean jobHasFinishedState() {
    checkIfHistorySet();
    JobState state = history.getState(jobhistoryid);
    if (state == null) {
      //should never happen
      return true;
    }
    return state.isFinalState();
  }

  protected void putVariable(String key, String value) {
    variables.put(key, value);
  }
  
  protected String getVariable(String key){
    return variables.get(key);
  }

  protected boolean variablesContainKey(String key) {
    return variables.containsKey(key);
  }

  protected String getFilePath(String key) {
    return files.get(key);
  }
  
  protected String getMainFilePath(){
    return files.get(KEY_MAIN_FILE);
  }
  
  protected Map<String,String> getFiles(){
    return files;
  }

  //TODO: move download methods to JobHistoryController?
  public StreamedContent downloadStdout() {
    JobHistory jh = getSelectedJob();
    if (jh == null) {
      return null;
    }
    String stdoutPath = jh.getStdoutPath();
    try {
      String filename = "stdout.log";
      return downloadFile(stdoutPath, filename);
    } catch (IOException ex) {
      logger.log(Level.SEVERE, getLogMessage(JobControllerEvent.STDOUT_DOWNLOAD_FAILURE,stdoutPath), ex);
      MessagesController.addErrorMessage(MessagesController.ERROR,
              getUserMessage(JobControllerEvent.STDOUT_DOWNLOAD_FAILURE, stdoutPath));
    }
    return null;
  }

  public StreamedContent downloadStderr() {
    JobHistory jh = getSelectedJob();
    if (jh == null) {
      return null;
    }
    String stderrPath = jh.getStderrPath();
    String filename = "stderr.log";
    try {
      return downloadFile(stderrPath, filename);
    } catch (IOException ex) {
      logger.log(Level.SEVERE, getLogMessage(JobControllerEvent.STDERR_DOWNLOAD_FAILURE,stderrPath), ex);
      MessagesController.addErrorMessage(MessagesController.ERROR,
              getUserMessage(JobControllerEvent.STDERR_DOWNLOAD_FAILURE, stderrPath));
    }
    return null;
  }

  protected final StreamedContent downloadFile(String path,
          String filename) throws IOException {
    checkIfFopsSet();
    InputStream is = fops.getInputStream(path);
    StreamedContent sc = new DefaultStreamedContent(is, Utils.getMimeType(
            filename),
            filename);
    logger.log(Level.INFO, "File was downloaded from HDFS path: {0}",
            path);
    return sc;
  }
  
  /**
   * Get a map containing all the extra files uploaded so far. The returned
   * map is NOT backed by the original map, so changes in either will not reflect
   * in the other.
   * @return 
   */
  protected final Map<String,String> getExtraFiles(){
    Map<String,String> allFiles = new HashMap(files);
    allFiles.remove(KEY_MAIN_FILE);
    return allFiles;
  }
}
