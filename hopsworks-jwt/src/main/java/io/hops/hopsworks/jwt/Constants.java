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
  
  public final static String ELK_SIGNING_KEY_NAME = "elk_jwt_signing_key";
  public final static String ELK_VALID_PROJECT_NAME = "pn";
  public final static String ELK_PROJECT_INODE_ID = "piid";
  public final static String ELK_PROJECT_ID = "projid";


  public final static String GIT_SIGNING_KEY_NAME = "git_jwt_signing_key";
  
  public final static String PROXY_SIGNING_KEY_NAME = "proxy_jwt_signing_key";
  public final static String PROXY_JWT_COOKIE_NAME = "proxy_session";
  public static final String PROXY_JWT_COOKIE_PATH = "/";
  public static final String PROXY_JWT_COOKIE_COMMENT = "";
  public static final boolean PROXY_JWT_COOKIE_SECURE = true;
  public static final boolean PROXY_JWT_COOKIE_HTTP_ONLY = true;
}
