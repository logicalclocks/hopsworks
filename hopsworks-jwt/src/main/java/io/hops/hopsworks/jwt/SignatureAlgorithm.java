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
package io.hops.hopsworks.jwt;

public enum SignatureAlgorithm {
  HS256("HS256", "HmacSHA256"),
  HS384("HS384", "HmacSHA384"),
  HS512("HS512", "HmacSHA512"),
  RS256("RS256", "SHA256withRSA"),
  RS384("RS384", "SHA384withRSA"),
  RS512("RS512", "SHA512withRSA"),
  ES256("ES256", "SHA256withECDSA"),
  ES384("ES384", "SHA384withECDSA"),
  ES512("ES512", "SHA512withECDSA");

  private final String value;
  private final String jcaName;

  private SignatureAlgorithm(String value, String jcaName) {
    this.value = value;
    this.jcaName = jcaName;
  }

  public String getValue() {
    return value;
  }

  public String getJcaName() {
    return jcaName;
  }

  @Override
  public String toString() {
    return value;
  }

}
