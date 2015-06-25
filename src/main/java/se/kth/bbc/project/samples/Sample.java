package se.kth.bbc.project.samples;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
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
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
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
@Table(name = "vangelis_kthfs.samples")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Sample.findAll",
          query
          = "SELECT s FROM Sample s"),
  @NamedQuery(name = "Sample.findById",
          query
          = "SELECT s FROM Sample s WHERE s.id = :id"),
  @NamedQuery(name = "Sample.findBySampledTime",
          query
          = "SELECT s FROM Sample s WHERE s.sampledTime = :sampledTime")})
public class Sample implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "id")
  private String id;
  @Basic(optional = false)
  @NotNull
  @Column(name = "sampled_time")
  @Temporal(TemporalType.TIMESTAMP)
  private Date sampledTime;
  @JoinColumn(name = "anatomical_site",
          referencedColumnName = "id")
  @ManyToOne
  private AnatomicalPart anatomicalSite;
  @OneToMany(mappedBy = "parentId")
  private Collection<Sample> childSamplesCollection;
  @JoinColumn(name = "parent_id",
          referencedColumnName = "id")
  @ManyToOne
  private Sample parentId;
  @JoinColumn(name = "samplecollection_id",
          referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Samplecollection samplecollectionId;
  @ElementCollection(targetClass = MaterialTypeEnum.class)
  @CollectionTable(name = "sample_material",
          joinColumns = @JoinColumn(name = "sample_id",
                  referencedColumnName = "id"))
  @Column(name = "type")
  @Enumerated(EnumType.STRING)
  private List<MaterialTypeEnum> materialTypeList;

  public Sample() {
  }

  public Sample(String id) {
    this.id = id;
  }

  public Sample(String id, Date sampledTime) {
    this.id = id;
    this.sampledTime = sampledTime;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Date getSampledTime() {
    return sampledTime;
  }

  public void setSampledTime(Date sampledTime) {
    this.sampledTime = sampledTime;
  }

  public AnatomicalPart getAnatomicalSite() {
    return anatomicalSite;
  }

  public void setAnatomicalSite(AnatomicalPart anatomicalSite) {
    this.anatomicalSite = anatomicalSite;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<Sample> getChildSamplesCollection() {
    return childSamplesCollection;
  }

  public void setChildSamplesCollection(
          Collection<Sample> childSamplesCollection) {
    this.childSamplesCollection = childSamplesCollection;
  }

  public Sample getParentId() {
    return parentId;
  }

  public void setParentId(Sample parentId) {
    this.parentId = parentId;
  }

  public Samplecollection getSamplecollectionId() {
    return samplecollectionId;
  }

  public void setSamplecollectionId(Samplecollection samplecollectionId) {
    this.samplecollectionId = samplecollectionId;
  }

  public List<MaterialTypeEnum> getMaterialTypeList() {
    return materialTypeList;
  }

  public void setMaterialTypeList(
          List<MaterialTypeEnum> materialTypeList) {
    this.materialTypeList = materialTypeList;
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
    if (!(object instanceof Sample)) {
      return false;
    }
    Sample other = (Sample) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.project.samples.Sample[ id=" + id + " ]";
  }

}
