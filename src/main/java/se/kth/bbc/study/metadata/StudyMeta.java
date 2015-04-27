package se.kth.bbc.study.metadata;

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
import se.kth.bbc.study.Study;

/**
 *
 * @author stig
 */
@Entity
@Table(name = "study_meta")
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
  @NotNull
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
  @JoinColumn(name = "studyname",
          referencedColumnName = "name",
          insertable
          = false,
          updatable = false)
  @OneToOne(optional = false)
  private Study study;
  @ElementCollection(targetClass = CollectionTypeStudyDesignEnum.class)
  @CollectionTable(name = "study_design",
          joinColumns = @JoinColumn(name = "study_id",
                  referencedColumnName = "id"))
  @Column(name = "design")
  @Enumerated(EnumType.STRING)
  private List<CollectionTypeStudyDesignEnum> studyDesignList;
  @ElementCollection(targetClass = InclusionCriteriumEnum.class)
  @CollectionTable(name = "study_inclusion_criteria",
          joinColumns = @JoinColumn(name = "study_id",
                  referencedColumnName = "id"))
  @Column(name = "criterium")
  @Enumerated(EnumType.STRING)
  private List<InclusionCriteriumEnum> inclusionCriteriaList;

  public List<CollectionTypeStudyDesignEnum> getStudyDesignList() {
    return studyDesignList;
  }

  public void setStudyDesignList(
          List<CollectionTypeStudyDesignEnum> studyDesignList) {
    this.studyDesignList = studyDesignList;
  }

  public List<InclusionCriteriumEnum> getInclusionCriteriaList() {
    return inclusionCriteriaList;
  }

  public void setInclusionCriteriaList(
          List<InclusionCriteriumEnum> inclusionCriteriaList) {
    this.inclusionCriteriaList = inclusionCriteriaList;
  }

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

  public Study getStudy() {
    return study;
  }

  public void setStudy(Study study) {
    this.study = study;
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
    if ((this.studyname == null && other.studyname != null) || (this.studyname
            != null && !this.studyname.equals(other.studyname))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.study.metadata.StudyMeta[ studyname=" + studyname + " ]";
  }

}
