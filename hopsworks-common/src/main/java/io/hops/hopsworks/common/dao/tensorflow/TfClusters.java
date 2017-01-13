package io.hops.hopsworks.common.dao.tensorflow;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
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
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;

@Entity
@Table(name = "tf_clusters",
        catalog = "hopsworks",
        schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "TfClusters.findAll",
          query = "SELECT t FROM TfClusters t"),
  @NamedQuery(name = "TfClusters.findById",
          query = "SELECT t FROM TfClusters t WHERE t.id = :id"),
  @NamedQuery(name = "TfClusters.findByProjectId",
          query = "SELECT t FROM TfClusters t WHERE t.projectId = :projectId"),
  @NamedQuery(name = "TfClusters.findByOwner",
          query = "SELECT t FROM TfClusters t WHERE t.creator = :email"),
  @NamedQuery(name = "TfClusters.findByName",
          query = "SELECT t FROM TfClusters t WHERE t.name = :name")})
public class TfClusters implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 150)
  @Column(name = "name")
  private String name;
  @OneToMany(cascade = CascadeType.ALL,
          mappedBy = "clusterId")
  private Collection<TfExecutions> tfExecutionsCollection;
  @JoinColumn(name = "creator",
          referencedColumnName = "email")
  @ManyToOne(optional = false)
  private Users creator;
  @JoinColumn(name = "project_id",
          referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Project projectId;
  @OneToMany(cascade = CascadeType.ALL,
          mappedBy = "clusterId")
  private Collection<TfFifoQueue> tfFifoQueueCollection;
  @OneToMany(cascade = CascadeType.ALL,
          mappedBy = "clusterId")
  private Collection<TfJobs> tfJobsCollection;

  public TfClusters() {
  }

  public TfClusters(Integer id) {
    this.id = id;
  }

  public TfClusters(Integer id, String name) {
    this.id = id;
    this.name = name;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<TfExecutions> getTfExecutionsCollection() {
    return tfExecutionsCollection;
  }

  public void setTfExecutionsCollection(
          Collection<TfExecutions> tfExecutionsCollection) {
    this.tfExecutionsCollection = tfExecutionsCollection;
  }

  public Users getCreator() {
    return creator;
  }

  public void setCreator(Users creator) {
    this.creator = creator;
  }

  public Project getProjectId() {
    return projectId;
  }

  public void setProjectId(Project projectId) {
    this.projectId = projectId;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<TfFifoQueue> getTfFifoQueueCollection() {
    return tfFifoQueueCollection;
  }

  public void setTfFifoQueueCollection(
          Collection<TfFifoQueue> tfFifoQueueCollection) {
    this.tfFifoQueueCollection = tfFifoQueueCollection;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<TfJobs> getTfJobsCollection() {
    return tfJobsCollection;
  }

  public void setTfJobsCollection(Collection<TfJobs> tfJobsCollection) {
    this.tfJobsCollection = tfJobsCollection;
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
    if (!(object instanceof TfClusters)) {
      return false;
    }
    TfClusters other = (TfClusters) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.tf.TfClusters[ id=" + id + " ]";
  }

}
