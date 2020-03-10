/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */
package io.hops.hopsworks.persistence.entity.user.activity;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "ActivityFlag")
@XmlEnum
public enum ActivityFlag {
  @XmlEnumValue("MEMBER")
  MEMBER("MEMBER"),
  @XmlEnumValue("PROJECT")
  PROJECT("PROJECT"),
  @XmlEnumValue("SERVICE")
  SERVICE("SERVICE"),
  @XmlEnumValue("DATASET")
  DATASET("DATASET"),
  @XmlEnumValue("JOB")
  JOB("JOB");
  
  private final String value;
  
  private ActivityFlag(String value) {
    this.value = value;
  }
  
  public String getValue() {
    return value;
  }
  
  @Override
  public String toString() {
    return value;
  }
  
}
