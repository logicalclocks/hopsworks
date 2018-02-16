/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

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
