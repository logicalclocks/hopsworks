package io.hops.hopsworks.common.dao.jobs;

import io.hops.hopsworks.common.dao.jobhistory.Execution;
import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.codehaus.jackson.annotate.JsonIgnore;

@Entity
@Table(name = "hopsworks.files_to_remove")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "FilesToRemove.findAll",
      query
      = "SELECT j FROM FilesToRemove j"),
  @NamedQuery(name = "FilesToRemove.findByExecutionId",
      query
      = "SELECT j FROM FilesToRemove j WHERE j.filesToRemovePK.executionId = :executionId"),
  @NamedQuery(name = "FilesToRemove.findByPath",
      query
      = "SELECT j FROM FilesToRemove j WHERE j.path = :path")})
public class FilesToRemove implements Serializable {

  private static final long serialVersionUID = 1L;

  @EmbeddedId
  protected FilesToRemovePK filesToRemovePK;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 255)
  @Column(name = "path")
  private String path;

  @JoinColumn(name = "execution_id",
      referencedColumnName = "id",
      insertable = false,
      updatable = false)
  @ManyToOne(optional = false)
  private Execution execution;

  public FilesToRemove() {
  }

  public FilesToRemove(FilesToRemovePK filesToRemovePK) {
    this.filesToRemovePK = filesToRemovePK;
  }

  public FilesToRemove(long executionId, String path) {
    this.filesToRemovePK = new FilesToRemovePK(executionId, path);
  }

  public FilesToRemovePK getFilesToRemovePK() {
    return filesToRemovePK;
  }

  public void setFilesToRemovePK(FilesToRemovePK filesToRemovePK) {
    this.filesToRemovePK = filesToRemovePK;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  @XmlTransient
  @JsonIgnore
  public Execution getExecution() {
    return execution;
  }

  public void setExecution(Execution execution) {
    this.execution = execution;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (filesToRemovePK != null ? filesToRemovePK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof FilesToRemove)) {
      return false;
    }
    FilesToRemove other = (FilesToRemove) object;
    return !((this.filesToRemovePK == null && other.filesToRemovePK != null) || (this.filesToRemovePK != null
        && !this.filesToRemovePK.equals(other.filesToRemovePK)));
  }

  @Override
  public String toString() {
    return "se.kth.bbc.job.FilesToRemove[ filesToRemovePK=" + filesToRemovePK + " ]";
  }

}
