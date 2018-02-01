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
