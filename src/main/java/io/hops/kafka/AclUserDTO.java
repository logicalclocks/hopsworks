package io.hops.kafka;

import java.io.Serializable;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author misdess
 */
@XmlRootElement
public class AclUserDTO implements Serializable {

  private String projectName;
  private List<String> userEmails;

  public AclUserDTO() {
  }

  public AclUserDTO(String projectName) {
    this.projectName = projectName;
  }

  public AclUserDTO(String projectName, List<String> userEmails) {
    this.projectName = projectName;
    this.userEmails = userEmails;
  }

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public List<String> getUserEmails() {
    return userEmails;
  }

  public void setUserEmails(List<String> userEmails) {
    this.userEmails = userEmails;
  }

}
