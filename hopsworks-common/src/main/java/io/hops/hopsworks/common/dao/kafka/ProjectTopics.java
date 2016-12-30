package io.hops.hopsworks.common.dao.kafka;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.CascadeType;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.codehaus.jackson.annotate.JsonIgnore;
import io.hops.hopsworks.common.dao.project.Project;

@Entity
@Table(name = "project_topics",
        catalog = "hopsworks",
        schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "ProjectTopics.findAll",
          query = "SELECT p FROM ProjectTopics p"),
  @NamedQuery(name = "ProjectTopics.findByTopicName",
          query
          = "SELECT p FROM ProjectTopics p WHERE p.projectTopicsPK.topicName = :topicName"),
  @NamedQuery(name = "ProjectTopics.findByProjectId",
          query
          = "SELECT p FROM ProjectTopics p WHERE p.projectTopicsPK.projectId = :projectId"),
  @NamedQuery(name = "ProjectTopics.findBySchemaVersion",
          query
          = "SELECT p FROM ProjectTopics p WHERE p.schemaTopics.schemaTopicsPK.name = :schema_name AND p.schemaTopics.schemaTopicsPK.version = :schema_version")})
public class ProjectTopics implements Serializable {

  private static final long serialVersionUID = 1L;

  @OneToMany(cascade = CascadeType.ALL,
          mappedBy = "projectTopics")
  private Collection<TopicAcls> topicAclsCollection;

  @JoinColumns({
    @JoinColumn(name = "schema_name",
            referencedColumnName = "name"),
    @JoinColumn(name = "schema_version",
            referencedColumnName = "version")})
  @ManyToOne(optional = false)
  private SchemaTopics schemaTopics;
  @JoinColumn(name = "project_id",
          referencedColumnName = "id",
          insertable = false,
          updatable = false)
  @ManyToOne(optional = false)
  private Project project;

  @EmbeddedId
  protected ProjectTopicsPK projectTopicsPK;

  public ProjectTopics() {
  }

  public ProjectTopics(ProjectTopicsPK projectTopicsPK) {
    this.projectTopicsPK = projectTopicsPK;
  }

  public ProjectTopics(String topicName, int projectId,
          SchemaTopics schemaTopics) {
    this.projectTopicsPK = new ProjectTopicsPK(topicName, projectId);
    this.schemaTopics = schemaTopics;
  }

  public ProjectTopics(SchemaTopics schemaTopics,
          ProjectTopicsPK projectTopicsPK) {
    this.schemaTopics = schemaTopics;
    this.projectTopicsPK = projectTopicsPK;
  }

  public ProjectTopicsPK getProjectTopicsPK() {
    return projectTopicsPK;
  }

  public void setProjectTopicsPK(ProjectTopicsPK projectTopicsPK) {
    this.projectTopicsPK = projectTopicsPK;
  }

  public SchemaTopics getSchemaTopics() {
    return schemaTopics;
  }

  public void setSchemaTopics(SchemaTopics schemaTopics) {
    this.schemaTopics = schemaTopics;
  }

  public Project getProject() {
    return project;
  }

  public void setProject(Project project) {
    this.project = project;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<TopicAcls> getTopicAclsCollection() {
    return topicAclsCollection;
  }

  public void setTopicAclsCollection(Collection<TopicAcls> topicAclsCollection) {
    this.topicAclsCollection = topicAclsCollection;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (projectTopicsPK != null ? projectTopicsPK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof ProjectTopics)) {
      return false;
    }
    ProjectTopics other = (ProjectTopics) object;
    if ((this.projectTopicsPK == null && other.projectTopicsPK != null)
            || (this.projectTopicsPK != null && !this.projectTopicsPK.equals(
                    other.projectTopicsPK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.kafka.ProjectTopics[ projectTopicsPK=" + projectTopicsPK
            + " ]";
  }

}
