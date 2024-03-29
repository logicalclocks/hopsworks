/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.persistence.entity.serving;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model Framework
 */
public enum ModelFramework {
  //Note: since we map enum directly to the DB the order is important!
  @JsonProperty("TENSORFLOW")
  TENSORFLOW,
  @JsonProperty("PYTHON")
  PYTHON,
  @JsonProperty("SKLEARN")
  SKLEARN,
  @JsonProperty("TORCH")
  TORCH;
  
  @JsonCreator
  public static ModelFramework fromString(String modelFramework) {
    if (modelFramework != null) {
      switch (modelFramework) {
        case "TENSORFLOW":
          return TENSORFLOW;
        case "PYTHON":
          return PYTHON;
        case "SKLEARN":
          return SKLEARN;
        case "TORCH":
          return TORCH;
      }
    }
    return null;
  }
}
