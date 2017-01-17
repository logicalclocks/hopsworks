/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.hopsworks.common.dao.pythonDeps;

import io.hops.hopsworks.common.dao.project.Project;
import java.io.Serializable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "project_pythondeps",
        catalog = "hopsworks",
        schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "ProjectPythondeps.findAll",
          query = "SELECT p FROM ProjectPythondeps p"),
  @NamedQuery(name = "ProjectPythondeps.findByProjectId",
          query
          = "SELECT p FROM ProjectPythondeps p WHERE p.projectPythondepsPK.projectId = :projectId"),
  @NamedQuery(name = "ProjectPythondeps.findByDepId",
          query
          = "SELECT p FROM ProjectPythondeps p WHERE p.projectPythondepsPK.depId = :depId")})
public class ProjectPythondeps implements Serializable {

  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected ProjectPythondepsPK projectPythondepsPK;
  @JoinColumn(name = "dep_id",
          referencedColumnName = "id",
          insertable = false,
          updatable = false)
  @ManyToOne(optional = false)
  private PythonDep pythonDep;

  @JoinColumn(name = "project_id",
          referencedColumnName = "id",
          insertable = false,
          updatable = false)
  @ManyToOne(optional = false)
  private Project project;

  public ProjectPythondeps() {
  }

  public ProjectPythondeps(ProjectPythondepsPK projectPythondepsPK) {
    this.projectPythondepsPK = projectPythondepsPK;
  }

  public ProjectPythondeps(int projectId, int depId) {
    this.projectPythondepsPK = new ProjectPythondepsPK(projectId, depId);
  }

  public ProjectPythondepsPK getProjectPythondepsPK() {
    return projectPythondepsPK;
  }

  public void setProjectPythondepsPK(ProjectPythondepsPK projectPythondepsPK) {
    this.projectPythondepsPK = projectPythondepsPK;
  }

  public PythonDep getPythonDep() {
    return pythonDep;
  }

  public void setPythonDep(PythonDep pythonDep) {
    this.pythonDep = pythonDep;
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
    hash += (projectPythondepsPK != null ? projectPythondepsPK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof ProjectPythondeps)) {
      return false;
    }
    ProjectPythondeps other = (ProjectPythondeps) object;
    if ((this.projectPythondepsPK == null && other.projectPythondepsPK != null)
            || (this.projectPythondepsPK != null && !this.projectPythondepsPK.
            equals(other.projectPythondepsPK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.pythonDeps.ProjectPythondeps[ projectPythondepsPK="
            + projectPythondepsPK + " ]";
  }

}
