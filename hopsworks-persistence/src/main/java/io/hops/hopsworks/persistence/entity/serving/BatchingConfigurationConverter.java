/*
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.persistence.entity.serving;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.logging.Logger;

@Converter
public class BatchingConfigurationConverter implements AttributeConverter<BatchingConfiguration, String> {
  private static final Logger LOGGER = Logger.getLogger(BatchingConfigurationConverter.class.getName());
  
  final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public String convertToDatabaseColumn(BatchingConfiguration batchingConfiguration) {
    String jsonConfig;
    try {
      jsonConfig = objectMapper.writeValueAsString(batchingConfiguration);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
    return jsonConfig;
  }

  @Override
  public BatchingConfiguration convertToEntityAttribute(String jsonConfig) {
    if (Strings.isNullOrEmpty(jsonConfig)) {
      return null;
    }
    try {
      return objectMapper.readValue(jsonConfig, BatchingConfiguration.class);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
