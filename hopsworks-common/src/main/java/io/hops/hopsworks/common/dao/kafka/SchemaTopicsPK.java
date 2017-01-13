package io.hops.hopsworks.common.dao.kafka;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Embeddable
public class SchemaTopicsPK implements Serializable {

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "name")
  private String name;
  @Basic(optional = false)
  @NotNull
  @Column(name = "version")
  private int version;

  public SchemaTopicsPK() {
  }

  public SchemaTopicsPK(String name, int version) {
    this.name = name;
    this.version = version;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (name != null ? name.hashCode() : 0);
    hash += (int) version;
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof SchemaTopicsPK)) {
      return false;
    }
    SchemaTopicsPK other = (SchemaTopicsPK) object;
    if ((this.name == null && other.name != null) || (this.name != null
            && !this.name.equals(other.name))) {
      return false;
    }
    if (this.version != other.version) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.kafka.SchemaTopicsPK[ name=" + name + ", version=" + version
            + " ]";
  }

}
