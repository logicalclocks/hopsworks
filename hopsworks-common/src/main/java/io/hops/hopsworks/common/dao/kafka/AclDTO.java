package io.hops.hopsworks.common.dao.kafka;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class AclDTO implements Serializable {

  private Integer id;
  private String projectName;
  private String userEmail;
  private String permissionType;
  private String operationType;
  private String host;
  private String role;

  public AclDTO() {
  }

  public AclDTO(Integer id, String projectName, String userEmail,
      String permissionType, String operationType, String host, String role) {
    this.id = id;
    this.projectName = projectName;
    this.userEmail = userEmail;
    this.permissionType = permissionType;
    this.operationType = operationType;
    this.host = host;
    this.role = role;
  }
  
  public AclDTO(String projectName, String userEmail,
      String permissionType, String operationType, String host, String role) {
    this.projectName = projectName;
    this.userEmail = userEmail;
    this.permissionType = permissionType;
    this.operationType = operationType;
    this.host = host;
    this.role = role;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public String getHost() {
    return host;
  }

  public String getOperationType() {
    return operationType;
  }

  public String getPermissionType() {
    return permissionType;
  }

  public String getRole() {
    return role;
  }

  public String getUserEmail() {
    return userEmail;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public void setOperationType(String operationType) {
    this.operationType = operationType;
  }

  public void setPermissionType(String permissionType) {
    this.permissionType = permissionType;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public void setUserEmail(String userEmail) {
    this.userEmail = userEmail;
  }
}
