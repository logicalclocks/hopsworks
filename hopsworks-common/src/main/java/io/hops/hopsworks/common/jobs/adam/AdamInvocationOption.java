/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
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
