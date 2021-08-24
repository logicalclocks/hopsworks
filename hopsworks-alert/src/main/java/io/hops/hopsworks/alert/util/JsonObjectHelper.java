/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.alert.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Set;

public class JsonObjectHelper {
  // taken from org.json to avoid dependency conflict
  public static boolean similar(JSONObject config1, JSONObject config2) {
    try {
      Set<String> set = config1.keySet();
      if (!set.equals(config2.keySet())) {
        return false;
      }
      Iterator<String> iterator = set.iterator();
      
      while (iterator.hasNext()) {
        String name = iterator.next();
        Object valueThis = config1.get(name);
        Object valueOther = config2.get(name);
        if (valueThis instanceof JSONObject) {
          if (!((JSONObject) valueThis).similar(valueOther)) {
            return false;
          }
        } else if (valueThis instanceof JSONArray) {
          if (!((JSONArray) valueThis).similar(valueOther)) {
            return false;
          }
        } else if (!valueThis.equals(valueOther)) {
          return false;
        }
      }
      return true;
    } catch (Throwable exception) {
      return false;
    }
  }
}
