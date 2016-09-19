package se.kth.bbc.security.ua;

import java.io.Serializable;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import se.kth.bbc.jobs.jobhistory.ExecutionFacade;
import se.kth.bbc.jobs.model.description.JobDescription;
import se.kth.bbc.jobs.model.description.JobDescriptionFacade;

/**
 * AdminUI for administering yarn jobs.
 * <p>
 */
@ManagedBean
@ViewScoped
public class JobAdministration implements Serializable {

  private static final long serialVersionUID = 1L;

  @EJB
  private JobDescriptionFacade jobFacade;
  @EJB
  private ExecutionFacade exeFacade;

  private List<JobDescription> jobs;

  private List<JobDescription> filteredJobs;

  public List<JobDescription> getAllJobs() {
    this.jobs = jobFacade.findAll();
    return this.jobs;
  }

  public void setFilteredJobs(List<JobDescription> filteredJobs) {
    this.filteredJobs = filteredJobs;
  }

  public List<JobDescription> getFilteredJobs() {
    return filteredJobs;
  }
}
