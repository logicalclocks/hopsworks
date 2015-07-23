package se.kth.bbc.jobs.model.description;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import se.kth.bbc.jobs.adam.AdamJobConfiguration;
import se.kth.bbc.project.Project;
import se.kth.hopsworks.user.model.Users;

/**
 *
 * @author stig
 */
@Entity
@DiscriminatorValue("ADAM")
@NamedQueries({
  @NamedQuery(name = "AdamJobDescription.findAll",
          query = "SELECT j FROM AdamJobDescription j"),
  @NamedQuery(name = "AdamJobDescription.findByProject",
          query
          = "SELECT j FROM AdamJobDescription j WHERE j.project = :project ORDER BY j.creationTime DESC")})
public class AdamJobDescription extends JobDescription<AdamJobConfiguration> {

  public AdamJobDescription() {
    this(null, null, null, null);
  }

  public AdamJobDescription(AdamJobConfiguration config) {
    this(config, null, null, null);
  }

  public AdamJobDescription(AdamJobConfiguration config, Project project,
          Users creator, String jobname) {
    super(config, project, creator, jobname);
  }

  @Override
  public String toString() {
    return "Adam" + super.toString();
  }
}
