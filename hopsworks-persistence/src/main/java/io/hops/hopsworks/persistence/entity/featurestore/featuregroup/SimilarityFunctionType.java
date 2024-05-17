/*
 * This file is part of Hopsworks
 * Copyright (C) 2024, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.persistence.entity.featurestore.featuregroup;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum SimilarityFunctionType {

  @JsonProperty(value = "l2_norm")
  L2_NORM("l2"),
  @JsonProperty(value = "cosine")
  COSINE("cosinesimil"),
  @JsonProperty(value = "dot_product")
  DOT_PRODUCT("innerproduct");

  private final String opensearchFunction;

  SimilarityFunctionType(String opensearchFunction) {
    this.opensearchFunction = opensearchFunction;
  }

  public String getOpensearchFunction() {
    return opensearchFunction;
  }
}
