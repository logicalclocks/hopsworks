
package se.kth.hopsworks.hdfsUsers.model;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.codehaus.jackson.annotate.JsonIgnore;


@Entity
@Table(name = "hops.hdfs_groups")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "HdfsGroups.findAll",
          query
          = "SELECT h FROM HdfsGroups h"),
  @NamedQuery(name = "HdfsGroups.findByName",
          query
          = "SELECT h FROM HdfsGroups h WHERE h.name = :name")})
public class HdfsGroups implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @Basic(optional = false)
  @NotNull
  @Lob
  @Column(name = "id")
  private byte[] id;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 1000)
  @Column(name = "name")
  private String name;

  @ManyToMany(mappedBy = "hdfsGroupsCollection")
  private Collection<HdfsUsers> hdfsUsersCollection;

  public HdfsGroups() {
  }

  public HdfsGroups(byte[] id) {
    this.id = id;
  }

  public HdfsGroups(byte[] id, String name) {
    this.id = id;
    this.name = name;
  }

  public byte[] getId() {
    return id;
  }

  public void setId(byte[] id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<HdfsUsers> getHdfsUsersCollection() {
    return hdfsUsersCollection;
  }

  public void setHdfsUsersCollection(Collection<HdfsUsers> hdfsUsersCollection) {
    this.hdfsUsersCollection = hdfsUsersCollection;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (id != null ? id.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof HdfsGroups)) {
      return false;
    }
    HdfsGroups other = (HdfsGroups) object;
    if ((this.id == null && other.id != null) ||
            (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.hopsworks.hdfsUsers.HdfsGroups[ id=" + id + " ]";
  }
  
}
