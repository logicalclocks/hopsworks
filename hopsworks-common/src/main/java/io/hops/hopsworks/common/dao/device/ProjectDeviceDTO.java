package io.hops.hopsworks.common.dao.device;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;

@XmlRootElement
public class ProjectDeviceDTO implements Serializable{

  private Integer projectId;

  private String deviceUuid;

  private String alias;

  private Date createdAt;

  private String state;

  private Date lastLoggedIn;

  public ProjectDeviceDTO(){}

  public ProjectDeviceDTO(
    Integer projectId, String deviceUuid, String alias, Date createdAt, String state, Date lastLoggedIn) {
    this.projectId = projectId;
    this.deviceUuid = deviceUuid;
    this.alias = alias;
    this.createdAt = createdAt;
    this.state = state;
    this.lastLoggedIn = lastLoggedIn;
  }


  public Integer getProjectId() {
    return projectId;
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }

  public String getDeviceUuid() {
    return deviceUuid;
  }

  public void setDeviceUuid(String deviceUuid) {
    this.deviceUuid = deviceUuid;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public Date getLastLoggedIn() {
    return lastLoggedIn;
  }

  public void setLastLoggedIn(Date lastLoggedIn) {
    this.lastLoggedIn = lastLoggedIn;
  }

  public String getAlias() {
    return alias;
  }

  public void setAlias(String alias) {
    this.alias = alias;
  }
}
