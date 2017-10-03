package io.hops.hopsworks.api.cluster;

import io.hops.hopsworks.common.dao.role.Action;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class RolesActionDTO {

  @XmlElement(name = "action",
      required = true)
  private Action action;

  public Action getAction() {
    return action;
  }

  public void setAction(Action action) {
    this.action = action;
  }

}
