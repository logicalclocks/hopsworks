/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.importjob;

import com.google.gson.annotations.SerializedName;

import javax.xml.bind.annotation.XmlEnum;

@XmlEnum
public enum ImportType {
  @SerializedName("s3")
  S3("s3"),
  @SerializedName("redshift")
  REDSHIFT("redshift");

  private final String importType;

  ImportType(String importType) {
    this.importType = importType;
  }

  public String toString() {
    return importType;
  }
}
