/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.bbc;

import java.io.Serializable;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ConsentDTOs implements Serializable {

  private List<ConsentDTO> consents;

  public ConsentDTOs(List<ConsentDTO> consents) {
    this.consents = consents;
  }

  public ConsentDTOs() {
  }

  public List<ConsentDTO> getConsents() {
    return consents;
  }

  public void setConsents(List<ConsentDTO> consents) {
    this.consents = consents;
  }
  
}
