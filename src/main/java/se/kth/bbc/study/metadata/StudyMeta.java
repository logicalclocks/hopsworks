package se.kth.bbc.study.metadata;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
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

/**
 *
 * @author stig
 */
@Entity
@Table(name = "STUDY_META")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "StudyMeta.findAll",
          query
          = "SELECT s FROM StudyMeta s"),
  @NamedQuery(name = "StudyMeta.findById",
          query
          = "SELECT s FROM StudyMeta s WHERE s.id = :id"),
  @NamedQuery(name = "StudyMeta.findByStudyname",
          query
          = "SELECT s FROM StudyMeta s WHERE s.studyname = :studyname"),
  @NamedQuery(name = "StudyMeta.findByDescription",
          query
          = "SELECT s FROM StudyMeta s WHERE s.description = :description")})
public class StudyMeta implements Serializable {
  private static final long serialVersionUID = 1L;
  @Size(max = 128)
  @Column(name = "id")
  private String id;
  @Id
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 128)
  @Column(name = "studyname")
  private String studyname;
  @Size(max = 2000)
  @Column(name = "description")
  private String description;
  @OneToMany(cascade = CascadeType.ALL,
          mappedBy = "studyMeta")
  private Collection<StudyDesign> studyDesignCollection;
  @OneToMany(cascade = CascadeType.ALL,
          mappedBy = "studyMeta")
  private Collection<StudyInclusionCriteria> studyInclusionCriteriaCollection;

  public StudyMeta() {
  }

  public StudyMeta(String studyname) {
    this.studyname = studyname;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getStudyname() {
    return studyname;
  }

  public void setStudyname(String studyname) {
    this.studyname = studyname;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<StudyDesign> getStudyDesignCollection() {
    return studyDesignCollection;
  }

  public void setStudyDesignCollection(
          Collection<StudyDesign> studyDesignCollection) {
    this.studyDesignCollection = studyDesignCollection;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<StudyInclusionCriteria> getStudyInclusionCriteriaCollection() {
    return studyInclusionCriteriaCollection;
  }

  public void setStudyInclusionCriteriaCollection(
          Collection<StudyInclusionCriteria> studyInclusionCriteriaCollection) {
    this.studyInclusionCriteriaCollection = studyInclusionCriteriaCollection;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (studyname != null ? studyname.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof StudyMeta)) {
      return false;
    }
    StudyMeta other = (StudyMeta) object;
    if ((this.studyname == null && other.studyname != null) ||
            (this.studyname != null && !this.studyname.equals(other.studyname))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.study.metadata.StudyMeta[ studyname=" + studyname + " ]";
  }
  
}
