package io.hops.hopsworks.common.dao.kafka;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Embeddable
public class ProjectTopicsPK implements Serializable {

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "topic_name")
  private String topicName;
  @Basic(optional = false)
  @NotNull
  @Column(name = "project_id")
  private int projectId;

  public ProjectTopicsPK() {
  }

  public ProjectTopicsPK(String topicName, int projectId) {
    this.topicName = topicName;
    this.projectId = projectId;
  }

  public String getTopicName() {
    return topicName;
  }

  public void setTopicName(String topicName) {
    this.topicName = topicName;
  }

  public int getProjectId() {
    return projectId;
  }

  public void setProjectId(int projectId) {
    this.projectId = projectId;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (topicName != null ? topicName.hashCode() : 0);
    hash += (int) projectId;
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof ProjectTopicsPK)) {
      return false;
    }
    ProjectTopicsPK other = (ProjectTopicsPK) object;
    if ((this.topicName == null && other.topicName != null) || (this.topicName
            != null && !this.topicName.equals(other.topicName))) {
      return false;
    }
    if (this.projectId != other.projectId) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.kafka.ProjectTopicsPK[ topicName=" + topicName
            + ", projectId=" + projectId + " ]";
  }

}
