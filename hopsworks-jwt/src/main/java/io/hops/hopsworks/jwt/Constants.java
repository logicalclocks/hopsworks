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

public class Constants {

  public static final String BEARER = "Bearer "; // The whitespace is required. 
  public static final String RENEWABLE = "renewable";
  public static final String EXPIRY_LEEWAY = "expLeeway";
  public static final String ROLES = "roles";
  public static final String WWW_AUTHENTICATE_VALUE="Bearer realm=\"Cauth Realm\"";
  
  public static final int DEFAULT_EXPIRY_LEEWAY = 60; //60 secs for exp
  public static final boolean DEFAULT_RENEWABLE = false;
  
  public static final int ONE_TIME_JWT_SIGNING_KEY_ROTATION_DAYS = 1;
  public static final String ONE_TIME_JWT_SIGNATURE_ALGORITHM = "HS256";
  public static final String ONE_TIME_JWT_SIGNING_KEY_NAME = "oneTimeKey";
  public static final String OLD_ONE_TIME_JWT_SIGNING_KEY_NAME = ONE_TIME_JWT_SIGNING_KEY_NAME + "_old";
  public static final long ONE_TIME_JWT_LIFETIME_MS = 60000l;
}
