package io.hops.hopsworks.kmon.struct;

public class DatePeriod {

  private String label;
  private String value;

  public DatePeriod(String label, String value) {
    this.label = label;
    this.value = value;
  }

  public String getLabel() {
    return label;
  }

  public String getValue() {
    return value;
  }
}
