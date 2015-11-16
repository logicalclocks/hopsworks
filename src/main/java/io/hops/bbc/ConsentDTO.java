/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.bbc;

import java.io.Serializable;
import java.util.Objects;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ConsentDTO implements Serializable {

  private String path;
  private String consentType;
  private String consentStatus;

  public ConsentDTO() {
  }

  public ConsentDTO(String path) {
    this.path = path;
    this.consentType = ConsentType.UNDEFINED.toString();
    this.consentStatus = ConsentStatus.UNDEFINED.toString();
  }

  public ConsentDTO(String name, ConsentType consentType, ConsentStatus consentStatus) {
    this.path = name;
    this.consentType = consentType.toString();
    this.consentStatus = consentStatus.toString();
  }

  public String getConsentStatus() {
    return consentStatus;
  }

  public void setConsentStatus(String consentStatus) {
    this.consentStatus = consentStatus;
  }

  public String getConsentType() {
    return consentType;
  }

  public void setConsentType(String consentType) {
    this.consentType = consentType;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String name) {
    this.path = name;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ConsentDTO)) {
      return false;
    }
    ConsentDTO other = (ConsentDTO) obj;
    return !((this.path == null && other.path != null) || (this.path != null 
        && (this.path.compareToIgnoreCase(other.path) != 0)));
  }

  @Override
  public int hashCode() {
    int hash = 5;
    hash = 97 * hash + Objects.hashCode(this.path);
    return hash;
  }

  @Override
  public String toString() {
    return "Path: " + path + " ; type: " + consentType + " ; status: " + consentStatus;
  }

  
}
