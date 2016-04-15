/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.kafka;

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
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.project.Project;

/**
 *
 * @author jdowling
 */
@Entity
@Table(name = "project_topics", catalog = "hopsworks", schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "ProjectTopics.findAll", query = "SELECT p FROM ProjectTopics p"),
  @NamedQuery(name = "ProjectTopics.findByTopicName", query = "SELECT p FROM ProjectTopics p WHERE p.topicName = :topicName"),
  @NamedQuery(name = "ProjectTopics.findByProject", query = "SELECT p FROM ProjectTopics p WHERE p.project = :project")

})
public class ProjectTopics implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 255)
  @Column(name = "topic_name")
  private String topicName;

  @JoinColumn(name = "project_id",
          referencedColumnName = "id",
          insertable = false,
          updatable = false)
  @ManyToOne(optional = false)
  private Project project;
  
  public ProjectTopics() {
  }

  public ProjectTopics(String topicName, Project project) {
    this.topicName = topicName;
    this.project = project;
  }

  public String getTopicName() {
    return topicName;
  }

  public void setTopicName(String topicName) {
    this.topicName = topicName;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (topicName != null ? topicName.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof ProjectTopics)) {
      return false;
    }
    ProjectTopics other = (ProjectTopics) object;
    if ((this.topicName == null && other.topicName != null) || (this.topicName != null && !this.topicName.equals(other.topicName))) {
      return false;
    }
    return true;
  }

  public Project getProject() {
    return project;
  }
  

  @Override
  public String toString() {
    return "io.hops.bbc.ProjectTopics[ topicName=" + topicName + " ]";
  }
  
}
