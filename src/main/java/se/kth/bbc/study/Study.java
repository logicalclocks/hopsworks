package se.kth.bbc.study;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
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
  @NamedQuery(name = "Study.findByOwner",
          query = "SELECT t FROM Study t WHERE t.owner = :owner"),
  @NamedQuery(name = "Study.findByCreated",
          query = "SELECT t FROM Study t WHERE t.created = :created"),
  @NamedQuery(name = "Study.findByEthicalStatus",
          query
          = "SELECT t FROM Study t WHERE t.ethicalStatus = :ethicalStatus"),
  @NamedQuery(name = "Study.findByRetentionPeriod",
          query
          = "SELECT t FROM Study t WHERE t.retentionPeriod = :retentionPeriod"),
  @NamedQuery(name = "Study.countStudyByOwner",
          query
          = "SELECT count(t) FROM Study t WHERE t.owner = :owner"),
  @NamedQuery(name = "Study.findByOwnerAndName",
          query
          = "SELECT t FROM Study t WHERE t.owner = :owner AND t.name = :name")})
public class Study implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  
  @Column(name = "deleted")
  private Boolean deleted;
  
  @OneToMany(mappedBy = "study_id")
  private Collection<Samplecollection> samplecollectionCollection;
  
  @OneToOne(cascade = CascadeType.ALL,
          mappedBy = "study_id")
  private StudyMeta studyMeta;
  
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 128)
  @Column(name = "studyname")
  private String name;
  
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "username")
  private String owner;
  
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
  
  @Column(name = "archived")
  private boolean archived;

  public Study() {
  }

  public Study(String name) {
    this.name = name;
  }

  public Study(String name, String username, Date timestamp) {
    this.name = name;
    this.owner = username;
    this.created = timestamp;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
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

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public Date getRetentionPeriod() {
    return retentionPeriod;
  }

  public void setRetentionPeriod(Date retentionPeriod) {
    this.retentionPeriod = retentionPeriod;
  }

  public StudyMeta getStudyMeta() {
    return studyMeta;
  }

  public void setStudyMeta(StudyMeta studyMeta) {
    this.studyMeta = studyMeta;
  }

  public boolean getArchived() {
    return archived;
  }
  
  public void setArchived(boolean archived) {
    this.archived = archived;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.study.Study[ name=" + name + " ]";
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

  public Study(Integer id) {
    this.id = id;
  }

  public Study(Integer id, String studyname) {
    this.id = id;
    this.name = studyname;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Boolean getDeleted() {
    return deleted;
  }

  public void setDeleted(Boolean deleted) {
    this.deleted = deleted;
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
    if (!(object instanceof Study)) {
      return false;
    }
    Study other = (Study) object;
    if ((this.id == null && other.id != null) ||
            (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

}
