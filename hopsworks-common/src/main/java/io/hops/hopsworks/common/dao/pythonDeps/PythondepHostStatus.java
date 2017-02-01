package io.hops.hopsworks.common.dao.pythonDeps;

import java.io.Serializable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "pythondep_host_status",
        catalog = "hopsworks",
        schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "PythondepHostStatus.findAll",
          query
          = "SELECT p FROM PythondepHostStatus p"),
  @NamedQuery(name = "PythondepHostStatus.findByProjectId",
          query
          = "SELECT p FROM PythondepHostStatus p WHERE p.pythondepHostStatusPK.projectId = :projectId"),
  @NamedQuery(name = "PythondepHostStatus.findByDepId",
          query
          = "SELECT p FROM PythondepHostStatus p WHERE p.pythondepHostStatusPK.depId = :depId"),
  @NamedQuery(name = "PythondepHostStatus.findByRepoId",
          query
          = "SELECT p FROM PythondepHostStatus p WHERE p.pythondepHostStatusPK.repoId = :repoId"),
  @NamedQuery(name = "PythondepHostStatus.findByHostId",
          query
          = "SELECT p FROM PythondepHostStatus p WHERE p.pythondepHostStatusPK.hostId = :hostId"),
  @NamedQuery(name = "PythondepHostStatus.findByStatus",
          query
          = "SELECT p FROM PythondepHostStatus p WHERE p.status = :status")})
public class PythondepHostStatus implements Serializable {

  public enum Status {
    INSTALLED,
    INSTALLING,
    FAILED
  }
  public enum Op {
    CREATE_REPO,
    REMOVE_REPO,
    CLONE_REPO,
    UPGRADE,
    ADD,
    REMOVE
  }

  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected PythondepHostStatusPK pythondepHostStatusPK;


  @Enumerated(EnumType.ORDINAL)
  private Status status = Status.INSTALLING;

  @Enumerated(EnumType.ORDINAL)
  private Op op;

  public PythondepHostStatus() {
  }

  public PythondepHostStatus(PythondepHostStatusPK pythondepHostStatusPK) {
    this.pythondepHostStatusPK = pythondepHostStatusPK;
  }

  public PythondepHostStatus(PythondepHostStatusPK pythondepHostStatusPK,
          PythondepHostStatus.Status status) {
    this.pythondepHostStatusPK = pythondepHostStatusPK;
    this.status = status;
  }

  public PythondepHostStatus(int projectId, int depId, int repoId, int hostId) {
    this.pythondepHostStatusPK
            = new PythondepHostStatusPK(projectId, depId, repoId, hostId);
  }

  public PythondepHostStatusPK getPythondepHostStatusPK() {
    return pythondepHostStatusPK;
  }

  public void setPythondepHostStatusPK(
          PythondepHostStatusPK pythondepHostStatusPK) {
    this.pythondepHostStatusPK = pythondepHostStatusPK;
  }

  public PythondepHostStatus.Status getStatus() {
    return status;
  }

  public void setStatus(PythondepHostStatus.Status status) {
    this.status = status;
  }

  public Op getOp() {
    return op;
  }

  public void setOp(Op op) {
    this.op = op;
  }
  
  @Override
  public int hashCode() {
    int hash = 0;
    hash += (pythondepHostStatusPK != null ? pythondepHostStatusPK.hashCode()
            : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof PythondepHostStatus)) {
      return false;
    }
    PythondepHostStatus other = (PythondepHostStatus) object;
    if ((this.pythondepHostStatusPK == null && other.pythondepHostStatusPK
            != null) || (this.pythondepHostStatusPK != null
            && !this.pythondepHostStatusPK.equals(other.pythondepHostStatusPK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.pythonDeps.PythondepHostStatus[ pythondepHostStatusPK="
            + pythondepHostStatusPK + " ]";
  }

}
