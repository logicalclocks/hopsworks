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
package io.hops.hopsworks.api.filter;


public class Audience {
  public static final String API = "api"; // Used by UI. Can access any rest endpoint
  public static final String JOB ="job"; // Used by internal services to start or stop jobs
  public static final String DATASET ="dataset";
  public static final String SERVING = "serving";
  public static final String EMAIL ="email";
  public static final String SERVICES ="services";
  public static final String GIT = "git";
  public static final String PROXY = "proxy";
}
