/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.jwt.utils;

import javax.servlet.http.Cookie;
import javax.ws.rs.core.NewCookie;

import static io.hops.hopsworks.jwt.Constants.PROXY_JWT_COOKIE_COMMENT;
import static io.hops.hopsworks.jwt.Constants.PROXY_JWT_COOKIE_HTTP_ONLY;
import static io.hops.hopsworks.jwt.Constants.PROXY_JWT_COOKIE_NAME;
import static io.hops.hopsworks.jwt.Constants.PROXY_JWT_COOKIE_PATH;
import static io.hops.hopsworks.jwt.Constants.PROXY_JWT_COOKIE_SECURE;
import static java.lang.Math.abs;

public class ProxyAuthHelper {

  /**
   * javax.ws.rs.core.NewCookie
   *
   * @param token
   * @return
   */
  public static NewCookie getNewCookie(String token, long tokenLifeMs) {
    int maxAge = getMaxAgeFromTokenLifeMs(tokenLifeMs);
    // Setting domain adds . in front of the domain name, which will create problem when renewing the cookie
    return new NewCookie(PROXY_JWT_COOKIE_NAME, token, PROXY_JWT_COOKIE_PATH, null, PROXY_JWT_COOKIE_COMMENT, maxAge,
      PROXY_JWT_COOKIE_SECURE, PROXY_JWT_COOKIE_HTTP_ONLY);
  }

  public static NewCookie getNewCookieForLogout() {
    return new NewCookie(PROXY_JWT_COOKIE_NAME, null, PROXY_JWT_COOKIE_PATH, null, PROXY_JWT_COOKIE_COMMENT, 0,
      PROXY_JWT_COOKIE_SECURE, PROXY_JWT_COOKIE_HTTP_ONLY);
  }

  /**
   * javax.servlet.http.Cookie
   *
   * @param token
   * @return
   */
  public static Cookie getCookie(String token, long tokenLifeMs) {
    int maxAge = getMaxAgeFromTokenLifeMs(tokenLifeMs);
    Cookie newCookie = new Cookie(PROXY_JWT_COOKIE_NAME, token);
    // Setting domain adds . in front of the domain name, which will create problem when renewing the cookie
    // newCookie.setDomain(xxxxxx);
    newCookie.setHttpOnly(PROXY_JWT_COOKIE_HTTP_ONLY);
    newCookie.setSecure(PROXY_JWT_COOKIE_SECURE);
    newCookie.setMaxAge(maxAge);
    newCookie.setPath(PROXY_JWT_COOKIE_PATH);
    newCookie.setComment(PROXY_JWT_COOKIE_COMMENT);
    return newCookie;
  }

  private static int getMaxAgeFromTokenLifeMs(long tokenLifeMs) {
    long maxAge = abs(tokenLifeMs) / 1000;
    if (maxAge > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    }
    return (int) maxAge;
  }
}
