package se.kth.bbc.study;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.codehaus.jackson.annotate.JsonIgnore;
import se.kth.bbc.study.metadata.StudyMeta;
import se.kth.bbc.study.samples.Samplecollection;

/**
 *
 * @author roshan
 */
@Entity
@Table(name = "study")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Study.findAll",
          query = "SELECT t FROM Study t"),
  @NamedQuery(name = "Study.findByName",
          query = "SELECT t FROM Study t WHERE t.name = :name"),
  @NamedQuery(name = "Study.findByUsername",
          query = "SELECT t FROM Study t WHERE t.username = :username"),
  @NamedQuery(name = "Study.findByCreated",
          query = "SELECT t FROM Study t WHERE t.created = :created"),
  @NamedQuery(name = "Study.findByEthicalStatus",
          query
          = "SELECT t FROM Study t WHERE t.ethicalStatus = :ethicalStatus"),
  @NamedQuery(name = "Study.findByRetentionPeriod",
          query
          = "SELECT t FROM Study t WHERE t.retentionPeriod = :retentionPeriod"),
  @NamedQuery(name = "Study.findOwner",
          query = "SELECT t.username FROM Study t WHERE t.name = :name"),
  @NamedQuery(name = "Study.countStudyByOwner",
          query
          = "SELECT count(t.name) FROM Study t WHERE t.username = :username")})
public class Study implements Serializable {

  @OneToMany(mappedBy = "study")
  private Collection<Samplecollection> samplecollectionCollection;
  @OneToOne(cascade = CascadeType.ALL,
          mappedBy = "study")
  private StudyMeta studyMeta;
  private static final long serialVersionUID = 1L;
  @Id
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 128)
  @Column(name = "name")
  private String name;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "username")
  private String username;
  @Column(name = "retention_period")
  @Temporal(TemporalType.DATE)
  private Date retentionPeriod;
  @Basic(optional = false)
  @NotNull
  @Column(name = "created")
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;
  @NotNull
  @Size(min = 1,
          max = 30)
  @Column(name = "ethical_status")
  private String ethicalStatus;

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public Study() {
  }

  public Study(String name) {
    this.name = name;
  }

  public Study(String name, String username, Date timestamp) {
    this.name = name;
    this.username = username;
    this.created = timestamp;
  }

  public String getEthicalStatus() {
    return ethicalStatus;
  }

  public void setEthicalStatus(String ethicalStatus) {
    this.ethicalStatus = ethicalStatus;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public Date getRetentionPeriod() {
    return retentionPeriod;
  }

  public void setRetentionPeriod(Date retentionPeriod) {
    this.retentionPeriod = retentionPeriod;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (name != null ? name.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof Study)) {
      return false;
    }
    Study other = (Study) object;
    return !((this.name == null && other.name != null) || (this.name != null
            && !this.name.equals(other.name)));
  }

  @Override
  public String toString() {
    return "se.kth.bbc.study.Study[ name=" + name + " ]";
  }

  public StudyMeta getStudyMeta() {
    return studyMeta;
  }

  public void setStudyMeta(StudyMeta studyMeta) {
    this.studyMeta = studyMeta;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<Samplecollection> getSamplecollectionCollection() {
    return samplecollectionCollection;
  }

  public void setSamplecollectionCollection(
          Collection<Samplecollection> samplecollectionCollection) {
    this.samplecollectionCollection = samplecollectionCollection;
  }

}
