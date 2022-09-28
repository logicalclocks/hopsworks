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
package io.hops.hopsworks.api.jwt;

import javax.xml.bind.annotation.adapters.XmlAdapter;

// until the request is fixed in yarn
public class ArrayAdapter extends XmlAdapter<String, String[]> {
  @Override
  public String[] unmarshal(String s) throws Exception {
    return s == null ? null : s.split(",");
  }
  
  @Override
  public String marshal(String[] strings) throws Exception {
    return strings == null ? null : String.join(",", strings);
  }
}
