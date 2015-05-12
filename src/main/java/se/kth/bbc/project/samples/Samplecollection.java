package se.kth.bbc.project.samples;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.codehaus.jackson.annotate.JsonIgnore;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.metadata.CollectionTypeProjectDesignEnum;
import se.kth.bbc.security.ua.model.User;

/**
 *
 * @author stig
 */
@Entity
@Table(name = "samplecollections")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Samplecollection.findAll",
          query
          = "SELECT s FROM Samplecollection s"),
  @NamedQuery(name = "Samplecollection.findById",
          query
          = "SELECT s FROM Samplecollection s WHERE s.id = :id"),
  @NamedQuery(name = "Samplecollection.findByAcronym",
          query
          = "SELECT s FROM Samplecollection s WHERE s.acronym = :acronym"),
  @NamedQuery(name = "Samplecollection.findByName",
          query
          = "SELECT s FROM Samplecollection s WHERE s.name = :name"),
  @NamedQuery(name = "Samplecollection.findByDescription",
          query
          = "SELECT s FROM Samplecollection s WHERE s.description = :description"),
  @NamedQuery(name = "Samplecollection.findByProject",
          query
          = "SELECT s FROM Samplecollection s WHERE s.project = :project")})
public class Samplecollection implements Serializable {

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
  @Size(min = 1,
          max = 255)
  @Column(name = "acronym",
          unique = true)
  private String acronym;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 1024)
  @Column(name = "name")
  private String name;
  @Size(max = 2000)
  @Column(name = "description")
  private String description;
  @JoinColumn(name = "project_id",
          referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Project project;
  @JoinColumn(name = "contact",
          referencedColumnName = "email")
  @ManyToOne(optional = false)
  private User contact;
  @ManyToMany
  @JoinTable(
          name = "samplecollection_disease",
          joinColumns = {
            @JoinColumn(name = "collection_id",
                    referencedColumnName = "id")},
          inverseJoinColumns = {
            @JoinColumn(name = "disease_id",
                    referencedColumnName = "id")})
  private Collection<Disease> diseases;
  @ElementCollection(targetClass = CollectionTypeProjectDesignEnum.class)
  @CollectionTable(name = "samplecollection_type",
          joinColumns = @JoinColumn(name = "collection_id",
                  referencedColumnName = "id"))
  @Column(name = "type")
  @Enumerated(EnumType.STRING)
  private List<CollectionTypeProjectDesignEnum> collectionTypeList;
  @OneToMany(cascade = CascadeType.ALL,
          mappedBy = "samplecollectionId")
  private Collection<Sample> sampleCollection;

  public Samplecollection() {
  }

  public Samplecollection(String id) {
    this.id = id;
  }

  public Samplecollection(String id, String acronym, String name) {
    this.id = id;
    this.acronym = acronym;
    this.name = name;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getAcronym() {
    return acronym;
  }

  public void setAcronym(String acronym) {
    this.acronym = acronym;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Project getProject() {
    return project;
  }

  public void setProject(Project project) {
    this.project = project;
  }

  public User getContact() {
    return contact;
  }

  public void setContact(User contact) {
    this.contact = contact;
  }

  public Collection<Disease> getDiseases() {
    return diseases;
  }

  public void setDiseases(Collection<Disease> diseases) {
    this.diseases = diseases;
  }

  public List<CollectionTypeProjectDesignEnum> getCollectionTypeList() {
    return collectionTypeList;
  }

  public void setCollectionTypeList(
          List<CollectionTypeProjectDesignEnum> collectionTypeList) {
    this.collectionTypeList = collectionTypeList;
  }
    
  @XmlTransient
  @JsonIgnore
  public Collection<Sample> getSampleCollection() {
    return sampleCollection;
  }

  public void setSampleCollection(Collection<Sample> sampleCollection) {
    this.sampleCollection = sampleCollection;
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
    if (!(object instanceof Samplecollection)) {
      return false;
    }
    Samplecollection other = (Samplecollection) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.project.samples.Samplecollection[ id=" + id + " ]";
  }

}
