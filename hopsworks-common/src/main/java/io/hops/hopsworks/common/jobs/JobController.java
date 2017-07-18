package io.hops.hopsworks.common.jobs;

import io.hops.hopsworks.common.dao.jobs.description.JobDescription;
import io.hops.hopsworks.common.dao.jobs.description.JobDescriptionFacade;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.jobs.configuration.JobConfiguration;

@Stateless
public class JobController {

  @EJB
  private JobDescriptionFacade jdFacade;
  @EJB
  private JobScheduler scheduler;

  private static final Logger logger = Logger.getLogger(JobController.class.
          getName());

  public JobDescription createJob(Users user, Project project,
          JobConfiguration config) {
    JobDescription created = this.jdFacade.create(user, project, config);
    if (config.getSchedule() != null) {
      scheduler.scheduleJobPeriodic(created);
    }
    return created;
  }

  public boolean scheduleJob(int jobId) {
    boolean status = false;
    JobDescription job = this.jdFacade.findById(jobId);
    if (job != null) {
      scheduler.scheduleJobPeriodic(job);
      status = true;
    }
    return status;
  }

}
