package io.hops.hopsworks.common.dao.pythonDeps;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.codehaus.jackson.annotate.JsonIgnore;

@Entity
@Table(name = "anaconda_repo",
        catalog = "hopsworks",
        schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "AnacondaRepo.findAll",
          query
          = "SELECT a FROM AnacondaRepo a"),
  @NamedQuery(name = "AnacondaRepo.findById",
          query
          = "SELECT a FROM AnacondaRepo a WHERE a.id = :id"),
  @NamedQuery(name = "AnacondaRepo.findByUrl",
          query
          = "SELECT a FROM AnacondaRepo a WHERE a.url = :url")})
public class AnacondaRepo implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "url")
  private String url;
  @OneToMany(cascade = CascadeType.ALL,
          mappedBy = "repoUrl")
  private Collection<PythonDep> pythonDepCollection;

  public AnacondaRepo() {
  }

  public AnacondaRepo(Integer id) {
    this.id = id;
  }

  public AnacondaRepo(Integer id, String url) {
    this.id = id;
    this.url = url;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<PythonDep> getPythonDepCollection() {
    return pythonDepCollection;
  }

  public void setPythonDepCollection(Collection<PythonDep> pythonDepCollection) {
    this.pythonDepCollection = pythonDepCollection;
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
    if (!(object instanceof AnacondaRepo)) {
      return false;
    }
    AnacondaRepo other = (AnacondaRepo) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.pythonDeps.AnacondaRepo[ id=" + id
            + " ]";
  }

}
