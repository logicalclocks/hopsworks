package se.kth.bbc.project.samples;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author stig
 */
@Entity
@Table(name = "vangelis_kthfs.diseases")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Disease.findAll",
          query
          = "SELECT d FROM Disease d"),
  @NamedQuery(name = "Disease.findById",
          query
          = "SELECT d FROM Disease d WHERE d.id = :id"),
  @NamedQuery(name = "Disease.findByOntologyName",
          query
          = "SELECT d FROM Disease d WHERE d.ontologyName = :ontologyName"),
  @NamedQuery(name = "Disease.findByOntologyVersion",
          query
          = "SELECT d FROM Disease d WHERE d.ontologyVersion = :ontologyVersion"),
  @NamedQuery(name = "Disease.findByOntologyCode",
          query
          = "SELECT d FROM Disease d WHERE d.ontologyCode = :ontologyCode"),
  @NamedQuery(name = "Disease.findByOntologyDescription",
          query
          = "SELECT d FROM Disease d WHERE d.ontologyDescription = :ontologyDescription"),
  @NamedQuery(name = "Disease.findByExplanation",
          query
          = "SELECT d FROM Disease d WHERE d.explanation = :explanation")})
public class Disease implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Size(max = 255)
  @Column(name = "ontology_name")
  private String ontologyName;
  @Size(max = 255)
  @Column(name = "ontology_version")
  private String ontologyVersion;
  @Size(max = 255)
  @Column(name = "ontology_code")
  private String ontologyCode;
  @Size(max = 2000)
  @Column(name = "ontology_description")
  private String ontologyDescription;
  @Size(max = 2000)
  @Column(name = "explanation")
  private String explanation;

  public Disease() {
  }

  public Disease(Integer id) {
    this.id = id;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getOntologyName() {
    return ontologyName;
  }

  public void setOntologyName(String ontologyName) {
    this.ontologyName = ontologyName;
  }

  public String getOntologyVersion() {
    return ontologyVersion;
  }

  public void setOntologyVersion(String ontologyVersion) {
    this.ontologyVersion = ontologyVersion;
  }

  public String getOntologyCode() {
    return ontologyCode;
  }

  public void setOntologyCode(String ontologyCode) {
    this.ontologyCode = ontologyCode;
  }

  public String getOntologyDescription() {
    return ontologyDescription;
  }

  public void setOntologyDescription(String ontologyDescription) {
    this.ontologyDescription = ontologyDescription;
  }

  public String getExplanation() {
    return explanation;
  }

  public void setExplanation(String explanation) {
    this.explanation = explanation;
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
    if (!(object instanceof Disease)) {
      return false;
    }
    Disease other = (Disease) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.project.samples.Disease[ id=" + id + " ]";
  }

}
