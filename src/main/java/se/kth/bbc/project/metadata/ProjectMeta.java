package se.kth.bbc.project.metadata;

import java.io.Serializable;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.project.Project;

/**
 *
 * @author stig
 */
@Entity
@Table(name = "project_meta")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "ProjectMeta.findAll",
          query
          = "SELECT s FROM ProjectMeta s"),
  @NamedQuery(name = "ProjectMeta.findByProject",
          query
          = "SELECT s FROM ProjectMeta s WHERE s.project = :project"),
  @NamedQuery(name = "ProjectMeta.findByDescription",
          query
          = "SELECT s FROM ProjectMeta s WHERE s.description = :description")})
public class ProjectMeta implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @Basic(optional = false)
  @NotNull
  @Column(name = "project_id")
  private Integer projectId;

  @Size(max = 2000)
  @Column(name = "description")
  private String description;

  @JoinColumn(name = "project_id",
          referencedColumnName = "id",
          insertable = false,
          updatable
          = false)
  @OneToOne(optional = false)
  private Project project;

  @ElementCollection(targetClass = CollectionTypeProjectDesignEnum.class)
  @CollectionTable(name = "project_design",
          joinColumns = @JoinColumn(name = "project_id",
                  referencedColumnName = "project_id"))
  @Column(name = "design")
  @Enumerated(EnumType.STRING)
  private List<CollectionTypeProjectDesignEnum> projectDesignList;

  @ElementCollection(targetClass = InclusionCriteriumEnum.class)
  @CollectionTable(name = "project_inclusion_criteria",
          joinColumns = @JoinColumn(name = "project_id",
                  referencedColumnName = "project_id"))
  @Column(name = "criterium")
  @Enumerated(EnumType.STRING)
  private List<InclusionCriteriumEnum> inclusionCriteriaList;

  public List<CollectionTypeProjectDesignEnum> getProjectDesignList() {
    return projectDesignList;
  }

  public void setProjectDesignList(
          List<CollectionTypeProjectDesignEnum> projectDesignList) {
    this.projectDesignList = projectDesignList;
  }

  public List<InclusionCriteriumEnum> getInclusionCriteriaList() {
    return inclusionCriteriaList;
  }

  public void setInclusionCriteriaList(
          List<InclusionCriteriumEnum> inclusionCriteriaList) {
    this.inclusionCriteriaList = inclusionCriteriaList;
  }

  public ProjectMeta() {
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Project getProject() {
    return project;
  }

  public void setProject(Project project) {
    this.project = project;
  }

  public ProjectMeta(Integer projectId) {
    this.projectId = projectId;
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
    hash += (projectId != null ? projectId.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof ProjectMeta)) {
      return false;
    }
    ProjectMeta other = (ProjectMeta) object;
    if ((this.projectId == null && other.projectId != null) || (this.projectId
            != null && !this.projectId.equals(other.projectId))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.project.metadata.ProjectMeta[ projectId=" + projectId
            + " ]";
  }

}
