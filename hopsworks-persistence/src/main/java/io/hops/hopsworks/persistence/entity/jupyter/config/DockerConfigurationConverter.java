/*
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.persistence.entity.jupyter.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import io.hops.hopsworks.persistence.entity.jobs.configuration.DockerJobConfiguration;
import io.hops.hopsworks.persistence.entity.jobs.configuration.JobConfiguration;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.logging.Logger;

@Converter
public class DockerConfigurationConverter implements AttributeConverter<JobConfiguration, String> {

  private static final Logger LOGGER = Logger.getLogger(DockerConfigurationConverter.class.getName());
  final ObjectMapper objectMapper = new ObjectMapper();
  
  @Override
  public String convertToDatabaseColumn(JobConfiguration config) {
    String jsonConfig;
    try {
      jsonConfig = objectMapper.writeValueAsString(config);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
    return jsonConfig;
  }
  
  @Override
  public JobConfiguration convertToEntityAttribute(String jsonConfig) {
    if (Strings.isNullOrEmpty(jsonConfig)) {
      return null;
    }
    try {
      return objectMapper.readValue(jsonConfig, DockerJobConfiguration.class);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
