package se.kth.bbc.security.ua;


public enum BBCGroup {

  AUDITOR(1004),
  HOPS_ADMIN(1005),
  HOPS_USER(1006)
  ;

  private final int value;

  private BBCGroup(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }
}
