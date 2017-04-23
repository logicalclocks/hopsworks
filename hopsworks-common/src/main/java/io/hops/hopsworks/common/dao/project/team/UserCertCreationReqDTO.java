
package io.hops.hopsworks.common.dao.project.team;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class UserCertCreationReqDTO implements Serializable{

  private String projectName;
  private String userName;

  public UserCertCreationReqDTO() {
  }

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }
  
}
