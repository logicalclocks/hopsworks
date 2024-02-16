/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.persistence.entity.featurestore.storageconnector.kafka;

import java.util.Arrays;
import java.util.Optional;

public enum SSLEndpointIdentificationAlgorithm {
  HTTPS("HTTPS"),
  EMPTY("");
  
  private String value;
  
  SSLEndpointIdentificationAlgorithm(String value) {
    this.value = value;
  }
  
  public static SSLEndpointIdentificationAlgorithm fromString(String value) {
    Optional<SSLEndpointIdentificationAlgorithm> algorithm = Arrays.stream(SSLEndpointIdentificationAlgorithm.values())
      .filter(a -> a.getValue().equals(value.toUpperCase())).findFirst();
  
    if (algorithm.isPresent()) {
      return algorithm.get();
    } else {
      throw new IllegalArgumentException("Invalid ssl endpoint identification algorithm provided");
    }
  }
  
  public String getValue() {
    return value;
  }
  
  // Overriding toString() method to return "" instead of "EMPTY"
  @Override
  public String toString(){
    return this.value;
  }
}
