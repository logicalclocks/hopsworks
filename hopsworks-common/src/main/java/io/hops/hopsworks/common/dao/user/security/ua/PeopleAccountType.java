package io.hops.hopsworks.common.dao.user.security.ua;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "peopleAccountType")
@XmlEnum
public enum PeopleAccountType {
  
  /*
   * 
   * WARNING: The ordinal of the enum is stored in the DB
   * Do not change the order of the enum, this would break older installations
   * 
   * zero to nine, come from the fact that this was refactored out of another enum and that 
   * we have to keep the ordinals the same in order to not break existing installations.
   * Feel free to replace them by useful values.
   * 
   */
  ZERO(0), ONE(1),TWO(2),THREE(3),FOUR(4),FIVE(5),SIX(6),SEVEN(7),HEIGHT(8),  

  // Mark account as LDAP
  @XmlEnumValue("LDAP_ACCOUNT")
  LDAP_ACCOUNT_TYPE(9),
  
  // For mobile account types classification
  @XmlEnumValue("M_ACCOUNT_TYPE")
  M_ACCOUNT_TYPE(10),  //kept number 10 and 11 to keep retrocompatibility

  // For Yubikey account types classification
  @XmlEnumValue("Y_ACCOUNT_TYPE")
  Y_ACCOUNT_TYPE(11); //kept number 10 and 11 to keep retrocompatibility

  private final int value;

  private PeopleAccountType(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }

  public static PeopleAccountType fromValue(int v) {
    for (PeopleAccountType c : PeopleAccountType.values()) {
      if (c.value == v) {
        return c;
      }
    }
    throw new IllegalArgumentException("" + v);
  }
}
