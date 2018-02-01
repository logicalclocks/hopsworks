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
 * A non-optional argument to an ADAM command. I.e. an input parameter that is
 * not preceded by a '-x' in the command.
 */
public final class AdamArgument {

  private final String name;
  private final String description;
  private final boolean isPath;
  private final boolean required;
  private final boolean isOutputPath;

  public AdamArgument(String name, String description, boolean isPath) {
    this(name, description, isPath, false);
  }

  public AdamArgument(String name, String description, boolean isPath,
          boolean isOutputPath) {
    this(name, description, isPath, isOutputPath, true);
  }

  public AdamArgument(String name, String description, boolean isPath,
          boolean isOutputPath, boolean required) {
    if (isOutputPath && !isPath) {
      throw new IllegalArgumentException(
              "Argument cannot be an output path if it is not a path.");
    }
    this.name = name;
    this.description = description;
    this.isPath = isPath;
    this.required = required;
    this.isOutputPath = isOutputPath;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public boolean isPath() {
    return isPath;
  }

  public boolean isRequired() {
    return required;
  }

  public boolean isOutputPath() {
    return isOutputPath;
  }

  @Override
  public String toString() {
    StringBuilder ret = new StringBuilder();
    ret.append(name).append("\t:\t");
    ret.append(description);
    if (isPath) {
      ret.append("[PATH]");
    }
    if (required) {
      ret.append("[REQUIRED]");
    }
    return ret.toString();
  }

}
