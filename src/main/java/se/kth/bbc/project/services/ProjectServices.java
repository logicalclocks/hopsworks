package se.kth.bbc.project.services;

import java.io.Serializable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.project.Project;

/**
 *
 * @author stig
 */
@Entity
@Table(name = "vangelis_kthfs.project_services")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "ProjectServices.findAll",
          query
          = "SELECT s FROM ProjectServices s"),
  @NamedQuery(name = "ProjectServices.findByProject",
          query
          = "SELECT s FROM ProjectServices s WHERE s.project = :project"),
  @NamedQuery(name = "ProjectServices.findServicesByProject",
          query
          = "SELECT s.projectServicePK.service FROM ProjectServices s WHERE s.project = :project ORDER BY s.projectServicePK.service"),
  @NamedQuery(name = "ProjectServices.findByService",
          query
          = "SELECT s FROM ProjectServices s WHERE s.projectServicePK.service = :service")})
public class ProjectServices implements Serializable {

  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected ProjectServicePK projectServicePK;
  @JoinColumn(name = "project_id",
          referencedColumnName = "id",
          insertable = false,
          updatable
          = false)
  @ManyToOne(optional = false)
  private Project project;

  public ProjectServices() {
  }

  public ProjectServices(ProjectServicePK projectServicesPK) {
    this.projectServicePK = projectServicesPK;
  }

  public ProjectServices(Project project, ProjectServiceEnum service) {
    this.projectServicePK = new ProjectServicePK(project.getId(), service);
  }

  public ProjectServicePK getProjectServicesPK() {
    return projectServicePK;
  }

  public void setProjectServicesPK(ProjectServicePK projectServicesPK) {
    this.projectServicePK = projectServicesPK;
  }

  public Project getProject() {
    return project;
  }

  public void setProject(Project project) {
    this.project = project;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (projectServicePK != null ? projectServicePK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof ProjectServices)) {
      return false;
    }
    ProjectServices other = (ProjectServices) object;
    if ((this.projectServicePK == null && other.projectServicePK != null)
            || (this.projectServicePK != null && !this.projectServicePK.equals(
                    other.projectServicePK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "[" + project + "," + projectServicePK.getService() + " ]";
  }

}
