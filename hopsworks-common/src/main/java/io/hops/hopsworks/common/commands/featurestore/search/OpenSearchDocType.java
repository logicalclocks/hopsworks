/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.common.commands.featurestore.search;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OpenSearchDocType {
  FEATURE_GROUP("featuregroup"),
  FEATURE_VIEW("featureview"),
  TRAINING_DATASET("trainingdataset");
  
  private String value;
  OpenSearchDocType(String value) {
    this.value = value;
  }
  
  @Override
  public String toString() {
    return value;
  }
  
  @JsonValue
  public String getValue() {
    return value;
  }
  
  @JsonCreator
  public static OpenSearchDocType getByValue(String value) {
    if(value != null) {
      for (OpenSearchDocType status : values()) {
        if (status.value.equals(value)) {
          return status;
        }
      }
      throw new IllegalArgumentException("Unknown search doc type '" + value + "'");
    }
    return null;
  }
}
