/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.tf;

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

/**
 *
 * @author jdowling
 */
@Entity
@Table(name = "tf_jobs", catalog = "hopsworks", schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "TfJobs.findAll", query = "SELECT t FROM TfJobs t"),
  @NamedQuery(name = "TfJobs.findById", query = "SELECT t FROM TfJobs t WHERE t.id = :id"),
  @NamedQuery(name = "TfJobs.findByCluster", query = "SELECT t FROM TfJobs t WHERE t.clusterId.id = :id"),
  @NamedQuery(name = "TfJobs.findByName", query = "SELECT t FROM TfJobs t WHERE t.name = :name"),
  @NamedQuery(name = "TfJobs.findByType", query = "SELECT t FROM TfJobs t WHERE t.type = :type")})
public class TfJobs implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 150)
  @Column(name = "name")
  private String name;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 50)
  @Column(name = "type")
  private String type;
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "jobId")
  private Collection<TfTasks> tfTasksCollection;
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "jobId")
  private Collection<TfExecutions> tfExecutionsCollection;
  @JoinColumn(name = "cluster_id", referencedColumnName = "id")
  @ManyToOne(optional = false)
  private TfClusters clusterId;

  public TfJobs() {
  }

  public TfJobs(Integer id) {
    this.id = id;
  }

  public TfJobs(Integer id, String name, String type) {
    this.id = id;
    this.name = name;
    this.type = type;
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

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<TfTasks> getTfTasksCollection() {
    return tfTasksCollection;
  }

  public void setTfTasksCollection(Collection<TfTasks> tfTasksCollection) {
    this.tfTasksCollection = tfTasksCollection;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<TfExecutions> getTfExecutionsCollection() {
    return tfExecutionsCollection;
  }

  public void setTfExecutionsCollection(Collection<TfExecutions> tfExecutionsCollection) {
    this.tfExecutionsCollection = tfExecutionsCollection;
  }

  public TfClusters getClusterId() {
    return clusterId;
  }

  public void setClusterId(TfClusters clusterId) {
    this.clusterId = clusterId;
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
    if (!(object instanceof TfJobs)) {
      return false;
    }
    TfJobs other = (TfJobs) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.tf.TfJobs[ id=" + id + " ]";
  }
  
}
