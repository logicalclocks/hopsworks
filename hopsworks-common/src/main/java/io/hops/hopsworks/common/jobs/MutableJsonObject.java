/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.common.jobs;

import io.hops.hopsworks.common.dao.jobs.JsonReduceable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;

/**
 * Represents a mutable JSON object with only String values.
 */
public class MutableJsonObject {

  private final Map<String, String> internalStrings;
  private final Map<String, MutableJsonObject> internalJsons;

  /**
   * Create a new mutable JSON object.
   */
  public MutableJsonObject() {
    internalStrings = new HashMap<>();
    internalJsons = new HashMap<>();
  }

  /**
   * Convert a JsonObject into a DatabaseJsonObject. Note that a
   * DatabaseJsonObject cannot contain a Json array or Null. Other types are
   * converted.
   * <p/>
   * @param object
   * @throws IllegalArgumentException
   */
  public MutableJsonObject(JsonObject object) throws IllegalArgumentException {
    this();
    for (Entry<String, JsonValue> k : object.entrySet()) {
      String key = k.getKey();
      JsonValue val = k.getValue();
      switch (val.getValueType()) {
        case FALSE:
        case NUMBER:
        case TRUE:
          internalStrings.put(key, val.toString());
          break;
        case STRING:
          internalStrings.put(key, ((JsonString) val).getString());
          break;
        case OBJECT:
          internalJsons.put(key, new MutableJsonObject((JsonObject) val));
          break;
        default:
          throw new IllegalArgumentException(
                  "DatabaseJsonObject can only convert JsonObject, boolean, string and number.");
      }
    }
  }

  /**
   * Add a String key-value pair. If the key already exists, the value is
   * overwritten.
   * <p/>
   * @param key
   * @param value
   * @throws NullPointerException If the value is null.
   */
  public void set(String key, String value) {
    if (value == null) {
      throw new NullPointerException("Cannot store null value.");
    }
    internalStrings.put(key, value);
  }

  /**
   * Add a String-JSON object pair. If the key already exists, the value is
   * overwritten. Changes in the original value are also reflected in this
   * object.
   * <p/>
   * @param key
   * @param value
   */
  public void set(String key, MutableJsonObject value) {
    internalJsons.put(key, value);
  }

  /**
   * Add a String-JSON object pair. If the key already exists, the value is
   * overwritten.
   * <p/>
   * @param key
   * @param value
   */
  public void set(String key, JsonReduceable value) {
    internalJsons.put(key, value.getReducedJsonObject());
  }

  /**
   * Get the string value for this key.
   * <p/>
   * @param key
   * @return
   * @throws IllegalArgumentException If the object does not contain a string
   * value for this key.
   */
  public String getString(String key) throws IllegalArgumentException {
    try {
      String ret = internalStrings.get(key);
      if (ret == null) {
        throw new IllegalArgumentException("No such key.");
      }
      return ret;
    } catch (Exception e) {
      throw new IllegalArgumentException("Cannot find String for key " + key
              + ".", e);
    }
  }

  /**
   * Get the string for the given key, or the given default if this key is not
   * present.
   * <p/>
   * @param key
   * @param defaultvalue
   * @return
   * @throws IllegalArgumentException If the given key does not map to a String.
   */
  public String getString(String key, String defaultvalue) throws
          IllegalArgumentException {
    try {
      String ret = internalStrings.get(key);
      if (ret == null) {
        return defaultvalue;
      }
      return ret;
    } catch (Exception e) {
      throw new IllegalArgumentException("Cannot find String for key " + key
              + ".", e);
    }
  }

  /**
   * Get the JSON object value for this key.
   * <p/>
   * @param key
   * @return
   * @throws IllegalArgumentException If the object does not contain an object
   * value for this key.
   */
  public MutableJsonObject getJsonObject(String key) throws
          IllegalArgumentException {
    try {
      return internalJsons.get(key);
    } catch (Exception e) {
      throw new IllegalArgumentException("Cannot find JSON object for key "
              + key
              + ".", e);
    }
  }

  /**
   * Check if this JSON object contains the given key on the highest level. This
   * means that no recursive checking is performed.
   * <p/>
   * @param key
   * @return
   */
  public boolean containsKey(String key) {
    return internalStrings.containsKey(key) || internalJsons.containsKey(key);
  }

  /**
   * Return the number of top-level elements in this object.
   * <p/>
   * @return
   */
  public int size() {
    return internalStrings.size() + internalJsons.size();
  }

  /**
   * Get a view on the keys in this JSON object. The keyset is not backed by the
   * map, nor vice versa.
   * <p/>
   * @return
   */
  public Set<String> keySet() {
    Set<String> keys = new HashSet<>(internalStrings.size() + internalJsons.
            size());
    keys.addAll(internalStrings.keySet());
    keys.addAll(internalJsons.keySet());
    return keys;
  }

  /**
   * Get a String representation in JSON format of this object.
   * <p/>
   * @return
   */
  public String toJson() {
    return toJsonObject().toString();
  }

  private JsonObject toJsonObject() {
    JsonObjectBuilder builder = Json.createObjectBuilder();
    for (String s : internalStrings.keySet()) {
      builder.add(s, internalStrings.get(s));
    }
    for (String s : internalJsons.keySet()) {
      builder.add(s, internalJsons.get(s).toJsonObject());
    }
    return builder.build();
  }

  @Override
  public int hashCode() {
    int hash = 3;
    hash = 89 * hash + this.internalStrings.hashCode();
    hash = 89 * hash + this.internalJsons.hashCode();
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final MutableJsonObject other = (MutableJsonObject) obj;
    if (this.internalStrings.equals(other.internalStrings)) {
      return false;
    }
    if (!this.internalJsons.equals(other.internalJsons)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return this.toJson();
  }

}
