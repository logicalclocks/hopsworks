package io.hops.hopsworks.common.dao.ndb;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "ndb_backup",
        catalog = "hopsworks",
        schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "NdbBackup.findAll",
          query
          = "SELECT n FROM NdbBackup n"),
  @NamedQuery(name = "NdbBackup.findByBackupId",
          query
          = "SELECT n FROM NdbBackup n WHERE n.backupId = :backupId"),
  @NamedQuery(name = "NdbBackup.findHighestBackupId",
          query
          = "SELECT MAX(n.backupId) FROM NdbBackup n"),
  @NamedQuery(name = "NdbBackup.findByCreated",
          query
          = "SELECT n FROM NdbBackup n WHERE n.created = :created")})
public class NdbBackup implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @Basic(optional = false)
  @NotNull
  @Column(name = "backup_id")
  private Integer backupId;
  @Basic(optional = false)
  @NotNull
  @Column(name = "created")
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;

  public NdbBackup() {
  }

  public NdbBackup(Integer backupId) {
    this.backupId = backupId;
  }

  public NdbBackup(Integer backupId, Date created) {
    this.backupId = backupId;
    this.created = created;
  }

  public Integer getBackupId() {
    return backupId;
  }

  public void setBackupId(Integer backupId) {
    this.backupId = backupId;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (backupId != null ? backupId.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof NdbBackup)) {
      return false;
    }
    NdbBackup other = (NdbBackup) object;
    if ((this.backupId == null && other.backupId != null) || (this.backupId
            != null && !this.backupId.equals(other.backupId))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.admin.fsOps.NdbBackup[ backupId=" + backupId
            + " ]";
  }

}
