package io.hops.hopsworks.common.dao.tensorflow;

import io.hops.hopsworks.common.dao.host.Host;
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

@Entity
@Table(name = "tf_resources",
        catalog = "hopsworks",
        schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "TfResources.findAll",
          query = "SELECT t FROM TfResources t"),
  @NamedQuery(name = "TfResources.findById",
          query = "SELECT t FROM TfResources t WHERE t.id = :id"),
  @NamedQuery(name = "TfResources.findByDeviceId",
          query = "SELECT t FROM TfResources t WHERE t.deviceId = :deviceId"),
  @NamedQuery(name = "TfResources.findByDevicePath",
          query = "SELECT t FROM TfResources t WHERE t.devicePath = :devicePath"),
  @NamedQuery(name = "TfResources.findByResource",
          query = "SELECT t FROM TfResources t WHERE t.resource = :resource"),
  @NamedQuery(name = "TfResources.findByFree",
          query = "SELECT t FROM TfResources t WHERE t.free = :free")})
public class TfResources implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @NotNull
  @Column(name = "device_id")
  private int deviceId;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "device_path")
  private String devicePath;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 20)
  @Column(name = "resource")
  private String resource;
  @Basic(optional = false)
  @NotNull
  @Column(name = "free")
  private boolean free;
  @OneToMany(cascade = CascadeType.ALL,
          mappedBy = "resourceId")
  private Collection<TfTasks> tfTasksCollection;
  @JoinColumn(name = "host_id",
          referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Host host;
  @OneToMany(cascade = CascadeType.ALL,
          mappedBy = "resourceId")
  private Collection<TfExecutions> tfExecutionsCollection;

  public TfResources() {
  }

  public TfResources(Integer id) {
    this.id = id;
  }

  public TfResources(Integer id, int deviceId, String devicePath,
          String resource, boolean free) {
    this.id = id;
    this.deviceId = deviceId;
    this.devicePath = devicePath;
    this.resource = resource;
    this.free = free;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public int getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(int deviceId) {
    this.deviceId = deviceId;
  }

  public String getDevicePath() {
    return devicePath;
  }

  public void setDevicePath(String devicePath) {
    this.devicePath = devicePath;
  }

  public String getResource() {
    return resource;
  }

  public void setResource(String resource) {
    this.resource = resource;
  }

  public boolean getFree() {
    return free;
  }

  public void setFree(boolean free) {
    this.free = free;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<TfTasks> getTfTasksCollection() {
    return tfTasksCollection;
  }

  public void setTfTasksCollection(Collection<TfTasks> tfTasksCollection) {
    this.tfTasksCollection = tfTasksCollection;
  }

  public Host getHost() {
    return host;
  }

  public void setHost(Host host) {
    this.host = host;
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

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (id != null ? id.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof TfResources)) {
      return false;
    }
    TfResources other = (TfResources) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.tf.TfResources[ id=" + id + " ]";
  }

}
