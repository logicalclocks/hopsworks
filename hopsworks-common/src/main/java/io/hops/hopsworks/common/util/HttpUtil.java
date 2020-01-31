/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.util;

import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;

public class HttpUtil {
  
  
  public static String extractUserAgent(HttpServletRequest httpServletRequest) {
    String userAgent = httpServletRequest.getHeader("User-Agent");
    if (userAgent == null || userAgent.isEmpty()) {
      return "Unknown User-Agent";
    }
    return StringUtils.left(userAgent, 255);
  }
  
  public static String extractRemoteHostIp(HttpServletRequest httpServletRequest) {
    String ipAddress = httpServletRequest.getHeader("X-Forwarded-For");
    if (ipAddress == null) {
      ipAddress = httpServletRequest.getRemoteAddr();
    }
    // if reverse proxy is used for request, X-FORWARD-FOR header value will contain
    // (client ip, load balancer server, reverse proxy server)
    return ipAddress.contains(",") ? ipAddress.split(",")[0] : ipAddress;
  }
}
