/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.tf;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author jdowling
 */
@Entity
@Table(name = "tf_tasks", catalog = "hopsworks", schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "TfTasks.findAll", query = "SELECT t FROM TfTasks t"),
  @NamedQuery(name = "TfTasks.findById", query = "SELECT t FROM TfTasks t WHERE t.id = :id")})
public class TfTasks implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @Basic(optional = false)
  @NotNull
  @Column(name = "id")
  private Integer id;
  @JoinColumn(name = "job_id", referencedColumnName = "id")
  @ManyToOne(optional = false)
  private TfJobs jobId;
  @JoinColumn(name = "resource_id", referencedColumnName = "id")
  @ManyToOne(optional = false)
  private TfResources resourceId;

  public TfTasks() {
  }

  public TfTasks(Integer id) {
    this.id = id;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public TfJobs getJobId() {
    return jobId;
  }

  public void setJobId(TfJobs jobId) {
    this.jobId = jobId;
  }

  public TfResources getResourceId() {
    return resourceId;
  }

  public void setResourceId(TfResources resourceId) {
    this.resourceId = resourceId;
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
    if (!(object instanceof TfTasks)) {
      return false;
    }
    TfTasks other = (TfTasks) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.tf.TfTasks[ id=" + id + " ]";
  }
  
}
