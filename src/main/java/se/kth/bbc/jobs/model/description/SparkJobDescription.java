package se.kth.bbc.jobs.model.description;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import se.kth.bbc.jobs.spark.SparkJobConfiguration;
import se.kth.bbc.project.Project;
import se.kth.hopsworks.user.model.Users;

/**
 *
 * @author stig
 */
@Entity
@DiscriminatorValue("SPARK")
@NamedQueries({
  @NamedQuery(name = "SparkJobDescription.findAll",
          query = "SELECT j FROM SparkJobDescription j"),
  @NamedQuery(name = "SparkJobDescription.findByProject",
          query
          = "SELECT j FROM SparkJobDescription j WHERE j.project = :project ORDER BY j.creationTime DESC")})
public class SparkJobDescription extends JobDescription<SparkJobConfiguration> {

  public SparkJobDescription() {
    this(null, null, null, null);
  }

  public SparkJobDescription(SparkJobConfiguration config) {
    this(config, null, null, null);
  }

  public SparkJobDescription(SparkJobConfiguration config, Project project,
          Users creator, String jobname) {
    super(config, project, creator, jobname);
  }

  @Override
  public String toString() {
    return "Spark" + super.toString();
  }
}
