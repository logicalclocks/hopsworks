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
package io.hops.hopsworks.persistence.entity.alertmanager;

import org.json.JSONObject;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.nio.charset.StandardCharsets;

@Converter
public class ConfigConverter implements AttributeConverter<JSONObject, byte[]> {
  @Override
  public byte[] convertToDatabaseColumn(JSONObject jsonObject) {
    return jsonObject.toString().getBytes(StandardCharsets.UTF_8);
  }
  
  @Override
  public JSONObject convertToEntityAttribute(byte[] bytes) {
    return new JSONObject(new String(bytes, StandardCharsets.UTF_8));
  }
}
