/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.tf;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.hopsworks.user.model.Users;

/**
 *
 * @author jdowling
 */
@Entity
@Table(name = "tf_fifo_queue", catalog = "hopsworks", schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "TfFifoQueue.findAll", query = "SELECT t FROM TfFifoQueue t"),
  @NamedQuery(name = "TfFifoQueue.findById", query = "SELECT t FROM TfFifoQueue t WHERE t.id = :id"),
  @NamedQuery(name = "TfFifoQueue.findByNumCpus", query = "SELECT t FROM TfFifoQueue t WHERE t.numCpus = :numCpus"),
  @NamedQuery(name = "TfFifoQueue.findByNumGpus", query = "SELECT t FROM TfFifoQueue t WHERE t.numGpus = :numGpus"),
  @NamedQuery(name = "TfFifoQueue.findBySubmissionTime", query = "SELECT t FROM TfFifoQueue t WHERE t.submissionTime = :submissionTime")})
public class TfFifoQueue implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @NotNull
  @Column(name = "num_cpus")
  private int numCpus;
  @Basic(optional = false)
  @NotNull
  @Column(name = "num_gpus")
  private int numGpus;
  @Basic(optional = false)
  @NotNull
  @Column(name = "submission_time")
  @Temporal(TemporalType.TIMESTAMP)
  private Date submissionTime;
  @JoinColumn(name = "proj_user", referencedColumnName = "email")
  @ManyToOne(optional = false)
  private Users projUser;
  @JoinColumn(name = "cluster_id", referencedColumnName = "id")
  @ManyToOne(optional = false)
  private TfClusters clusterId;

  public TfFifoQueue() {
  }

  public TfFifoQueue(Integer id) {
    this.id = id;
  }

  public TfFifoQueue(Integer id, int numCpus, int numGpus, Date submissionTime) {
    this.id = id;
    this.numCpus = numCpus;
    this.numGpus = numGpus;
    this.submissionTime = submissionTime;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public int getNumCpus() {
    return numCpus;
  }

  public void setNumCpus(int numCpus) {
    this.numCpus = numCpus;
  }

  public int getNumGpus() {
    return numGpus;
  }

  public void setNumGpus(int numGpus) {
    this.numGpus = numGpus;
  }

  public Date getSubmissionTime() {
    return submissionTime;
  }

  public void setSubmissionTime(Date submissionTime) {
    this.submissionTime = submissionTime;
  }

  public Users getProjUser() {
    return projUser;
  }

  public void setProjUser(Users projUser) {
    this.projUser = projUser;
  }

  public TfClusters getClusterId() {
    return clusterId;
  }

  public void setClusterId(TfClusters clusterId) {
    this.clusterId = clusterId;
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
    if (!(object instanceof TfFifoQueue)) {
      return false;
    }
    TfFifoQueue other = (TfFifoQueue) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.tf.TfFifoQueue[ id=" + id + " ]";
  }
  
}
