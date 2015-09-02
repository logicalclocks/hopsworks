package se.kth.hopsworks.controller;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import se.kth.bbc.jobs.JobScheduler;
import se.kth.bbc.jobs.model.configuration.JobConfiguration;
import se.kth.bbc.jobs.model.description.JobDescription;
import se.kth.bbc.jobs.model.description.JobDescriptionFacade;
import se.kth.bbc.project.Project;
import se.kth.hopsworks.user.model.Users;

/**
 *
 * @author stig
 */
@Stateless
public class JobController {
  
  @EJB
  private JobDescriptionFacade jdFacade;
  @EJB
  private JobScheduler scheduler;
  
  public JobDescription createJob(Users user, Project project, JobConfiguration config){
    JobDescription created = jdFacade.create(user, project, config);
    if(config.getSchedule() != null){
      scheduler.scheduleJobPeriodic(created);
    }
    return created;
  }
}
