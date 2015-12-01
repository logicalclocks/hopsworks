package se.kth.bbc.security.ua.model;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
 
@Embeddable
public class PeopleGroupPK implements Serializable {

  @Basic(optional = false)
  @NotNull
  @Column(name = "uid")
  private int uid;
  @Basic(optional = false)
  @NotNull
  @Column(name = "gid")
  private int gid;

  public PeopleGroupPK() {
  }

  public PeopleGroupPK(int uid, int gid) {
    this.uid = uid;
    this.gid = gid;
  }

  public int getUid() {
    return uid;
  }

  public void setUid(int uid) {
    this.uid = uid;
  }

  public int getGid() {
    return gid;
  }

  public void setGid(int gid) {
    this.gid = gid;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (int) uid;
    hash += (int) gid;
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof PeopleGroupPK)) {
      return false;
    }
    PeopleGroupPK other = (PeopleGroupPK) object;
    if (this.uid != other.uid) {
      return false;
    }
    if (this.gid != other.gid) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.security.ua.model.PeopleGroupPK[ uid=" + uid + ", gid="
            + gid + " ]";
  }

}
