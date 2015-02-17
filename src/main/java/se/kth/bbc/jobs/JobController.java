package se.kth.bbc.jobs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
  private FileSelectionController fileSelector;

  /**
   * Method called after the main execution file has been uploaded to the
   * server.
   */
  protected abstract void registerMainFile(String filename,
          Map<String, String> attributes);

  /**
   * Method called after an extra file (e.g. input) has been uploaded to the
   * server.
   */
  protected abstract void registerExtraFile(String filename,
          Map<String, String> attributes);

  public final void uploadMainFile(FileUploadEvent event,
          Map<String, String> attributes) {
    files.clear();
    variables.clear();
    try {
      uploadFile(KEY_MAIN_FILE, event);
      MessagesController.addInfoMessage("Success.", getUserMessage(
              JobControllerEvent.MAIN_UPLOAD_SUCCESS, event.getFile().
              getFileName()));
      registerMainFile(event.getFile().getFileName(), attributes);
    } catch (Exception ex) {
      logger.log(Level.SEVERE, getLogMessage(
              JobControllerEvent.MAIN_UPLOAD_FAILURE, event.getFile().
              getFileName()), ex);
      MessagesController.addErrorMessage(getUserMessage(
              JobControllerEvent.MAIN_UPLOAD_FAILURE, event.getFile().
              getFileName()));
    }
  }

  public final void selectMainFile(String path, Map<String, String> attributes) {
    if (!path.startsWith("hdfs:")) {
      path = "hdfs://" + path;
    }

    if (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }
    System.out.println("Called select main file: " + path);
    files.clear();
    variables.clear();
    files.put(KEY_MAIN_FILE, path);
    registerMainFile(Utils.getFileName(path), attributes);
  }

  public final void uploadExtraFile(FileUploadEvent event,
          Map<String, String> attributes) {
    try {
      uploadFile(event.getFile().getFileName(), event);
      MessagesController.addInfoMessage("Success.", getUserMessage(
              JobControllerEvent.EXTRA_FILE_SUCCESS, event.getFile().
              getFileName()));
      registerExtraFile(event.getFile().getFileName(), attributes);
    } catch (Exception e) {
      logger.log(Level.SEVERE, getLogMessage(
              JobControllerEvent.EXTRA_FILE_FAILURE, event.getFile().
              getFileName()), e);
      MessagesController.addErrorMessage(getUserMessage(
              JobControllerEvent.EXTRA_FILE_FAILURE, event.getFile().
              getFileName()));
    }
  }

  public final void selectExtraFile(String path, Map<String, String> attributes) {
    if (!path.startsWith("hdfs:")) {
      path = "hdfs://" + path;
    }
    if (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }
    System.out.println("Called select extra file: " + path);
    files.put(Utils.getFileName(path), path);
    registerExtraFile(Utils.getFileName(path), attributes);
  }

  private void uploadFile(String key, FileUploadEvent event) throws Exception {
    if (basePath == null) {
      throw new IllegalStateException("Basepath has not been set!");
    }
    UploadedFile file = event.getFile();
    String uploadPath = basePath + file.getFileName();
    writeUploadedFile(file, uploadPath);
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
      case FILE_DOWNLOAD_FAILURE:
        return "An error occurred while trying to download file.";
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
      case FILE_DOWNLOAD_FAILURE:
        return "Failed to download file from path " + extraInfo + ".";
      default:
        throw new IllegalStateException("Enum value unaccounted for!");
    }
  }

  public final void setBasePath(String basepath) throws IOException {
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

  public final String getBasePath() {
    return basePath;
  }

  private void checkIfHistorySet() {
    if (history == null) {
      throw new IllegalStateException("JobHistoryFacade has not been set.");
    }
  }

  private void checkIfFopsSet() {
    if (fops == null) {
      throw new IllegalStateException("FileOperations bean has not been set.");
    }
  }

  private void checkIfFileSelectorSet() {
    if (fileSelector == null) {
      throw new IllegalStateException(
              "FileSelectionController has not been set.");
    }
  }

  protected final void setJobHistoryFacade(JobHistoryFacade facade) {
    this.history = facade;
  }

  protected final void setFileOperations(FileOperations fops) {
    this.fops = fops;
  }

  protected final void setFileSelector(FileSelectionController fileSelector) {
    this.fileSelector = fileSelector;
  }

  public final void setJobId(Long jobId) {
    this.jobhistoryid = jobId;
  }

  public final Long getJobId() {
    return jobhistoryid;
  }

  public final JobHistory getSelectedJob() {
    checkIfHistorySet();
    if (jobhistoryid == null) {
      return null;
    } else {
      return history.findById(jobhistoryid);
    }
  }

  public final void setSelectedJob(Long id) {
    this.jobhistoryid = id;
  }

  public final void setSelectedJob(JobHistory job) {
    this.jobhistoryid = job.getId();
  }

  public final boolean isJobSelected() {
    return jobhistoryid != null;
  }

  public final boolean isSelectedJobRunning() {
    if (jobhistoryid == null) {
      return false;
    } else {
      return !jobHasFinishedState();
    }
  }

  public final boolean isSelectedJobHasFinished() {
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

  protected final void putVariable(String key, String value) {
    variables.put(key, value);
  }

  protected final String getVariable(String key) {
    return variables.get(key);
  }

  protected final boolean variablesContainKey(String key) {
    return variables.containsKey(key);
  }

  protected final String getFilePath(String key) {
    return files.get(key);
  }

  protected final String getMainFilePath() {
    return files.get(KEY_MAIN_FILE);
  }

  protected final void updateMainFilePath(String newPath) {
    files.put(KEY_MAIN_FILE, newPath);
  }

  protected final Map<String, String> getFiles() {
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
      logger.log(Level.SEVERE, getLogMessage(
              JobControllerEvent.STDOUT_DOWNLOAD_FAILURE, stdoutPath), ex);
      MessagesController.addErrorMessage(MessagesController.ERROR,
              getUserMessage(JobControllerEvent.STDOUT_DOWNLOAD_FAILURE,
                      stdoutPath));
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
      logger.log(Level.SEVERE, getLogMessage(
              JobControllerEvent.STDERR_DOWNLOAD_FAILURE, stderrPath), ex);
      MessagesController.addErrorMessage(MessagesController.ERROR,
              getUserMessage(JobControllerEvent.STDERR_DOWNLOAD_FAILURE,
                      stderrPath));
    }
    return null;
  }

  protected final StreamedContent downloadFile(String path,
          String filename) throws IOException {
    checkIfFopsSet();
    //TODO: check if need to convert to try-with-resources or if this breaks streamedcontent
    InputStream is = fops.getInputStream(path);
    StreamedContent sc = new DefaultStreamedContent(is, Utils.getMimeType(
            filename),
            filename);
    logger.log(Level.FINE, "File was downloaded from HDFS path: {0}",
            path);
    return sc;
  }

  public StreamedContent downloadFile(String path) {
    try {
      return downloadFile(Utils.getDirectoryPart(path), Utils.getFileName(path));
    } catch (IOException ex) {
      logger.log(Level.SEVERE, getLogMessage(
              JobControllerEvent.FILE_DOWNLOAD_FAILURE, path), ex);
      MessagesController.addErrorMessage(MessagesController.ERROR,
              getUserMessage(JobControllerEvent.FILE_DOWNLOAD_FAILURE, path));
      return null;
    }
  }

  /**
   * Get a map containing all the extra files uploaded so far. The returned
   * map is NOT backed by the original map, so changes in either will not
   * reflect
   * in the other.
   * <p>
   * @return
   */
  protected final Map<String, String> getExtraFiles() {
    Map<String, String> allFiles = new HashMap(files);
    allFiles.remove(KEY_MAIN_FILE);
    return allFiles;
  }

  public final boolean isDir(String path) {
    return fops.isDir(path);
  }

  /**
   * Write an uploaded file to a specific path. This method is functionally
   * equivalent to UploadedFile.write(String path), except that that one does
   * not work in Glassfish 4. Should file a report on that somewhere.
   * <p>
   * @param file The file to write.
   * @param destination The destination, including filename, to which the file
   * should be written.
   * @throws IOException If writing fails.
   */
  private static void writeUploadedFile(UploadedFile file, String destination)
          throws IOException {
    File dest = new File(destination);
    try (
            InputStream stream = file.getInputstream();
            OutputStream out = new FileOutputStream(dest)) {

      byte[] buffer = new byte[1024];
      int bytesRead;
      while ((bytesRead = stream.read(buffer)) != -1) {
        out.write(buffer, 0, bytesRead);
      }
    } catch (IOException e) {
      throw new IOException("Failed to write uploaded file to path "
              + destination, e);
    }
  }

  /**
   *
   * @param isMainUpload
   * @param args A series of key-value arguments.
   */
  public final void prepareFileSelector(boolean isMainUpload, String attrs) {
    checkIfFileSelectorSet();
    Map<String, String> reqAttrs = new HashMap<>();
    if (attrs != null && !attrs.isEmpty()) {
      String[] args = attrs.trim().split(" ");
      if (args.length % 2 != 0) {
        throw new IllegalArgumentException(
                "prepareFileSelector should be called with pairs of arguments.");
      }
      //Get parameters
      for (int i = 0; i < args.length; i += 2) {
        reqAttrs.put(args[i], args[i + 1]);
      }
    }
    //Setup file selector
    fileSelector.init(this, isMainUpload, reqAttrs);
  }
}
