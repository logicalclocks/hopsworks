package se.kth.bbc.security.ua;


public enum BBCGroup {

  BBC_ADMIN(1001),
  BBC_RESEARCHER(1002),
  BBC_GUEST(1003),
  AUDITOR(1004),  
  SYS_ADMIN(1005),
  BBC_USER(1006);

  private final int value;

  private BBCGroup(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }
}
