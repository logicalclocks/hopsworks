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
