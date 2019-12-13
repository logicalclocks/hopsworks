package io.hops.hopsworks.api.experiments.dto.results;

import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;
import java.util.HashMap;

@XmlRootElement
public class ExperimentResult {

  @XmlAnyAttribute
  private HashMap<QName, String> attributes;

  public ExperimentResult() {
    //Needed for JAXB
  }

  public HashMap<QName, String> getAttributes() {
    return this.attributes;
  }
}
