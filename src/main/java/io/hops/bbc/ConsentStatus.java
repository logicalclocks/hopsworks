package io.hops.bbc;

public enum ConsentStatus {

  APPROVED("Approved"),
  PENDING("Pending"),
  REJECTED("Rejected"),
  UNDEFINED("Undefined")
  ;

  private final String readable;

  private ConsentStatus(String readable) {
    this.readable = readable;
  }
  public static ConsentStatus create(String str) {
    if (str.compareTo(APPROVED.toString()) == 0) {
      return APPROVED;
    }
    if (str.compareTo(PENDING.toString()) == 0) {
      return PENDING;
    }
    if (str.compareTo(REJECTED.toString()) == 0) {
      return REJECTED;
    }
    return UNDEFINED;
  }
  @Override
  public String toString() {
    return readable;
  }

}
