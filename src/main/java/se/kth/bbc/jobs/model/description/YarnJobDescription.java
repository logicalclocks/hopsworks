package se.kth.bbc.jobs.model.description;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import se.kth.bbc.jobs.yarn.YarnJobConfiguration;
import se.kth.bbc.project.Project;
import se.kth.hopsworks.user.model.Users;

/**
 *
 * @author stig
 */
@Entity
@DiscriminatorValue("YARN")
@NamedQueries({
  @NamedQuery(name = "YarnJobDescription.findAll",
          query = "SELECT j FROM YarnJobDescription j"),
  @NamedQuery(name = "YarnJobDescription.findByProject",
          query
          = "SELECT j FROM YarnJobDescription j WHERE j.project = :project ORDER BY j.creationTime DESC")})
public class YarnJobDescription extends JobDescription<YarnJobConfiguration> {

  public YarnJobDescription(){
    this(null, null, null, null);
  }
  
  public YarnJobDescription(YarnJobConfiguration config){
    this(config, null, null, null);
  }
  
  public YarnJobDescription(YarnJobConfiguration config, Project project,
          Users creator, String jobname) {
    super(config, project, creator, jobname);
  }
  
  @Override
  public String toString(){
    return "Yarn"+super.toString();
  }
}
