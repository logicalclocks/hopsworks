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
package io.hops.hopsworks.api.proxy;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModifyHeaderHttpServletRequest extends HttpServletRequestWrapper {
  
  private final Map<String, String> headerMap = new HashMap<>();
  
  public ModifyHeaderHttpServletRequest(HttpServletRequest request) {
    super(request);
  }

  public void addHeader(String name, String value) {
    // need to be case-insensitive to prevent malicious users adding auth headers to request with different case
    headerMap.put(name.toLowerCase(), value);
  }
  
  @Override
  public String getHeader(String name) {
    String headerValue;
    if (headerMap.containsKey(name.toLowerCase())) {
      headerValue = headerMap.get(name.toLowerCase());
    } else {
      headerValue = super.getHeader(name);
    }
    return headerValue;
  }
  
  @Override
  public Enumeration<String> getHeaderNames() {
    List<String> names = Collections.list(super.getHeaderNames());
    names.addAll(headerMap.keySet());
    return Collections.enumeration(names);
  }
  
  @Override
  public Enumeration<String> getHeaders(String name) {
    List<String> values = new ArrayList<>();
    // important to check the map first so malicious users can't send auth headers added by the proxy
    if (headerMap.containsKey(name.toLowerCase())) {
      values.add(headerMap.get(name.toLowerCase()));
    } else {
      values.addAll(Collections.list(super.getHeaders(name)));
    }
    return Collections.enumeration(values);
  }
}
