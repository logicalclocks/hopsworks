/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.kafka;

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
