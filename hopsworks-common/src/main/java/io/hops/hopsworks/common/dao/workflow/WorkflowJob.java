package io.hops.hopsworks.common.dao.workflow;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import javax.persistence.OneToMany;

@Entity
@Table(name = "oozie.WF_JOBS")
@XmlRootElement
public class WorkflowJob implements Serializable {

  @Id
  @Column(name = "id",
          nullable = false)
  private String id;
  @Basic
  @Column(name = "app_path")
  private String path;
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
  @Column(name = "last_modified_time")
  private Date updatedAt;
  @Basic
  @Column(name = "status")
  private String status;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
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

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public WorkflowJob() {
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    WorkflowJob job = (WorkflowJob) o;

    if (id != null ? !id.equals(job.id) : job.id != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    return result;
  }

  @ManyToOne(cascade = CascadeType.ALL)
  @PrimaryKeyJoinColumn(name = "app_name")
  private WorkflowExecution execution;

  public WorkflowExecution getExecution() {
    return execution;
  }

  public Boolean isDone() {
    return !(this.status.toLowerCase() == "running" || this.status.toLowerCase()
            == "prep");
  }

  @OneToMany(cascade = CascadeType.REMOVE,
          mappedBy = "job")
  private Collection<WorkflowAction> actions;

  @XmlElement(name = "actions")
  public Collection<WorkflowAction> getActions() {
    return actions;
  }
}
