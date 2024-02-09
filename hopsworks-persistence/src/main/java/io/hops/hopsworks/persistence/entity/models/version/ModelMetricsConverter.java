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

package io.hops.hopsworks.persistence.entity.models.version;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.json.JSONObject;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.logging.Logger;

@Converter
public class ModelMetricsConverter implements AttributeConverter<Metrics, String> {

  public ModelMetricsConverter() {
    objectMapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
    objectMapper.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, false);
  }

  private final ObjectMapper objectMapper = new ObjectMapper();

  public <T> T readValue(String jsonConfig, Class<T> resultClass) throws JsonProcessingException {
    if(jsonConfig == null) {
      jsonConfig = new JSONObject().toString();
    }
    return objectMapper.readValue(jsonConfig, resultClass);
  }

  public String writeValue(Object value) throws JsonProcessingException {
    return objectMapper.writeValueAsString(value);
  }

  private static final Logger LOGGER = Logger.getLogger(ModelMetricsConverter.class.getName());

  @Override
  public String convertToDatabaseColumn(Metrics metrics) {
    String jsonConfig;
    try {
      jsonConfig = writeValue(metrics);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to marshal value:" + metrics, e);
    }
    return jsonConfig;
  }

  @Override
  public Metrics convertToEntityAttribute(String jsonConfig) {
    try {
      return readValue(jsonConfig, Metrics.class);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to unmarshal value:" + jsonConfig, e);
    }
  }
}
