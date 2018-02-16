/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package io.hops.hopsworks.common.dao.dela.certs;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "hopssite_cluster_certs",
  catalog = "hopsworks",
  schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "ClusterCertificate.findByName",
    query = "SELECT c FROM ClusterCertificate c WHERE c.clusterName = :clusterName"),
  @NamedQuery(name = "ClusterCertificate.findAll",
    query = "SELECT c FROM ClusterCertificate c")})
public class ClusterCertificate implements Serializable {
  private static final long serialVersionUID = 1L;
  public static final String QUERY_BY_NAME = "ClusterCertificate.findByName";
  public static final String CLUSTER_NAME = "clusterName";
  
  @Id
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
    max = 129)
  @Column(name = "cluster_name")
  private String clusterName;
  @Lob
  @Column(name = "cluster_key")
  private byte[] clusterKey;
  @Lob
  @Column(name = "cluster_cert")
  private byte[] clusterCert;
  @NotNull
  @Column(name = "cert_password")
  private String certificatePassword;

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public byte[] getClusterKey() {
    return clusterKey;
  }

  public void setClusterKey(byte[] clusterKey) {
    this.clusterKey = clusterKey;
  }

  public byte[] getClusterCert() {
    return clusterCert;
  }

  public void setClusterCert(byte[] clusterCert) {
    this.clusterCert = clusterCert;
  }

  public String getCertificatePassword() {
    return certificatePassword;
  }

  public void setCertificatePassword(String certificatePassword) {
    this.certificatePassword = certificatePassword;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (clusterName != null ? clusterName.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof ClusterCertificate)) {
      return false;
    }
    ClusterCertificate other = (ClusterCertificate) object;
    if ((this.clusterName == null && other.clusterName != null)
      || (this.clusterName != null && !this.clusterName.equals(other.clusterName))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.dela.dao.certs.ClusterCerts[ clusterName=" + clusterName + " ]";
  }
}
