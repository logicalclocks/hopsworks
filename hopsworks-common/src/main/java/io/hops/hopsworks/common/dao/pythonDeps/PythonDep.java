package io.hops.hopsworks.common.dao.pythonDeps;

import io.hops.hopsworks.common.dao.project.Project;
import java.io.Serializable;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.codehaus.jackson.annotate.JsonIgnore;

@Entity
@Table(name = "python_dep",
        catalog = "hopsworks",
        schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "PythonDep.findAll",
          query
          = "SELECT p FROM PythonDep p"),
  @NamedQuery(name = "PythonDep.findById",
          query
          = "SELECT p FROM PythonDep p WHERE p.id = :id"),
  @NamedQuery(name = "PythonDep.findByDependency",
          query
          = "SELECT p FROM PythonDep p WHERE p.dependency = :dependency"),
  @NamedQuery(name = "PythonDep.findByDependencyAndVersion",
          query
          = "SELECT p FROM PythonDep p WHERE p.dependency = :dependency AND p.version = :version"),
  @NamedQuery(name = "PythonDep.findByVersion",
          query
          = "SELECT p FROM PythonDep p WHERE p.version = :version")})
public class PythonDep implements Serializable {
  
  
  public enum Status {
    INSTALLED,
    INSTALLING,
    FAILED
  }
  

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 128)
  @Column(name = "dependency")
  private String dependency;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 128)
  @Column(name = "version")
  private String version;
  @ManyToMany(mappedBy = "pythonDepCollection")
  private Collection<Project> projectCollection;
  @JoinColumn(name = "repo_url",
          referencedColumnName = "id")
  @ManyToOne(optional = false)
  private AnacondaRepo repoUrl;

  @Enumerated(EnumType.ORDINAL)
  private Status status = Status.INSTALLING;

  public PythonDep() {
  }

  public PythonDep(Integer id) {
    this.id = id;
  }

  public PythonDep(Integer id, String dependency, String version) {
    this.id = id;
    this.dependency = dependency;
    this.version = version;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getDependency() {
    return dependency;
  }

  public void setDependency(String dependency) {
    this.dependency = dependency;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<Project> getProjectCollection() {
    return projectCollection;
  }

  public void setProjectCollection(Collection<Project> projectCollection) {
    this.projectCollection = projectCollection;
  }

  public AnacondaRepo getRepoUrl() {
    return repoUrl;
  }

  public void setRepoUrl(AnacondaRepo repoUrl) {
    this.repoUrl = repoUrl;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public Status getStatus() {
    return status;
  }

  
  @Override
  public int hashCode() {
    int hash = 0;
    hash += (id != null ? id.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof PythonDep)) {
      return false;
    }
    PythonDep other = (PythonDep) object;
    if ((this.id == null && other.id != null) ||
            (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.pythonDeps.PythonDep[ id=" + id + " ]";
  }


}
