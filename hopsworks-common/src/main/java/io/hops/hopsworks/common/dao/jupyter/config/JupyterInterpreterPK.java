package io.hops.hopsworks.common.dao.jupyter.config;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Embeddable
public class JupyterInterpreterPK implements Serializable {

  @Basic(optional = false)
  @NotNull
  @Column(name = "port")
  private int port;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "name")
  private String name;

  public JupyterInterpreterPK() {
  }

  public JupyterInterpreterPK(int port, String name) {
    this.port = port;
    this.name = name;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (int) port;
    hash += (name != null ? name.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof JupyterInterpreterPK)) {
      return false;
    }
    JupyterInterpreterPK other = (JupyterInterpreterPK) object;
    if (this.port != other.port) {
      return false;
    }
    if ((this.name == null && other.name != null) ||
            (this.name != null && !this.name.equals(other.name))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.jupyter.config.JupyterInterpreterPK[ port=" +
            port + ", name=" + name + " ]";
  }

}
