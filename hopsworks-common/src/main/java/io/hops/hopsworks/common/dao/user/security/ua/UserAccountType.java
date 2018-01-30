package io.hops.hopsworks.common.dao.user.security.ua;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "userAccountType")
@XmlEnum
public enum UserAccountType {

  @XmlEnumValue("M_ACCOUNT_TYPE")
  M_ACCOUNT_TYPE(0),
  @XmlEnumValue("LDAP_ACCOUNT")
  LDAP_ACCOUNT_TYPE(1);

  private final int value;

  private UserAccountType(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }

  public static UserAccountType fromValue(int v) {
    for (UserAccountType c : UserAccountType.values()) {
      if (c.value == v) {
        return c;
      }
    }
    throw new IllegalArgumentException("" + v);
  }
}
