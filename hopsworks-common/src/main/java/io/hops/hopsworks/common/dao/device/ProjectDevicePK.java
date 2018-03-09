package io.hops.hopsworks.common.dao.device;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Embeddable
public class ProjectDevicePK implements Serializable {

  private static final long serialVersionUID = 1L;

  @Basic(optional = false)
  @NotNull
  @Column(name = "project_id")
  private Integer projectId;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 36)
  @Column(name = "device_uuid")
  private String deviceUuid;

  public ProjectDevicePK() {
  }

  public ProjectDevicePK(Integer projectId, String deviceUuid) {
    this.projectId = projectId;
    this.deviceUuid = deviceUuid;
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

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (int) projectId;
    hash += (deviceUuid != null ? deviceUuid.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof ProjectDevicePK)) {
      return false;
    }
    ProjectDevicePK other = (ProjectDevicePK) object;
    return this.projectId == other.projectId &&
        this.deviceUuid.equals(other.deviceUuid);
  
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.device.ProjectDevicePK[ projectId="
        + this.projectId +  ", deviceUuid=" + this.deviceUuid + " ]";

  }
}
