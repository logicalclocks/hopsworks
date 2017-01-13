package io.hops.hopsworks.common.dao.kafka;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class CsrDTO implements Serializable {

  private String caPubCert;
  private String pubAgentCert;

  public CsrDTO() {
  }

  public CsrDTO(String caPubCert, String pubAgentCert) {
    this.caPubCert = caPubCert;
    this.pubAgentCert = pubAgentCert;
  }

  public String getCaPubCert() {
    return caPubCert;
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
