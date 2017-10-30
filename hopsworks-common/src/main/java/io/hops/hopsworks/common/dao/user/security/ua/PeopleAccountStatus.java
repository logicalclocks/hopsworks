package io.hops.hopsworks.common.dao.user.security.ua;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "peopleAccountStatus")
@XmlEnum
public enum PeopleAccountStatus {

  /*
   * 
   * WARNING: The ordinal of the enum is stored in the DB
   * Do not change the order of the enum, this would break older installations
   * 
   * zero was added to have the ordinal mathc the value while not breaking existing installation.
   * 
   */
  ZERO(0),
  
  // Status of new Mobile users requests
  @XmlEnumValue("NEW_MOBILE_ACCOUNT")
  NEW_MOBILE_ACCOUNT(1),

  // Status of new Yubikey users requests
  @XmlEnumValue("NEW_YUBIKEY_ACCOUNT")
  NEW_YUBIKEY_ACCOUNT(2),

  // For new account requests where users should validate their account request
  @XmlEnumValue("VERIFIED_ACCOUNT")
  VERIFIED_ACCOUNT(3),

  // When user is approved to use the platform
  @XmlEnumValue("ACTIVATED_ACCOUNT")
  ACTIVATED_ACCOUNT(4),

  // Users that are no longer granted to access the platform.
  // Users with this state, can not login, change password even as guest users
  @XmlEnumValue("DEACTIVATED_ACCOUNT")
  DEACTIVATED_ACCOUNT(5),

  // Blocked users can not user the resources. This can be due to suspicious behaviour of users. 
  // For example. multiple false logings
  @XmlEnumValue("BLOCKED_ACCOUNT")
  BLOCKED_ACCOUNT(6),

  // For scenrios where mobile device is compromised/lost
  @XmlEnumValue("LOST_MOBILE")
  LOST_MOBILE(7),

  // For scenarios where Yubikey device is compromised/lost
  @XmlEnumValue("LOST_YUBIKEY")
  LOST_YUBIKEY(8),

  // Mark account as SPAM
  @XmlEnumValue("SPAM_ACCOUNT")
  SPAM_ACCOUNT(9);



  private final int value;

  private PeopleAccountStatus(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }
  
  public static PeopleAccountStatus fromValue(int v) {
    for (PeopleAccountStatus c : PeopleAccountStatus.values()) {
      if (c.value==v) {
        return c;
      }
    }
    throw new IllegalArgumentException("" + v);
  }
}
