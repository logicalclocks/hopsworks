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

package io.hops.hopsworks.alerting.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.google.common.base.Strings;
import io.hops.hopsworks.alerting.api.AlertManagerClient;
import io.hops.hopsworks.alerting.config.dto.AlertManagerConfig;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigFileNotFoundException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigReadException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigUpdateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerResponseException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerServerException;

import java.io.File;
import java.io.IOException;

public class AlertManagerConfigController {
  private static final String CONFIG_FILE_PATH = "/srv/hops/alertmanager/alertmanager/alertmanager.yml";
  
  private final File configFile;
  
  private AlertManagerConfigController(File configFile) {
    this.configFile = configFile;
  }
  
  /**
   * Read Alertmanager config from configFile
   * @return
   */
  public AlertManagerConfig read() throws AlertManagerConfigReadException {
    ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    try {
      return objectMapper.readValue(configFile, AlertManagerConfig.class);
    } catch (IOException e) {
      throw new AlertManagerConfigReadException("Failed to read configuration file. Error " + e.getMessage());
    }
  }
  
  /**
   * Writes alertManagerConfig to configFile in YAML format.
   * Do not use if you are not sure the yaml is well-formed.
   * User writeAndReload instead that will roll back if loading the yaml fails
   * @param alertManagerConfig
   * @throws IOException
   */
  public void write(AlertManagerConfig alertManagerConfig) throws AlertManagerConfigUpdateException {
    YAMLFactory yamlFactory = new YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
    ObjectMapper objectMapper = new ObjectMapper(yamlFactory);
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.setPropertyNamingStrategy(new PropertyNamingStrategies.SnakeCaseStrategy());
    try {
      objectMapper.writeValue(configFile, alertManagerConfig);
    } catch (IOException e) {
      throw new AlertManagerConfigUpdateException("Failed to update configuration file. Error " + e.getMessage());
    }
  }
  
  /**
   * Writes alertManagerConfig to configFile in YAML format.
   * Rolls back if it fails to reload the file to the alertmanager.
   * @param alertManagerConfig
   * @throws IOException
   * @throws AlertManagerConfigUpdateException
   */
  public void writeAndReload(AlertManagerConfig alertManagerConfig, AlertManagerClient client)
    throws AlertManagerConfigUpdateException, AlertManagerServerException, AlertManagerConfigReadException {
    AlertManagerConfig alertManagerConfigTmp = read();
    write(alertManagerConfig);
    try {
      client.reload();
    } catch (AlertManagerResponseException e) {
      write(alertManagerConfigTmp);
      throw new AlertManagerConfigUpdateException("Failed to update AlertManagerConfig. " + e.getMessage(), e);
    } catch (AlertManagerServerException se) {
      write(alertManagerConfigTmp);
      throw se;
    }
  }
  
  public static class Builder {
    private String configPath;
  
    public Builder() {
    }
  
    public Builder withConfigPath(String configPath) {
      this.configPath = configPath;
      return this;
    }
  
    private File getConfigFile() throws AlertManagerConfigFileNotFoundException {
      File configFile;
      if (Strings.isNullOrEmpty(this.configPath)) {
        configFile = new File(CONFIG_FILE_PATH);
      } else {
        configFile = new File(this.configPath);
      }
      if (!configFile.exists() || !configFile.isFile()) {
        //We should always have an alertmanager collocated with hopsworks b/c we need to reload the configuration
        //to test if it is valid.
        throw new AlertManagerConfigFileNotFoundException(
          "Alertmanager configuration file not found. Path: " + configFile.getPath());
      } else if (!configFile.canWrite()) {
        throw new AlertManagerConfigFileNotFoundException(
            "Failed to access Alertmanager configuration file. Write permission denied on: " + configFile.getPath());
      }
      return configFile;
    }
    
    public AlertManagerConfigController build() throws AlertManagerConfigFileNotFoundException {
      return new AlertManagerConfigController(getConfigFile());
    }
  }
}
