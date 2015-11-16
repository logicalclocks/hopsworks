/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.project.privacy.model;

import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ConsentsDTO {

  private List<Consents> consents;
  
  public ConsentsDTO() {
  }

  public List<Consents> getConsents() {
    return consents;
  }

  public void setConsents(List<Consents> consents) {
    this.consents = consents;
  }
  
}
