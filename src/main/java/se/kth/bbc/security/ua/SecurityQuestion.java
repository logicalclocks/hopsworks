package se.kth.bbc.security.ua;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
public enum SecurityQuestion {

  MAIDEN_NAME("What is your mother's maiden name?"),
  PET("What is the name of your first pet?"),
  TEACHER("What is the name of your favorite teacher?"),
  FRIEND("What is the name of your favorite childhood friend?");

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
