package io.hops.hopsworks.api.models.dto;

import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;

import java.util.HashMap;

@XmlRootElement
public class ModelResult {

  @XmlAnyAttribute
  private HashMap<QName, Double> attributes;

  public ModelResult() {
    //Needed for JAXB
  }

  public HashMap<QName, Double> getAttributes() {
    return this.attributes;
  }

  public void setAttributes(HashMap<QName, Double> attributes) {
    this.attributes = attributes;
  }
}
