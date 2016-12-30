package io.hops.hopsworks.common.dao.workflow;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonIgnore;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import io.hops.hopsworks.common.dao.user.Users;

@Entity
@Table(name = "hopsworks.workflow_executions")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "WorkflowExecution.find",
          query
          = "SELECT e FROM WorkflowExecution e WHERE e.id = :id AND e.workflowId = :workflowId")})
public class WorkflowExecution implements Serializable {

  public WorkflowExecution() {
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id",
          nullable = false)
  @XmlElement(name = "id")
  private Integer id;

  public Integer getId() {
    return id;
  }

  @Basic(optional = false)
  @NotNull
  @Column(name = "workflow_id",
          nullable = false)
  private Integer workflowId;

  public Integer getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(Integer workflowId) {
    this.workflowId = workflowId;
  }

  @Basic(optional = false)
  @NotNull
  @Column(name = "user_id",
          nullable = false)
  private Integer userId;

  public Integer getUserId() {
    return userId;
  }

  public void setUserId(Integer userId) {
    this.userId = userId;
  }

  @Basic(optional = false)
  @Column(name = "workflow_timestamp")
  private Date workflowTimestamp;

  public Date getWorkflowTimestamp() {
    return workflowTimestamp;
  }

  public void setWorkflowTimestamp(Date workflowTimestamp) {
    this.workflowTimestamp = workflowTimestamp;
  }

  @Basic(optional = false)
  @Column(name = "error",
          nullable = false)
  private String error;

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  @Column(name = "snapshot")
  @Convert(converter = NodeDataConverter.class)
  private JsonNode snapshot;

  public JsonNode getSnapshot() {
    return snapshot;
  }

  public void setSnapshot(JsonNode snapshot) {
    this.snapshot = snapshot;
  }

  public String getPath() {
    return "/Workflows/" + this.getUser().getUsername() + "/" + this.
            getWorkflow().getName() + "/" + this.getWorkflowTimestamp().
            getTime() + "/";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    WorkflowExecution workflowExecution = (WorkflowExecution) o;

    if (id != null ? !id.equals(workflowExecution.id) : workflowExecution.id
            != null) {
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
  @PrimaryKeyJoinColumn(name = "workflow_id")
  private Workflow workflow;

  @ManyToOne(fetch = FetchType.LAZY)
  @PrimaryKeyJoinColumn(name = "user_id")
  private Users user;

  @JsonIgnore
  @XmlTransient
  public Users getUser() {
    return user;
  }

  public void setUser(Users user) {
    this.user = user;
  }

  @JsonIgnore
  @XmlTransient
  public Workflow getWorkflow() {
    return workflow;
  }

  public void setWorkflow(Workflow workflow) {
    this.workflow = workflow;
  }

  @OneToMany(cascade = CascadeType.REMOVE,
          mappedBy = "execution")
  private Collection<WorkflowJob> jobs;

  @JsonIgnore
  @XmlTransient
  public Collection<WorkflowJob> getJobs() {
    return jobs;
  }

  @XmlElement(name = "jobIds")
  public Set<String> getJobIds() {
    Set<String> ids = new HashSet();
    for (WorkflowJob job : this.jobs) {
      ids.add(job.getId());
    }
    return ids;
  }
}
