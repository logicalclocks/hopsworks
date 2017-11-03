package io.hops.hopsworks.common.dao.user.cluster;

import javax.xml.bind.annotation.XmlEnumValue;

public enum RegistrationStatusEnum {
  @XmlEnumValue("Registered")
  REGISTERED("Registered"),
  @XmlEnumValue("Registration pending")
  REGISTRATION_PENDING("Registration pending"),
  @XmlEnumValue("Unregistration pending")
  UNREGISTRATION_PENDING("Unregistration pending");

  private final String readable;

  private RegistrationStatusEnum(String readable) {
    this.readable = readable;
  }

  public static RegistrationStatusEnum fromString(String shortName) {
    switch (shortName) {
      case "Registered":
        return RegistrationStatusEnum.REGISTERED;
      case "Registration pending":
        return RegistrationStatusEnum.REGISTRATION_PENDING;
      case "Unregistration pending":
        return RegistrationStatusEnum.UNREGISTRATION_PENDING;
      default:
        throw new IllegalArgumentException("ShortName [" + shortName + "] not supported.");
    }
  }

  @Override
  public String toString() {
    return this.readable;
  }

}
