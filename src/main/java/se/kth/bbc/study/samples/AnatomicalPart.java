package se.kth.bbc.study.samples;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.codehaus.jackson.annotate.JsonIgnore;

/**
 *
 * @author stig
 */
@Entity
@Table(name = "ANATOMICAL_PARTS")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "AnatomicalPart.findAll",
          query
          = "SELECT a FROM AnatomicalPart a"),
  @NamedQuery(name = "AnatomicalPart.findById",
          query
          = "SELECT a FROM AnatomicalPart a WHERE a.id = :id"),
  @NamedQuery(name = "AnatomicalPart.findByOntologyName",
          query
          = "SELECT a FROM AnatomicalPart a WHERE a.ontologyName = :ontologyName"),
  @NamedQuery(name = "AnatomicalPart.findByOntologyVersion",
          query
          = "SELECT a FROM AnatomicalPart a WHERE a.ontologyVersion = :ontologyVersion"),
  @NamedQuery(name = "AnatomicalPart.findByOntologyCode",
          query
          = "SELECT a FROM AnatomicalPart a WHERE a.ontologyCode = :ontologyCode"),
  @NamedQuery(name = "AnatomicalPart.findByOntologyDescription",
          query
          = "SELECT a FROM AnatomicalPart a WHERE a.ontologyDescription = :ontologyDescription"),
  @NamedQuery(name = "AnatomicalPart.findByExplanation",
          query
          = "SELECT a FROM AnatomicalPart a WHERE a.explanation = :explanation")})
public class AnatomicalPart implements Serializable {
  @OneToMany(mappedBy = "anatomicalSite")
  private Collection<Sample> sampleCollection;
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

  public AnatomicalPart() {
  }

  public AnatomicalPart(Integer id) {
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
    if (!(object instanceof AnatomicalPart)) {
      return false;
    }
    AnatomicalPart other = (AnatomicalPart) object;
    if ((this.id == null && other.id != null) ||
            (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.study.samples.AnatomicalPart[ id=" + id + " ]";
  }

  @XmlTransient
  @JsonIgnore
  public Collection<Sample> getSampleCollection() {
    return sampleCollection;
  }

  public void setSampleCollection(Collection<Sample> sampleCollection) {
    this.sampleCollection = sampleCollection;
  }
  
}
