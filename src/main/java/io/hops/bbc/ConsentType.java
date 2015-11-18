package io.hops.bbc;

public enum ConsentType {

  ETHICAL_APPROVAL("Ethical Approval"),
  CONSENT_INFO("Consent Info"),
  NON_CONSENT("Non Consent Info"),
  UNDEFINED("Undefined");

  private final String readable;

  private ConsentType(String readable) {
    this.readable = readable;
  }

  public static ConsentType create(String str) {
    if (str.compareTo(ETHICAL_APPROVAL.toString()) == 0) {
      return ETHICAL_APPROVAL;
    }
    if (str.compareTo(CONSENT_INFO.toString()) == 0) {
      return CONSENT_INFO;
    }
    if (str.compareTo(NON_CONSENT.toString()) == 0) {
      return NON_CONSENT;
    }
    return UNDEFINED;
  }

  @Override
  public String toString() {
    return readable;
  }

}
