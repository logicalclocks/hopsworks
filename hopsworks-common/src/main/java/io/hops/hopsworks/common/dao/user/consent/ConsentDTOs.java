package io.hops.hopsworks.common.dao.user.consent;

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
