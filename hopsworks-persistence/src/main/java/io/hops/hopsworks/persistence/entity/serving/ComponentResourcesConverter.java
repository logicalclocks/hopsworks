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
public class ComponentResourcesConverter implements AttributeConverter<DeployableComponentResources, String> {
  
  private static final Logger LOGGER = Logger.getLogger(DeployableComponentResources.class.getName());
  final ObjectMapper objectMapper = new ObjectMapper();
  
  @Override
  public String convertToDatabaseColumn(DeployableComponentResources config) {
    String jsonConfig;
    try {
      jsonConfig = objectMapper.writeValueAsString(config);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
    return jsonConfig;
  }
  
  @Override
  public DeployableComponentResources convertToEntityAttribute(String jsonConfig) {
    if (Strings.isNullOrEmpty(jsonConfig)) {
      return null;
    }
    try {
      return objectMapper.readValue(jsonConfig, DeployableComponentResources.class);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
