package io.hops.hopsworks.common.dao.user;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class UserProjectDTO {

  private String email;
  private int projectId;
  private String role;

  public UserProjectDTO() {
  }

  public int getProjectId() {
    return projectId;
  }

  public String getRole() {
    return role;
  }

  public void setProject(int projectId) {
    this.projectId = projectId;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  @Override
  public String toString() {
    return "UserDTO{" + "email=" + email + ", project=" + projectId
            + ", role=" + role + '}';
  }

}
