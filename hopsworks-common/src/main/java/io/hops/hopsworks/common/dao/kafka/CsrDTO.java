package io.hops.hopsworks.common.dao.kafka;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class CsrDTO implements Serializable {

  private String caPubCert;
  private String intermediateCaPubCert;
  private String pubAgentCert;
  private String hadoopHome;

  public CsrDTO() {
  }

  public CsrDTO(String caPubCert, String pubAgentCert, String hadoopHome) {
    this.caPubCert = caPubCert;
    this.pubAgentCert = pubAgentCert;
    this.hadoopHome = hadoopHome;
  }

  public CsrDTO(String caPubCert, String intermediateCaPubCert, String pubAgentCert, String hadoopHome) {
    this.caPubCert = caPubCert;
    this.intermediateCaPubCert = intermediateCaPubCert;
    this.pubAgentCert = pubAgentCert;
    this.hadoopHome = hadoopHome;
  }

  public String getHadoopHome() {
    return hadoopHome;
  }

  public void setHadoopHome(String hadoopHome) {
    this.hadoopHome = hadoopHome;
  }

  public String getCaPubCert() {
    return caPubCert;
  }

  public String getIntermediateCaPubCert() {
    return intermediateCaPubCert;
  }

  public void setIntermediateCaPubCert(String intermediateCaPubCert) {
    this.intermediateCaPubCert = intermediateCaPubCert;
  }

  public String getPubAgentCert() {
    return pubAgentCert;
  }

  public void setCaPubCert(String caPubCert) {
    this.caPubCert = caPubCert;
  }

  public void setPubAgentCert(String pubAgentCert) {
    this.pubAgentCert = pubAgentCert;
  }

}
