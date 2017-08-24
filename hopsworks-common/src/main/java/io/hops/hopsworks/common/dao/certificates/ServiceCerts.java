package io.hops.hopsworks.common.dao.certificates;

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
@Table(name = "service_certs",
        catalog = "hopsworks",
        schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "ServiceCerts.findAll",
          query = "SELECT s FROM ServiceCerts s"),
  @NamedQuery(name = "ServiceCerts.findByServiceName",
          query
          = "SELECT s FROM ServiceCerts s WHERE s.serviceName = :serviceName")})

public class ServiceCerts implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 20)
  @Column(name = "service_name")
  private String serviceName;
  @Lob
  @Column(name = "service_key")
  private byte[] serviceKey;
  @Lob
  @Column(name = "service_cert")
  private byte[] serviceCert;
  @NotNull
  @Column(name = "cert_password")
  private String certificatePassword;

  public ServiceCerts() {
  }

  public ServiceCerts(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public byte[] getServiceKey() {
    return serviceKey;
  }

  public void setServiceKey(byte[] serviceKey) {
    this.serviceKey = serviceKey;
  }

  public byte[] getServiceCert() {
    return serviceCert;
  }

  public void setServiceCert(byte[] serviceCert) {
    this.serviceCert = serviceCert;
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
    hash += (serviceName != null ? serviceName.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof ServiceCerts)) {
      return false;
    }
    ServiceCerts other = (ServiceCerts) object;
    if ((this.serviceName == null && other.serviceName != null)
            || (this.serviceName != null && !this.serviceName.equals(
                    other.serviceName))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.hopsworks.certificates.ServiceCerts[ serviceName="
            + serviceName + " ]";
  }

}
