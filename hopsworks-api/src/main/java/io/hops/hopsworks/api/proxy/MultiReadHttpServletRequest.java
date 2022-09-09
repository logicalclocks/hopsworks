/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
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

import org.apache.commons.io.IOUtils;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class MultiReadHttpServletRequest extends HttpServletRequestWrapper {
  private byte[] cachedBody;
  
  public MultiReadHttpServletRequest(HttpServletRequest request) {
    super(request);
    try {
      this.cachedBody = IOUtils.toByteArray(request.getInputStream());
    } catch (IOException e) {
      this.cachedBody = new byte[0];
    }
  }
  
  @Override
  public ServletInputStream getInputStream() throws IOException {
    return new CachedBodyServletInputStream(this.cachedBody);
  }
  
  @Override
  public BufferedReader getReader() throws IOException {
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.cachedBody);
    return new BufferedReader(new InputStreamReader(byteArrayInputStream));
  }
}
