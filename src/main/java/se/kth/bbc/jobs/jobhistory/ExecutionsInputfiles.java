/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.jobs.jobhistory;

import java.io.Serializable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.project.fb.Inode;

@Entity
@Table(name = "hopsworks.executions_inputfiles")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "ExecutionsInputfiles.findAll", query = "SELECT e FROM ExecutionsInputfiles e"),
  @NamedQuery(name = "ExecutionsInputfiles.findByExecutionId", query = "SELECT e FROM ExecutionsInputfiles e WHERE e.executionsInputfilesPK.executionId = :executionId"),
  @NamedQuery(name = "ExecutionsInputfiles.findByInodePid", query = "SELECT e FROM ExecutionsInputfiles e WHERE e.executionsInputfilesPK.inodePid = :inodePid"),
  @NamedQuery(name = "ExecutionsInputfiles.findByInodePidName", query = "SELECT e FROM ExecutionsInputfiles e WHERE e.executionsInputfilesPK.inodePid = :inodePid AND e.executionsInputfilesPK.inodeName = :inodeName"),
  @NamedQuery(name = "ExecutionsInputfiles.findByInodeName", query = "SELECT e FROM ExecutionsInputfiles e WHERE e.executionsInputfilesPK.inodeName = :inodeName")})
public class ExecutionsInputfiles implements Serializable {

  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected ExecutionsInputfilesPK executionsInputfilesPK;
  
  @JoinColumn(name = "executionId",
          referencedColumnName = "id",
          insertable = false,
          updatable
          = false)
  @ManyToOne(optional = false)
  private int execution;
  
  @JoinColumn(name = "inode_pid",
          referencedColumnName = "parent_id",
          insertable = false,
          updatable
          = false)
  @ManyToOne(optional = false)
  private int inode_pid;
  
  @JoinColumn(name = "inode_name",
          referencedColumnName = "name",
          insertable = false,
          updatable
          = false)
  @ManyToOne(optional = false)
  private String inode_name;
  
  public ExecutionsInputfiles() {
  }

  public ExecutionsInputfiles(ExecutionsInputfilesPK executionsInputfilesPK) {
    this.executionsInputfilesPK = executionsInputfilesPK;
  }

  public ExecutionsInputfiles(int executionId, int inodePid, String inodeName) {
    this.executionsInputfilesPK = new ExecutionsInputfilesPK(executionId, inodePid, inodeName);
  }

  public ExecutionsInputfilesPK getExecutionsInputfilesPK() {
    return executionsInputfilesPK;
  }

  public void setExecutionsInputfilesPK(ExecutionsInputfilesPK executionsInputfilesPK) {
    this.executionsInputfilesPK = executionsInputfilesPK;
  }

  public int getExecution() {
    return execution;
  }
  
  @Override
  public int hashCode() {
    int hash = 0;
    hash += (executionsInputfilesPK != null ? executionsInputfilesPK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof ExecutionsInputfiles)) {
      return false;
    }
    ExecutionsInputfiles other = (ExecutionsInputfiles) object;
    if ((this.executionsInputfilesPK == null && other.executionsInputfilesPK != null) || (this.executionsInputfilesPK != null && !this.executionsInputfilesPK.equals(other.executionsInputfilesPK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.jobs.execution.ExecutionsInputfiles[ executionsInputfilesPK=" + executionsInputfilesPK + " ]";
  }
  
}
