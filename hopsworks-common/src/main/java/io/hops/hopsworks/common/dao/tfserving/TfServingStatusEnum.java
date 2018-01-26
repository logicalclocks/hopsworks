package io.hops.hopsworks.common.dao.tfserving;


import javax.xml.bind.annotation.XmlEnumValue;

public enum TfServingStatusEnum {
  @XmlEnumValue("Created")
  CREATED("Created"),
  @XmlEnumValue("Running")
  RUNNING("Running"),
  @XmlEnumValue("Stopped")
  STOPPED("Stopped"),
  @XmlEnumValue("Starting")
  STARTING("Starting");

  private final String readable;

  private TfServingStatusEnum(String readable) {
    this.readable = readable;
  }

  public static TfServingStatusEnum fromString(String shortName) {
    switch (shortName) {
      case "Created":
        return TfServingStatusEnum.CREATED;
      case "Running":
        return TfServingStatusEnum.RUNNING;
      case "Stopped":
        return TfServingStatusEnum.STOPPED;
      case "Starting":
        return TfServingStatusEnum.STARTING;
      default:
        throw new IllegalArgumentException("ShortName [" + shortName + "] not supported.");
    }
  }

  @Override
  public String toString() {
    return this.readable;
  }

}
