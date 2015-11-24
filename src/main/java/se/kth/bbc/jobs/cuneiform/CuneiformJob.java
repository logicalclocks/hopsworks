package se.kth.bbc.jobs.cuneiform;

import com.google.common.base.Strings;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import se.kth.bbc.jobs.AsynchronousJobExecutor;
import se.kth.bbc.jobs.cuneiform.model.CuneiformJobConfiguration;
import se.kth.bbc.jobs.cuneiform.model.WorkflowDTO;
import se.kth.bbc.jobs.model.description.JobDescription;
import se.kth.bbc.jobs.yarn.YarnJob;
import se.kth.bbc.jobs.yarn.YarnRunner;
import se.kth.bbc.lims.Utils;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.util.Settings;

/**
 * Takes care of the execution of a Cuneiform job: run job, update history
 * object, copy logs and fetch output.
 * <p/>
 * @author stig
 */
public final class CuneiformJob extends YarnJob {

  private static final Logger logger = Logger.getLogger(CuneiformJob.class.
          getName());
  public static final String APPID_PLACEHOLDER = "$APPID";
  private static final String APPID_REGEX = "\\$APPID";
  private static final String OUT_LOGS = "AppMaster.stdout";
  private static final String ERR_LOGS = "AppMaster.stderr";

  private String summaryPath;
  private String stdOutPath = "/hiway/" + CuneiformJob.APPID_PLACEHOLDER + "/"
          + OUT_LOGS;
  private String stdErrPath = "/hiway/" + CuneiformJob.APPID_PLACEHOLDER + "/"
          + ERR_LOGS;

  private Path tempFile;
  private String sparkDir;
    
  //Just for ease of use...
  private final CuneiformJobConfiguration config;

  /**
   *
   * @param job
   * @param services
   * @param user
   * @throws IllegalArgumentException If the given JobDescription does not
   * contain a CuneiformJobConfiguration object.
   */
  public CuneiformJob(JobDescription job,
          AsynchronousJobExecutor services, Users user, String hadoopDir, String sparkDir) {
    super(job, services, user, hadoopDir);
    if (!(job.getJobConfig() instanceof CuneiformJobConfiguration)) {
      throw new IllegalArgumentException(
              "The jobconfiguration in JobDescription must be of type CuneiformJobDescription. Received: "
              + job.getJobConfig().getClass());
    }
    this.config = (CuneiformJobConfiguration) job.getJobConfig(); //We can do this because of the type check earlier.
    this.sparkDir = sparkDir;
  }

  @Override
  protected void runJob() {
    //Can only be called if this job has a valid id.
    //Try to start the application master.
    boolean proceed = startApplicationMaster();
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
    if (appFinishedSuccessfully()) {
      processOutput();
    }
    //Update execution time and final state
    updateState(getFinalState());
  }

  private void processOutput() {
    try {
      String resultsPath = runner.getLocalResourcesBasePath()
              + File.separator
              + summaryPath;
      String json = services.getFileOperations().cat(resultsPath);
      JSONObject jobj = new JSONObject(json);
      JSONArray outputpaths = jobj.getJSONArray("output");
      for (int i = 0; i < outputpaths.length(); i++) {
        String outfile = outputpaths.getString(i);
        String destPath = "/" + Settings.DIR_ROOT + "/" + jobDescription.
                getProject().getName() + "/"
                + Settings.CUNEIFORM_DEFAULT_OUTPUT_PATH + getExecution().
                getId() + "/" + Utils.getFileName(outfile);
        services.getFileOperations().renameInHdfs(outfile, destPath);
        services.getJobOutputFileFacade().create(getExecution(), Utils.
                getFileName(outfile), destPath);
      }
    } catch (IOException | JSONException e) {
      logger.log(Level.SEVERE,
              "Failed to copy output files after running Cuneiform job "
              + getExecution().getId(), e);
    }
  }

  /**
   * Updates the Execution object with the actual paths of the logs.
   */
  @Override
  protected void copyLogs() {
    try {
      stdOutPath = stdOutPath.replaceAll(APPID_REGEX, getExecution().getAppId());
      services.getFileOperations().renameInHdfs(stdOutPath,
              getStdOutFinalDestination());
      stdErrPath = stdErrPath.replaceAll(APPID_REGEX, getExecution().getAppId());
      services.getFileOperations().renameInHdfs(stdErrPath,
              getStdErrFinalDestination());
      updateExecution(null, -1, getStdOutFinalDestination(),
              getStdErrFinalDestination(), null, null, null);
    } catch (IOException ex) {
      logger.log(Level.SEVERE, "Error while copying logs for job "
              + getExecution().getId() + ".", ex);
    }
  }

