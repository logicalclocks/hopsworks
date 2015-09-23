package se.kth.hopsworks.controller;

import java.io.IOException;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import se.kth.bbc.jobs.jobhistory.Execution;
import se.kth.bbc.jobs.model.description.JobDescription;
import se.kth.hopsworks.user.model.Users;

/**
 * Takes care of booting the execution of a job.
 * <p/>
 * @author stig
 */
@Stateless
public class ExecutionController {

  //Controllers
  @EJB
  private CuneiformController cuneiformController;
  @EJB
  private SparkController sparkController;
  @EJB
  private AdamController adamController;

  public Execution start(JobDescription job, Users user) throws IOException {
    switch (job.getJobType()) {
      case CUNEIFORM:
        return cuneiformController.startWorkflow(job, user);
      case ADAM:
        return adamController.startJob(job, user);
      case SPARK:
        return sparkController.startJob(job, user);
      default:
        throw new IllegalArgumentException("Unsupported job type: " + job.
                getJobType());
    }
  }
}
