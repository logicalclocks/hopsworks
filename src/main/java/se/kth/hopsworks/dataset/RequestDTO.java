
package se.kth.hopsworks.dataset;

import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author ermiasg
 */
@XmlRootElement
public class RequestDTO {
  private Integer inodeId;
  private Integer projectId;
  private String message;

  public RequestDTO() {
  }

  public RequestDTO(Integer inodeId, Integer projectId, String message) {
    this.inodeId = inodeId;
    this.projectId = projectId;
    this.message = message;
  }

  public Integer getInodeId() {
    return inodeId;
  }

  public void setInodeId(Integer inodeId) {
    this.inodeId = inodeId;
  }

  public Integer getProjectId() {
    return projectId;
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
  
  
}