  @Override
  protected final boolean setupJob() {
    //Then: go about starting the job
    WorkflowDTO wf = config.getWf();
    if (Strings.isNullOrEmpty(config.getAppName())) {
      config.setAppName("Untitled Cuneiform job");
    }
    // Set the job name if necessary.
//    String wfLocation;
//    try {
//      wfLocation = prepWorkflowFile(wf);
//    } catch (IOException e) {
//      writeToLogs(new IOException("Error while setting up workflow file.", e));
//      return false;
//    }

    String resultName = "results";

    YarnRunner.Builder b = new YarnRunner.Builder(Settings.HIWAY_JAR_PATH,
            "hiway-core.jar");
    b.amMainClass(
            "de.huberlin.wbi.hiway.am.cuneiform.CuneiformApplicationMaster");
    b.addAmJarToLocalResources(false); 

    b.localResourcesBasePath("/hiway/"
            + YarnRunner.APPID_PLACEHOLDER);

    //construct AM arguments
    StringBuilder args = new StringBuilder("--workflow ");
//    args.append(Utils.getFileName(wfLocation));
    args.append(wf.getPath()).append(",true");
    args.append(" --appid ");
    args.append(YarnRunner.APPID_PLACEHOLDER);
    args.append(" --summary ");
    args.append(resultName);


    logger.log(Level.INFO, "Hiway YARN command string: {0}", args.toString());
    
    b.amArgs(args.toString());

    //Pass on workflow file
//    b.addFilePathToBeCopied(wfLocation, true);
    b.stdOutPath(OUT_LOGS);
    b.stdErrPath(ERR_LOGS);
    b.logPathsRelativeToResourcesPath(false);

    b.addToAppMasterEnvironment("CLASSPATH", "/home/glassfish/software/hiway/lib/*:/home/glassfish/software/hiway/*");

    //Set Yarn configuration
    b.setConfig(config);

    try {
      //Get the YarnRunner instance
      runner = b.build(hadoopDir, sparkDir);
    } catch (IOException ex) {
      logger.log(Level.SEVERE,
              "Unable to create temp directory for logs.",
              ex);
      writeToLogs(
              new IOException("Unable to create temp directory for AM logs.", ex));
      return false;
    }

    
//TODO: include input files
    String stdOutFinalDestination = 
        Utils.getHdfsRootPath(hadoopDir, jobDescription.getProject().getName())
            + Settings.CUNEIFORM_DEFAULT_OUTPUT_PATH + getExecution().getId()
            + File.separator
            + "stdout.log";
//    String stdErrFinalDestination = Utils.getHdfsRootPath(jobDescription.getProject().getName(), user.getUsername())
    String stdErrFinalDestination = 
                Utils.getHdfsRootPath(hadoopDir, jobDescription.getProject().getName())
            + Settings.CUNEIFORM_DEFAULT_OUTPUT_PATH + getExecution().getId()
            + File.separator
            + "stderr.log";
    setStdOutFinalDestination(stdOutFinalDestination);
    setStdErrFinalDestination(stdErrFinalDestination);
    this.summaryPath = resultName;
    return true;
  }

  /**
   * Prepare the workflow file for running. Edits the workflow contents, but
   * does not alter anything else. Paths are assumed to be HDFS absolute paths.
   * It then writes the contents to a temporary workflow file in HDFS.
   * <p/>
   * @param wf
   * @throws IOException
   * @return The path at which the temporary file was created.
   */
  private String prepWorkflowFile(WorkflowDTO wf) throws IOException {
    if (wf.areContentsEmpty()) {
      wf.setContents(services.getFileOperations().cat(wf.getPath()));
    }
    // Update the workflow contents
    wf.updateContentsFromVars();
    //actually write to workflow file
    Path p = Files.createTempFile(Utils.stripExtension(wf.getName()), ".cf");
    try (FileWriter t = new FileWriter(p.toFile(), false)) {
      t.write(wf.getContents());
    }
    return p.toString();
  }

  @Override
  protected void cleanup() {
    if (tempFile != null) {
      try {
        Files.deleteIfExists(tempFile);
      } catch (IOException ex) {
        Logger.getLogger(CuneiformJob.class.getName()).log(Level.WARNING,
                "Unable to remove created temp file.",
                ex);
        writeToLogs(new IOException("Unable to remove created temp file.", ex));
      }
    }
  }

}
