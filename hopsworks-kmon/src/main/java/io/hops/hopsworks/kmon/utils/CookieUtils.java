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

package io.hops.hopsworks.kmon.utils;

import java.util.Map;
import javax.faces.context.FacesContext;
import javax.servlet.http.Cookie;

public class CookieUtils {

  public static String read(String name) {
    Map<String, Object> requestCookieMap = FacesContext.
            getCurrentInstance().getExternalContext().getRequestCookieMap();

    Cookie c = (Cookie) requestCookieMap.get(name);

    if (c == null) {
      return "";
    }
    return c.getValue();
  }

  public static void write(String name, String value) {
    FacesContext.getCurrentInstance().getExternalContext()
            .addResponseCookie(name, value, null);
  }
}
