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
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author jdowling
 */
@Entity
@Table(name = "tf_executions", catalog = "hopsworks", schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "TfExecutions.findAll", query = "SELECT t FROM TfExecutions t"),
  @NamedQuery(name = "TfExecutions.findByAttemptId", query = "SELECT t FROM TfExecutions t WHERE t.attemptId = :attemptId"),
  @NamedQuery(name = "TfExecutions.findByResult", query = "SELECT t FROM TfExecutions t WHERE t.result = :result"),
  @NamedQuery(name = "TfExecutions.findByStartTime", query = "SELECT t FROM TfExecutions t WHERE t.startTime = :startTime"),
  @NamedQuery(name = "TfExecutions.findByEndTime", query = "SELECT t FROM TfExecutions t WHERE t.endTime = :endTime")})
public class TfExecutions implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @Basic(optional = false)
  @NotNull
  @Column(name = "attempt_id")
  private Integer attemptId;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 255)
  @Column(name = "result")
  private String result;
  @Basic(optional = false)
  @NotNull
  @Column(name = "start_time")
  @Temporal(TemporalType.TIMESTAMP)
  private Date startTime;
  @Basic(optional = false)
  @NotNull
  @Column(name = "end_time")
  @Temporal(TemporalType.TIMESTAMP)
  private Date endTime;
  @JoinColumn(name = "job_id", referencedColumnName = "id")
  @ManyToOne(optional = false)
  private TfJobs jobId;
  @JoinColumn(name = "cluster_id", referencedColumnName = "id")
  @ManyToOne(optional = false)
  private TfClusters clusterId;
  @JoinColumn(name = "resource_id", referencedColumnName = "id")
  @ManyToOne(optional = false)
  private TfResources resourceId;

  public TfExecutions() {
  }

  public TfExecutions(Integer attemptId) {
    this.attemptId = attemptId;
  }

  public TfExecutions(Integer attemptId, String result, Date startTime, Date endTime) {
    this.attemptId = attemptId;
    this.result = result;
    this.startTime = startTime;
    this.endTime = endTime;
  }

  public Integer getAttemptId() {
    return attemptId;
  }

  public void setAttemptId(Integer attemptId) {
    this.attemptId = attemptId;
  }

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  public Date getStartTime() {
    return startTime;
  }

  public void setStartTime(Date startTime) {
    this.startTime = startTime;
  }

  public Date getEndTime() {
    return endTime;
  }

  public void setEndTime(Date endTime) {
    this.endTime = endTime;
  }

  public TfJobs getJobId() {
    return jobId;
  }

  public void setJobId(TfJobs jobId) {
    this.jobId = jobId;
  }

  public TfClusters getClusterId() {
    return clusterId;
  }

  public void setClusterId(TfClusters clusterId) {
    this.clusterId = clusterId;
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
    hash += (attemptId != null ? attemptId.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof TfExecutions)) {
      return false;
    }
    TfExecutions other = (TfExecutions) object;
    if ((this.attemptId == null && other.attemptId != null) || (this.attemptId != null && !this.attemptId.equals(other.attemptId))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.tf.TfExecutions[ attemptId=" + attemptId + " ]";
  }
  
}
