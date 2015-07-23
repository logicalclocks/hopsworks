package se.kth.bbc.jobs.model.description;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.apache.hadoop.mapred.JobConfigurable;
import se.kth.bbc.jobs.cuneiform.model.CuneiformJobConfiguration;
import se.kth.bbc.project.Project;
import se.kth.hopsworks.user.model.Users;

/**
 *
 * @author stig
 */
@Entity
@DiscriminatorValue("CUNEIFORM")
@NamedQueries({
  @NamedQuery(name = "CuneiformJobDescription.findAll",
          query = "SELECT j FROM CuneiformJobDescription j"),
  @NamedQuery(name = "CuneiformJobDescription.findByProject",
          query
          = "SELECT j FROM CuneiformJobDescription j WHERE j.project = :project ORDER BY j.creationTime DESC")})
public class CuneiformJobDescription extends JobDescription<CuneiformJobConfiguration> {

  public CuneiformJobDescription() {
    this(null, null, null, null);
  }

  public CuneiformJobDescription(CuneiformJobConfiguration config) {
    this(config, null, null, null);
  }

  public CuneiformJobDescription(CuneiformJobConfiguration config,
          Project project,
          Users creator, String jobname) {
    super(config, project, creator, jobname);
  }

  @Override
  public String toString() {
    return "Cuneiform" + super.toString();
  }
}
