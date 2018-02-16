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

package io.hops.hopsworks.common.dao.tfserving;


import javax.xml.bind.annotation.XmlEnumValue;

public enum TfServingStatusEnum {
  @XmlEnumValue("Created")
  CREATED("Created"),
  @XmlEnumValue("Running")
  RUNNING("Running"),
  @XmlEnumValue("Stopped")
  STOPPED("Stopped"),
  @XmlEnumValue("Starting")
  STARTING("Starting");

  private final String readable;

  private TfServingStatusEnum(String readable) {
    this.readable = readable;
  }

  public static TfServingStatusEnum fromString(String shortName) {
    switch (shortName) {
      case "Created":
        return TfServingStatusEnum.CREATED;
      case "Running":
        return TfServingStatusEnum.RUNNING;
      case "Stopped":
        return TfServingStatusEnum.STOPPED;
      case "Starting":
        return TfServingStatusEnum.STARTING;
      default:
        throw new IllegalArgumentException("ShortName [" + shortName + "] not supported.");
    }
  }

  @Override
  public String toString() {
    return this.readable;
  }

}
