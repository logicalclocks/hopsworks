 
package se.kth.bbc.security.ua;

 
public enum SecurityQuestion {

  MAIDEN_NAME("Mother's maiden name?"),
  PET("Name of your first pet?"),
  LOVE("Name of your first love?");

  private final String value;

  private SecurityQuestion(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static SecurityQuestion getQuestion(String text) {
    if (text != null) {
      for (SecurityQuestion b : SecurityQuestion.values()) {
        if (text.equalsIgnoreCase(b.value)) {
          return b;
        }
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return value;
  }
}
