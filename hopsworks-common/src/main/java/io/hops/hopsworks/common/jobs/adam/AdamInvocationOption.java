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

package io.hops.hopsworks.common.jobs.adam;

public class AdamInvocationOption {

  private final AdamOption opt;
  private String stringValue;
  private boolean booleanValue = false;
  private final boolean usesBoolean;

  public AdamInvocationOption(AdamOption opt) {
    this.opt = opt;
    usesBoolean = opt.isFlag();
  }

  public String getStringValue() {
    if (usesBoolean) {
      throw new IllegalStateException(
              "Cannot get the String value of a boolean option.");
    }
    return stringValue;
  }

  public void setStringValue(String stringValue) {
    if (usesBoolean) {
      throw new IllegalStateException(
              "Cannot set the String value of a boolean option.");
    }
    this.stringValue = stringValue;
  }

  public boolean getBooleanValue() {
    if (!usesBoolean) {
      throw new IllegalStateException(
              "Cannot get the boolean value of a String option.");
    }
    return booleanValue;
  }

  public void setBooleanValue(boolean booleanValue) {
    if (!usesBoolean) {
      throw new IllegalStateException(
              "Cannot set the boolean value of a String option.");
    }
    this.booleanValue = booleanValue;
  }

  public AdamOption getOpt() {
    return opt;
  }

  @Override
  public String toString() {
    if (usesBoolean) {
      return "<" + opt + ":" + booleanValue + ">";
    } else {
      return "<" + opt + ":" + stringValue + ">";
    }
  }

}
