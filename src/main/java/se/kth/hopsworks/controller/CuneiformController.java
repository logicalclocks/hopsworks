package se.kth.hopsworks.controller;

import de.huberlin.wbi.cuneiform.core.semanticmodel.HasFailedException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.jobs.AsynchronousJobExecutor;
import se.kth.bbc.jobs.cuneiform.CuneiformJob;
import se.kth.bbc.jobs.cuneiform.model.WorkflowDTO;
import se.kth.bbc.jobs.jobhistory.Execution;
import se.kth.bbc.jobs.model.description.CuneiformJobDescription;
import se.kth.hopsworks.user.model.Users;

/**
 * Interaction point between frontend and backend. Upload, inspect Cuneiform
 * workflows.
 * <p>
 * @author stig
 */
@Stateless
public class CuneiformController {

  private Logger logger = Logger.getLogger(CuneiformController.class.getName());

  @EJB
  private FileOperations fops;
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
    } else if (!path.endsWith(".cf")) {
      throw new IllegalArgumentException(
              "Specified path does not point to .cf file.");
    }

    // Get the contents
    String txt = fops.cat(path);

    //Create the workflowDTO
    WorkflowDTO wf = new WorkflowDTO(path, txt);
    wf.inspect();
    return wf;
  }

  /**
   * Start the workflow *wf* with the given name, as the user with given
   * username.
   * <p>
   * @param job
   * @param user
   * @return The execution object for the started execution.
   * @throws IOException
   * @throws NullPointerException If the user or job are null.
   * @throws IllegalArgumentException If the job does not represent a Cuneiform
   * JobDescription.
   */
  public Execution startWorkflow(CuneiformJobDescription job, Users user) throws
          IOException,
          IllegalArgumentException, NullPointerException {
    //First: some parameter checking
    if (job == null) {
      throw new NullPointerException("Cannot execute a null job.");
    }
    if (user == null) {
      throw new NullPointerException("Cannot execute a job as a null user.");
    }

    //Then: create a CuneiformJob to run it
    CuneiformJob cfjob = new CuneiformJob(job, submitter, user);
    Execution jh = cfjob.requestExecutionId();
    if (jh != null) {
      submitter.startExecution(cfjob);
    } else {
      logger.log(Level.SEVERE,
              "Failed to persist JobHistory. Aborting execution.");
      throw new IOException("Failed to persist JobHistory.");
    }
    activities.persistActivity(ActivityFacade.RAN_JOB, job.getProject(), user.
            asUser());
    return jh;
  }

}
