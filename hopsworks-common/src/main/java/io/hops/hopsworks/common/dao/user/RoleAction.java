package io.hops.hopsworks.common.dao.user;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "bbc-group-action")
@XmlEnum
public enum RoleAction {
  @XmlEnumValue("ADD")
  ADD,

  @XmlEnumValue("REMOVE")
  REMOVE;
}
