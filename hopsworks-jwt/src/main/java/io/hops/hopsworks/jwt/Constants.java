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

  public static final String BEARER = "Bearer ";
  public static final String RENEWABLE = "renewable";
  public static final String EXPIRY_LEEWAY = "expLeeway";
  public static final String ROLES = "roles";
  public static final String WWW_AUTHENTICATE_VALUE="Bearer realm=\"Cauth Realm\"";
  
  public static final int DEFAULT_EXPIRY_LEEWAY = 5;
  public static final boolean DEFAULT_RENEWABLE = true;
}
