/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.persistence.entity.git;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import io.hops.hopsworks.persistence.entity.git.config.GitCommandConfiguration;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.logging.Logger;

@Converter
public class GitCommandConfigurationConverter implements AttributeConverter<GitCommandConfiguration, String> {
  private static final Logger LOGGER = Logger.getLogger(GitCommandConfiguration.class.getName());
  final ObjectMapper objectMapper = new ObjectMapper();
  
  @Override
  public String convertToDatabaseColumn(GitCommandConfiguration commandConfiguration) {
    String jsonConfig;
    try {
      jsonConfig = objectMapper.writeValueAsString(commandConfiguration);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
    return jsonConfig;
  }
  
  @Override
  public GitCommandConfiguration convertToEntityAttribute(String jsonConfig) {
    if (Strings.isNullOrEmpty(jsonConfig)) {
      return null;
    }
    try {
      return objectMapper.readValue(jsonConfig, GitCommandConfiguration.class);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
