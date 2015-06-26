package se.kth.bbc.security.ua.model;

import java.io.Serializable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@Entity
@Table(name = "vangelis_kthfs.people_group")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "PeopleGroup.findAll",
          query = "SELECT p FROM PeopleGroup p"),
  @NamedQuery(name = "PeopleGroup.findByUid",
          query = "SELECT p FROM PeopleGroup p WHERE p.peopleGroupPK.uid = :uid"),
  @NamedQuery(name = "PeopleGroup.findByGid",
          query = "SELECT p FROM PeopleGroup p WHERE p.peopleGroupPK.gid = :gid")})
public class PeopleGroup implements Serializable {

  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected PeopleGroupPK peopleGroupPK;
  @JoinColumn(name = "uid",
          referencedColumnName = "uid",
          insertable = false,
          updatable = false)
  @ManyToOne(optional = false)
  private User user;

  public PeopleGroup() {
  }

  public PeopleGroup(PeopleGroupPK peopleGroupPK) {
    this.peopleGroupPK = peopleGroupPK;
  }

  public PeopleGroup(int uid, int gid) {
    this.peopleGroupPK = new PeopleGroupPK(uid, gid);
  }

  public PeopleGroupPK getPeopleGroupPK() {
    return peopleGroupPK;
  }

  public void setPeopleGroupPK(PeopleGroupPK peopleGroupPK) {
    this.peopleGroupPK = peopleGroupPK;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (peopleGroupPK != null ? peopleGroupPK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof PeopleGroup)) {
      return false;
    }
    PeopleGroup other = (PeopleGroup) object;
    if ((this.peopleGroupPK == null && other.peopleGroupPK != null)
            || (this.peopleGroupPK != null && !this.peopleGroupPK.equals(
                    other.peopleGroupPK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.security.ua.model.PeopleGroup[ peopleGroupPK="
            + peopleGroupPK + " ]";
  }

}
