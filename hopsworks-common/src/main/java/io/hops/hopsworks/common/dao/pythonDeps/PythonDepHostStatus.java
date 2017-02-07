package io.hops.hopsworks.common.dao.pythonDeps;

import io.hops.hopsworks.common.dao.pythonDeps.PythonDepsFacade.CondaOp;
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
  @NamedQuery(name = "PythonDepHostStatus.findAll",
          query
          = "SELECT p FROM PythonDepHostStatus p"),
  @NamedQuery(name = "PythonDepHostStatus.findByProjectId",
          query
          = "SELECT p FROM PythonDepHostStatus p WHERE p.pythondepHostStatusPK.projectId = :projectId"),
  @NamedQuery(name = "PythonDepHostStatus.findByDepId",
          query
          = "SELECT p FROM PythonDepHostStatus p WHERE p.pythondepHostStatusPK.depId = :depId"),
  @NamedQuery(name = "PythonDepHostStatus.findByRepoId",
          query
          = "SELECT p FROM PythonDepHostStatus p WHERE p.pythondepHostStatusPK.repoId = :repoId"),
  @NamedQuery(name = "PythonDepHostStatus.findByHostId",
          query
          = "SELECT p FROM PythonDepHostStatus p WHERE p.pythondepHostStatusPK.hostId = :hostId"),
  @NamedQuery(name = "PythonDepHostStatus.findByStatus",
          query
          = "SELECT p FROM PythonDepHostStatus p WHERE p.status = :status")})
public class PythonDepHostStatus implements Serializable {

  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected PythonDepHostStatusPK pythondepHostStatusPK;

  @Enumerated(EnumType.ORDINAL)
  private PythonDep.Status status = PythonDep.Status.INSTALLING;

  @Enumerated(EnumType.ORDINAL)
  private PythonDepsFacade.CondaOp op;

  public PythonDepHostStatus() {
  }

  public PythonDepHostStatus(PythonDepHostStatusPK pythondepHostStatusPK,
          CondaOp op, PythonDep.Status status) {
    this.pythondepHostStatusPK = pythondepHostStatusPK;
    this.op = op;
    this.status = status;
  }

  public PythonDepHostStatus(int projectId, int depId, int repoId, int hostId, 
          CondaOp op) {
    this.pythondepHostStatusPK
            = new PythonDepHostStatusPK(projectId, depId, repoId, hostId);
    this.op = op;
  }

  public PythonDepHostStatusPK getPythonDepHostStatusPK() {
    return pythondepHostStatusPK;
  }

  public void setPythonDepHostStatusPK(
          PythonDepHostStatusPK pythondepHostStatusPK) {
    this.pythondepHostStatusPK = pythondepHostStatusPK;
  }

  public PythonDep.Status getStatus() {
    return status;
  }

  public void setStatus(PythonDep.Status status) {
    this.status = status;
  }

  public CondaOp getOp() {
    return op;
  }

  public void setOp(CondaOp op) {
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
    if (!(object instanceof PythonDepHostStatus)) {
      return false;
    }
    PythonDepHostStatus other = (PythonDepHostStatus) object;
    if ((this.pythondepHostStatusPK == null && other.pythondepHostStatusPK
            != null) || (this.pythondepHostStatusPK != null
            && !this.pythondepHostStatusPK.equals(other.pythondepHostStatusPK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.pythonDeps.PythonDepHostStatus[ pythondepHostStatusPK="
            + pythondepHostStatusPK + " ]";
  }

}
