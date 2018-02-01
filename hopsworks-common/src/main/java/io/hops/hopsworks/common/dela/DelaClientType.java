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

package io.hops.hopsworks.common.dela;

/**
 * used in delaClientCtrl.js - make sure to sync
 */
public enum DelaClientType {

  BASE_CLIENT("BASE_CLIENT"),
  FULL_CLIENT("FULL_CLIENT");

  public final String type;
  
  DelaClientType(String type) {
    this.type = type;
  }
  
  public static DelaClientType from(String clientType) {
    switch (clientType) {
      case "BASE_CLIENT":
        return BASE_CLIENT;
      case "FULL_CLIENT":
        return FULL_CLIENT;
      default:
        return BASE_CLIENT;
    }
  }
}
