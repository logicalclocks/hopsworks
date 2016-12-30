package io.hops.hopsworks.common.dao.workflow;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "oozie.WF_ACTIONS")
@XmlRootElement
public class WorkflowAction implements Serializable {

  @Id
  @Column(name = "id")
  private String id;
  @Basic
  @Column(name = "name")
  private String nodeId;
  @Basic
  @Column(name = "start_time")
  private Date startedAt;
  @Basic
  @Column(name = "end_time")
  private Date endedAt;
  @Basic
  @Column(name = "created_time")
  private Date createdAt;
  @Basic
  @Column(name = "last_check_time")
  private Date checkedAt;

  @Basic
  @Column(name = "error_code")
  private String errorCode;

  @Basic
  @Column(name = "error_message")
  private String errorMessage;
  @Basic
  @Column(name = "wf_id")
  private String jobId;

  @Basic
  @Column(name = "external_status")
  private String jobStatus;

  @Basic
  @Column(name = "pending")
  private Integer pending;

  @Basic
  @Column(name = "retries")
  private Integer retries;
  @Basic
  @Column(name = "signal_value")
  private String signalValue;

  @Basic
  @Column(name = "status")
  private String status;

  @Basic
  @Column(name = "transition")
  private String transition;

  @Basic
  @Column(name = "type")
  private String type;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getNodeId() {
    return nodeId;
  }

  public void setNodeId(String nodeId) {
    this.nodeId = nodeId;
  }

  public Date getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(Date startedAt) {
    this.startedAt = startedAt;
  }

  public Date getEndedAt() {
    return endedAt;
  }

  public void setEndedAt(Date endedAt) {
    this.endedAt = endedAt;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
  }

  public Date getCheckedAt() {
    return checkedAt;
  }

  public void setCheckedAt(Date checkedAt) {
    this.checkedAt = checkedAt;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(String errorCode) {
    this.errorCode = errorCode;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public String getJobId() {
    return jobId;
  }

  public void setJobId(String jobId) {
    this.jobId = jobId;
  }

  public String getJobStatus() {
    return jobStatus;
  }

  public void setJobStatus(String jobStatus) {
    this.jobStatus = jobStatus;
  }

  public Integer getPending() {
    return pending;
  }

  public void setPending(Integer pending) {
    this.pending = pending;
  }

  public Integer getRetries() {
    return retries;
  }

  public void setRetries(Integer retries) {
    this.retries = retries;
  }

  public String getSignalValue() {
    return signalValue;
  }

  public void setSignalValue(String signalValue) {
    this.signalValue = signalValue;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getTransition() {
    return transition;
  }

  public void setTransition(String transition) {
    this.transition = transition;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public WorkflowAction() {
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    WorkflowAction action = (WorkflowAction) o;

    if (id != null ? !id.equals(action.id) : action.id != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    return result;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @PrimaryKeyJoinColumn(name = "wf_id")
  private WorkflowJob job;

  public WorkflowJob getJob() {
    return job;
  }

  @OneToOne(cascade = CascadeType.ALL,
          fetch = FetchType.LAZY)
  @PrimaryKeyJoinColumn(name = "name",
          referencedColumnName = "external_id")
  private Node node;

  @XmlElement(name = "node")
  public Node getNode() {
    return node;
  }
}
