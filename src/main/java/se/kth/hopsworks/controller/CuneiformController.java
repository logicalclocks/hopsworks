package se.kth.hopsworks.controller;

import de.huberlin.wbi.cuneiform.core.semanticmodel.HasFailedException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.jobs.AsynchronousJobExecutor;
import se.kth.bbc.jobs.cuneiform.CuneiformJob;
import se.kth.bbc.jobs.cuneiform.model.WorkflowDTO;
import se.kth.bbc.jobs.jobhistory.JobHistory;
import se.kth.bbc.jobs.jobhistory.JobHistoryFacade;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.jobs.yarn.YarnRunner;
import se.kth.bbc.lims.Constants;
import se.kth.bbc.lims.Utils;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectFacade;

/**
 * Interaction point between frontend and backend. Upload, inspect Cuneiform
 * workflows.
 * <p>
 * @author stig
 */
@Stateless
public class CuneiformController {

  private Logger logger = Logger.getLogger(CuneiformController.class.getName());
  private static final String OUT_LOGS = "AppMaster.stdout";
  private static final String ERR_LOGS = "AppMaster.stderr";

  @EJB
  private FileOperations fops;
  @EJB
  private JobHistoryFacade history;
  @EJB
  private ProjectFacade projects;
  @EJB
  private AsynchronousJobExecutor submitter;
  @EJB
  private ActivityFacade activities;

  /**
   * Inspect the workflow at the given path under the projectname. The path
   * should be absolute. This method returns a WorkflowDTO with the contents
   * and input and output parameter.
   * <p>
   * @param path The project-relative path to the workflow file.
   * @return WorkflowDTO with (a.o.) the workflow parameters.
   * @throws java.io.IOException on failure of reading the workflow.
   * @throws de.huberlin.wbi.cuneiform.core.semanticmodel.HasFailedException
   * On inspection failure.
   * @throws IllegalArgumentException if the given projectId does not
   * correspond to a project.
   */
  public WorkflowDTO inspectWorkflow(String path) throws
          IOException, HasFailedException, IllegalArgumentException {

    if (!fops.exists(path)) {
      throw new IllegalArgumentException("No such file.");
    } else if (fops.isDir(path)) {
      throw new IllegalArgumentException("Specified path is a directory.");

    }
    // Get the workflow name.
    String wfName = Utils.getFileName(path);

    // Get the contents
    String txt = fops.cat(path);

    //Create the workflowDTO
    WorkflowDTO wf = new WorkflowDTO(wfName, txt);
    wf.inspect();
    return wf;
  }

  /**
   * Start the workflow *wf* with the given name, as the user with given
   * username in capacity of member of given project.
   * <p>
   * @param jobName
   * @param wf
   * @param user
   * @param projectId
   * @return The job history object for the started job.
   * @throws IOException
   */
  public JobHistory startWorkflow(String jobName, WorkflowDTO wf, String user,
          Integer projectId) throws IOException {
    // Set the job name if necessary.
    if (jobName == null || jobName.isEmpty()) {
      jobName = "Untitled Cuneiform job";
    }
    String wfLocation;
    try {
      wfLocation = prepWorkflowFile(wf);
    } catch (IOException e) {
      throw new IOException("Error while setting up workflow file.", e);
    }

    String resultName = "results";

    YarnRunner.Builder b = new YarnRunner.Builder(Constants.HIWAY_JAR_PATH,
            "Hiway.jar");
    b.amMainClass(
            "de.huberlin.wbi.hiway.am.cuneiform.CuneiformApplicationMaster");
    b.appName("Cuneiform " + jobName);
    b.addAmJarToLocalResources(false); // Weird way of hiway working

    String machineUser = Utils.getYarnUser();

    b.localResourcesBasePath("/hiway/"
            + YarnRunner.APPID_PLACEHOLDER);

    //construct AM arguments
    StringBuilder args = new StringBuilder("--workflow ");
    args.append(Utils.getFileName(wfLocation));
    args.append(" --appid ");
    args.append(YarnRunner.APPID_PLACEHOLDER);
    args.append(" --summary ");
    args.append(resultName);

    b.amArgs(args.toString());

    //Pass on workflow file
    b.addFilePathToBeCopied(wfLocation, true);
    b.stdOutPath(">(tee AppMaster.stdout <LOG_DIR>/"+OUT_LOGS+")");
    b.stdErrPath(">(tee AppMaster.stderr <LOG_DIR>/"+ERR_LOGS+" >&2)");
    b.logPathsRelativeToResourcesPath(false);

    b.addToAppMasterEnvironment("CLASSPATH", "/srv/hiway/lib/*:/srv/hiway/*");
    YarnRunner r;

    try {
      //Get the YarnRunner instance
      r = b.build();
    } catch (IOException ex) {
      logger.log(Level.SEVERE,
              "Unable to create temp directory for logs.",
              ex);
      throw new IOException("Unable to create temp directory for AM logs.", ex);
    }

    CuneiformJob job = new CuneiformJob(history, fops, r);
    job.setStdOutPath("/hiway/"+CuneiformJob.APPID_PLACEHOLDER+"/"+OUT_LOGS);
    job.setStdErrPath("/hiway/"+CuneiformJob.APPID_PLACEHOLDER+"/"+ERR_LOGS);
    Project project = projects.find(projectId);

    //TODO: include input and execution files
    JobHistory jh = job.requestJobId(jobName, user,
            project, JobType.CUNEIFORM);
    if (jh != null) {
      String stdOutFinalDestination = Utils.getHdfsRootPath(project.getName())
              + Constants.CUNEIFORM_DEFAULT_OUTPUT_PATH + jh.getId() + File.separator
              + "stdout.log";
      String stdErrFinalDestination = Utils.getHdfsRootPath(project.getName())
              + Constants.CUNEIFORM_DEFAULT_OUTPUT_PATH + jh.getId() + File.separator
              + "stderr.log";
      job.setStdOutFinalDestination(stdOutFinalDestination);
      job.setStdErrFinalDestination(stdErrFinalDestination);
      job.setSummaryPath(resultName);
      submitter.startExecution(job);
    } else {
      logger.log(Level.SEVERE,
              "Failed to persist JobHistory. Aborting execution.");
      throw new IOException("Failed to persist JobHistory.");
    }
    activities.persistActivity(ActivityFacade.RAN_JOB, project, user);
    return jh;
  }

  /**
   * Prepare the workflow file for running. Edits the workflow contents, but
   * does not alter anything else. Paths are assumed to be HDFS absolute paths.
   * It then writes the contents to a temporary workflow file in HDFS.
   * <p>
   * @param wf
   * @throws IOException
   * @return The path at which the temporary file was created.
   */
  private String prepWorkflowFile(WorkflowDTO wf) throws IOException {
    // Update the workflow contents
    wf.updateContentsFromVars();
    //actually write to workflow file
    Path p = Files.createTempFile(Utils.stripExtension(wf.getName()), ".cf");
    try(FileWriter t = new FileWriter(p.toFile(),false)){
      t.write(wf.getContents());
    }
    return p.toString();
  }

}
