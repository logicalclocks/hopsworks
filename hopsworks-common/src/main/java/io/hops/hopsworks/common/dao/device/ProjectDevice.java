package io.hops.hopsworks.common.dao.device;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "hopsworks.project_devices")
@XmlRootElement
@NamedQueries({
  @NamedQuery(
      name = "ProjectDevice.findAll",
      query = "SELECT pd FROM ProjectDevice pd"),
  @NamedQuery(
      name = "ProjectDevice.findByProjectId",
      query = "SELECT pd FROM ProjectDevice pd WHERE pd.projectDevicePK.projectId = :projectId"),
  @NamedQuery(
    name = "ProjectDevice.findByProjectIdAndState",
    query = "SELECT pd FROM ProjectDevice pd WHERE pd.projectDevicePK.projectId = :projectId AND pd.state = :state"),
  @NamedQuery(
      name = "ProjectDevice.findByProjectDevicePK",
      query= "SELECT pd FROM ProjectDevice pd WHERE pd.projectDevicePK = :projectDevicePK")})
public class ProjectDevice implements Serializable{

  private static final long serialVersionUID = 1L;

  @EmbeddedId
  private ProjectDevicePK projectDevicePK;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 64)
  @Column(name = "password")
  private String password;

  @Size(min = 1, max = 80)
  @Column(name = "alias")
  private String alias;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "created_at")
  private Date createdAt;

  /**
   * The state can take the following integer values that are directly linked with the enum State that is
   * defined within this class. The index of the elements within the enum State controls its corresponding value.
   *  0 -> Pending
   *  1 -> Enabled
   *  2 -> Disabled
   *
   */
  @Basic(optional = false)
  @Column(name = "state")
  private Integer state;
  
  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "last_logged_in")
  private Date lastLoggedIn;

  public enum State{
    Pending,
    Approved,
    Disabled
  }
  
  public ProjectDevice() {}

  public ProjectDevice(ProjectDevicePK projectDevicePK, String password, State deviceState, String alias) {
    this.projectDevicePK = projectDevicePK;
    this.password = password;
    this.state = deviceState.ordinal();
    this.alias = alias;
  }

  public ProjectDevicePK getProjectDevicePK() {
    return projectDevicePK;
  }

  public void setProjectDevicePK(ProjectDevicePK projectDevicePK) {
    this.projectDevicePK = projectDevicePK;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String passUuid) {
    this.password = passUuid;
  }

  public String getAlias() {
    return alias;
  }

  public void setAlias(String alias) {
    this.alias = alias;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
  }

  public State getState(){
    return State.values()[state];
  }

  public void setState(State state) {
    this.state = state.ordinal();
  }

  public Date getLastLoggedIn() {
    return lastLoggedIn;
  }

  public void setLastLoggedIn(Date lastProduced) {
    this.lastLoggedIn = lastProduced;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (
        this.projectDevicePK != null ? this.projectDevicePK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof ProjectDevice)) {
      return false;
    }

    ProjectDevice other = (ProjectDevice) object;

    return !((this.projectDevicePK == null && other.projectDevicePK != null)
        || (this.projectDevicePK != null
        && !this.projectDevicePK.equals(other.projectDevicePK)));
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.device.ProjectDevice[ " +
        "projectDevicePK= " + this.projectDevicePK + " ]";
  }

}
