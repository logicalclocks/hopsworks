package se.kth.hopsworks.certificates;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author paul
 */
@Embeddable
public class UserCertsPK implements Serializable {

  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 100)
  @Column(name = "projectname")
  private String projectname;
  @Basic(optional = false)
  @NotNull
  @Column(name = "user_id")
  private int userId;

  public UserCertsPK() {
  }

  public UserCertsPK(String projectname, int userId) {
    this.projectname = projectname;
    this.userId = userId;
  }

  public String getProjectname() {
    return projectname;
  }

  public void setProjectname(String projectname) {
    this.projectname = projectname;
  }

  public int getUserId() {
    return userId;
  }

  public void setUserId(int userId) {
    this.userId = userId;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (projectname != null ? projectname.hashCode() : 0);
    hash += (int) userId;
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof UserCertsPK)) {
      return false;
    }
    UserCertsPK other = (UserCertsPK) object;
    if ((this.projectname == null && other.projectname != null) || (this.projectname != null && !this.projectname.equals(other.projectname))) {
      return false;
    }
    if (this.userId != other.userId) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.hopsworks.certificates.UserCertsPK[ projectname=" + projectname + ", userId=" + userId + " ]";
  }
  
}
