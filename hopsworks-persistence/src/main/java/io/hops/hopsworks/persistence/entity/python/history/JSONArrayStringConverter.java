/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.persistence.entity.python.history;

import com.google.common.base.Strings;
import org.json.JSONArray;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class JSONArrayStringConverter implements AttributeConverter<JSONArray, String> {
  @Override
  public String convertToDatabaseColumn(JSONArray jsonArray) {
    if (jsonArray == null) {
      return null;
    }
    return jsonArray.toString();
  }

  @Override
  public JSONArray convertToEntityAttribute(String value) {
    if (Strings.isNullOrEmpty(value)) {
      return new JSONArray();
    }
    return new JSONArray(value);
  }
}

