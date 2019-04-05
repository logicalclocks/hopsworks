package io.hops.hopsworks.common.dao.user.activity;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "ActivityFlag")
@XmlEnum
public enum ActivityFlag {
  @XmlEnumValue("MEMBER")
  MEMBER("MEMBER"),
  @XmlEnumValue("PROJECT")
  PROJECT("PROJECT"),
  @XmlEnumValue("SERVICE")
  SERVICE("SERVICE"),
  @XmlEnumValue("DATASET")
  DATASET("DATASET"),
  @XmlEnumValue("JOB")
  JOB("JOB");
  
  private final String value;
  
  private ActivityFlag(String value) {
    this.value = value;
  }
  
  public String getValue() {
    return value;
  }
  
  @Override
  public String toString() {
    return value;
  }
  
}
