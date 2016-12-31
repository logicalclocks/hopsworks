package io.hops.hopsworks.common.dao.kafka;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "shared_topics",
        catalog = "hopsworks",
        schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "SharedTopics.findAll",
          query = "SELECT s FROM SharedTopics s"),
  @NamedQuery(name = "SharedTopics.findByTopicName",
          query
          = "SELECT s FROM SharedTopics s WHERE s.sharedTopicsPK.topicName = :topicName"),
  @NamedQuery(name = "SharedTopics.findByProjectId",
          query
          = "SELECT s FROM SharedTopics s WHERE s.sharedTopicsPK.projectId = :projectId"),
  @NamedQuery(name = "SharedTopics.findByTopicNameAndProjectId",
          query
          = "SELECT s FROM SharedTopics s WHERE s.sharedTopicsPK.projectId "
          + "= :projectId and s.sharedTopicsPK.topicName = :topicName")})
public class SharedTopics implements Serializable {

  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected SharedTopicsPK sharedTopicsPK;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 1000)
  @Column(name = "owner_id")
  private Integer projectId;

  public SharedTopics() {
  }

  public SharedTopics(SharedTopicsPK sharedTopicsPK, Integer projectId) {
    this.sharedTopicsPK = sharedTopicsPK;
    this.projectId = projectId;
  }

  public SharedTopics(String topicName, int owningId, Integer projectId) {
    this.sharedTopicsPK = new SharedTopicsPK(topicName, projectId);
    this.projectId = owningId;
  }

  public SharedTopicsPK getSharedTopicsPK() {
    return sharedTopicsPK;
  }

  public void setSharedTopicsPK(SharedTopicsPK sharedTopicsPK) {
    this.sharedTopicsPK = sharedTopicsPK;
  }

  public Integer getProjectId() {
    return projectId;
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += projectId;
    hash += (sharedTopicsPK != null ? sharedTopicsPK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof SharedTopics)) {
      return false;
    }
    SharedTopics other = (SharedTopics) object;
    if (this.projectId != other.projectId) {
      return false;
    }
    if ((this.sharedTopicsPK == null && other.sharedTopicsPK != null)
            || (this.sharedTopicsPK != null
            && !this.sharedTopicsPK.equals(other.sharedTopicsPK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.kafka.SharedTopics[ sharedTopicsPK="
            + sharedTopicsPK + " ] shared to: " + this.projectId;
  }

}
