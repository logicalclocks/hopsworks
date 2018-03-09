package io.hops.hopsworks.common.dao.device;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "hopsworks.project_devices_settings")
@XmlRootElement
@NamedQueries({
  @NamedQuery(
      name = "ProjectDevicesSettings.findAll",
      query = "SELECT pds FROM ProjectDevicesSettings pds"),
  @NamedQuery(
      name = "ProjectDevicesSettings.findByProjectId",
      query= "SELECT pds FROM ProjectDevicesSettings pds WHERE pds.projectId = :projectId")})
public class ProjectDevicesSettings implements Serializable{

  private static final long serialVersionUID = 1L;

  @Id
  @Column(name = "project_id")
  private Integer projectId;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 128)
  @Column(name = "jwt_secret")
  private String jwtSecret;

  @Basic(optional = false)
  @NotNull
  @Column(name = "jwt_token_duration")
  private Integer jwtTokenDuration;
  
  public ProjectDevicesSettings(){
  }

  public ProjectDevicesSettings(Integer projectId, String jwtSecret,
                                Integer jwtTokenDuration) {
    this.projectId = projectId;
    this.jwtSecret = jwtSecret;
    this.jwtTokenDuration = jwtTokenDuration;
  }

  public int getProjectId() {
    return projectId;
  }

  public void setProjectId(int projectId) {
    this.projectId = projectId;
  }

  public String getJwtSecret() {
    return jwtSecret;
  }

  public void setJwtSecret(String jwtSecret) {
    this.jwtSecret = jwtSecret;
  }

  public Integer getJwtTokenDuration() {
    return jwtTokenDuration;
  }

  public void setJwtTokenDuration(Integer jwtTokenDuration) {
    this.jwtTokenDuration = jwtTokenDuration;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (this.projectId != null ? this.projectId.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof ProjectDevicesSettings)) {
      return false;
    }
    ProjectDevicesSettings other = (ProjectDevicesSettings) object;
    return this.projectId != other.projectId;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.device.ProjectDevicesSettings[" +
        "projectId=" + this.projectId + " ]";
  }

}
