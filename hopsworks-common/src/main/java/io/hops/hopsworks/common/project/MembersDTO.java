package io.hops.hopsworks.common.project;

import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;

@XmlRootElement
public class MembersDTO {

  private List<ProjectTeam> projectTeam;

  public MembersDTO() {
  }

  public MembersDTO(List<ProjectTeam> projectTeam) {
    this.projectTeam = projectTeam;
  }

  public List<ProjectTeam> getProjectTeam() {
    return projectTeam;
  }

  public void setProjectTeam(List<ProjectTeam> projectTeam) {
    this.projectTeam = projectTeam;
  }

  @Override
  public String toString() {
    return "MembersDTO{" + "projectTeam=" + projectTeam + '}';
  }

}
