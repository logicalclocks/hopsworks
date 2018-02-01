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

/**
 * An optional argument to an ADAM command. I.e. an argument that is preceded by
 * a dash ('-') in the command.
 */
public final class AdamOption {

  private final String name;
  private final String description;
  private final boolean valueIsPath;
  private final boolean isFlag; //If is flag: no value required. If it is no flag, a value should be provided
  private final boolean isOutputPath;

  public AdamOption(String name, String description, boolean valueIsPath,
          boolean isFlag) {
    this(name, description, valueIsPath, isFlag, false);
  }

  public AdamOption(String name, String description, boolean valueIsPath,
          boolean isFlag, boolean isOutputPath) {
    if (isFlag && (valueIsPath || isOutputPath)) {
      throw new IllegalArgumentException(
              "An option cannot be both a path and a flag.");
    } else if (!valueIsPath && isOutputPath) {
      throw new IllegalArgumentException(
              "An option cannot be an output path but not a path.");
    }
    this.name = name;
    this.valueIsPath = valueIsPath;
    this.description = description;
    this.isFlag = isFlag;
    this.isOutputPath = isOutputPath;
  }

  public String getName() {
    return name;
  }

  public boolean isValuePath() {
    return valueIsPath;
  }

  public String getDescription() {
    return description;
  }

  public boolean isFlag() {
    return isFlag;
  }

  public boolean isOutputPath() {
    return isOutputPath;
  }

  public String getCliVal() {
    return "-" + name;
  }

  @Override
  public String toString() {
    StringBuilder ret = new StringBuilder();
    ret.append(name).append("\t:\t");
    ret.append(description);
    if (valueIsPath) {
      ret.append("[PATH]");
    }
    if (isFlag) {
      ret.append("[FLAG]");
    }
    return ret.toString();
  }

}
