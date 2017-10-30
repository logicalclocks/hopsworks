package io.hops.hopsworks.common.dao.user.security.ua;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "SecurityQuestion")
@XmlEnum
public enum SecurityQuestion {

  @XmlEnumValue("Mother's maiden name?")
  MAIDEN_NAME("Mother's maiden name?"),
  @XmlEnumValue("Name of your first pet?")
  PET("Name of your first pet?"),
  @XmlEnumValue("Name of your first love?")
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

  private static final List<SecurityQuestion> VALUES = Collections.unmodifiableList(Arrays.asList(values()));
  private static final int SIZE = VALUES.size();
  private static final Random RANDOM = new Random();

  public static SecurityQuestion randomQuestion() {
    return VALUES.get(RANDOM.nextInt(SIZE));
  }
}
