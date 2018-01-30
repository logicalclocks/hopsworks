package io.hops.hopsworks.common.dao.kagent;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "cluster-action")
@XmlEnum
public enum Action {
  @XmlEnumValue("START")
  START("startRole"),

  @XmlEnumValue("RESTART")
  RESTART("restartRole"),

  @XmlEnumValue("STOP")
  STOP("stopRole");

  private final String value;

  Action(String v) {
    value = v;
  }

  public String value() {
    return value;
  }

  public static Action fromValue(String v) {
    for (Action c : Action.values()) {
      if (c.value.equals(v)) {
        return c;
      }
    }
    throw new IllegalArgumentException(v);
  }
}
