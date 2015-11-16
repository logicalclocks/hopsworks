/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.project;

import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ConsentsDTO {

  private List<String> registeredConsents;
  private List<String> unRegisteredConsents;
  
  public ConsentsDTO() {
  }

  public List<String> getRegisteredConsents() {
    return registeredConsents;
  }

  public List<String> getUnRegisteredConsents() {
    return unRegisteredConsents;
  }

  public void setRegisteredConsents(List<String> registeredConsents) {
    this.registeredConsents = registeredConsents;
  }

  public void setUnRegisteredConsents(List<String> unRegisteredConsents) {
    this.unRegisteredConsents = unRegisteredConsents;
  }
  
}
