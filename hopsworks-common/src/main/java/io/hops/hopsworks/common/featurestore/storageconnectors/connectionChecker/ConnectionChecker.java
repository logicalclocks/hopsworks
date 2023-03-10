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

package io.hops.hopsworks.common.featurestore.storageconnectors.connectionChecker;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.snowflake.FeaturestoreSnowflakeConnectorController;
import io.hops.hopsworks.common.featurestore.storageconnectors.snowflake.FeaturestoreSnowflakeConnectorDTO;
import io.hops.hopsworks.common.proxies.client.HttpClient;
import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.ProcessDescriptor;
import io.hops.hopsworks.common.util.ProcessResult;
import io.hops.hopsworks.common.util.ProjectUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.opensearch.common.Strings;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ConnectionChecker {
  private static final Logger LOGGER = Logger.getLogger(ConnectionChecker.class.getName());
  @EJB
  private OSProcessExecutor osProcessExecutor;
  @EJB
  private Settings settings;
  @EJB
  private FeaturestoreSnowflakeConnectorController snowflakeConnectorController;
  @EJB
  private ProjectUtils projectUtils;
  private Path STAGING_PATH;
  @EJB
  private HttpClient httpClient;
  
  @PostConstruct
  public void init() {
    STAGING_PATH = Paths.get(settings.getStagingDir(), "connectors");
  }
  
  public ConnectionCheckerDTO checkConnection(Users user, Project project, Featurestore featurestore,
    FeaturestoreStorageConnectorDTO storageConnectorDto) throws FeaturestoreException {
    
    File jsonFile = null;
    switch (storageConnectorDto.getStorageConnectorType()) {
      case SNOWFLAKE:
        // verify dto
        snowflakeConnectorController.verifyConnectorDTO((FeaturestoreSnowflakeConnectorDTO) storageConnectorDto);
        break;
      default:
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_TYPE, Level.FINE,
          "Storage connector type '" + storageConnectorDto.getStorageConnectorType() + "' is not yet supported");
    }
    
    try {
      Files.createDirectories(STAGING_PATH);
    } catch (IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.CONNECTION_CHECKER_ERROR, Level.SEVERE,
        "Failed to create staging directory", e.getMessage(), e);
    }
    
    try {
      jsonFile = new File(buildInputFilePath(user, project, featurestore).toUri());
      LOGGER.log(Level.FINE, String.format("Creating input JSON at path %S", jsonFile.getAbsolutePath()));
      // create input json file from dto
      httpClient.getObjectMapper().writeValue(jsonFile, storageConnectorDto);
      // execute transaction
      ProcessResult result = execute(jsonFile.toPath());
      LOGGER.log(
        Level.FINE,
        String.format("Output response for connection test: %s%s", result.getStdout(), result.getStderr())
      );
      // set result
      ConnectionCheckerDTO outputDto = new ConnectionCheckerDTO();
      outputDto.setConnectionOutput(String.format("%s%s", result.getStdout(), result.getStderr()));
      outputDto.setStatusCode(result.getExitCode());
      outputDto.setStorageConnectorDTO(storageConnectorDto);
      
      return outputDto;
    } catch (IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.CONNECTION_CHECKER_ERROR, Level.SEVERE,
        "Failed to create JSON file from input request", e.getMessage(), e);
    } finally {
      deleteStagedFile(jsonFile);
    }
  }
  
  private Path buildInputFilePath(Users user, Project project, Featurestore fs) {
    return Paths.get(STAGING_PATH.toString(), String.format("%s_%s_%s_%s.json", user.getUsername(),
      project.getId(), fs.getId(), System.currentTimeMillis()));
  }
  
  private void getContainerLogs(Path logFile) throws FeaturestoreException {
    if (Files.exists(logFile)) {
      try {
        LOGGER.log(Level.FINE, String.format("Reading container logs from path %s", logFile.toAbsolutePath()));
        String logLines = String.join("\n", Files.readAllLines(logFile));
        if (!Strings.isNullOrEmpty(logLines)) {
          LOGGER.log(Level.INFO, logLines);
        }
      } catch (IOException e) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FILE_READ_ERROR, Level.SEVERE,
          String.format("Failure during reading container logs %s", logFile), "", e);
      } finally {
        deleteStagedFile(logFile.toFile());
      }
    }
  }
  
  private void deleteStagedFile(File file) throws FeaturestoreException {
    if (file != null) {
      try {
        FileUtils.delete(file);
        LOGGER.log(Level.FINE, "Deleted staged file: " + file.getAbsolutePath());
      } catch (IOException e) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FILE_DELETION_ERROR, Level.SEVERE,
          String.format("Failure during cleaning up staging file %s", file.getAbsolutePath()), "", e);
      }
    }
  }
  
  protected ProcessResult execute(Path jsonFile) throws FeaturestoreException {
    
    try {
      ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .setWaitTimeout(10, TimeUnit.SECONDS)
        .addCommand("/usr/bin/sudo")
        .addCommand(Paths.get(settings.getSudoersDir(), settings.getTEST_CONNECTOR_LAUNCHER()).toString())
        .addCommand(jsonFile.toString())
        .addCommand(projectUtils.getFullDockerImageName(settings.getTestConnectorImage()))
        .build();
      
      String containerLogFileName = FilenameUtils.getBaseName(jsonFile.toString()).concat(".log");
      LOGGER.log(Level.INFO, "Executing process to start docker container for testing connection");
      ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
      if (processResult.getExitCode()!=0) {
        getContainerLogs(Paths.get(jsonFile.getParent().toString(), containerLogFileName));        
      }
      LOGGER.log(Level.INFO, "Ended process of docker container for testing connection");
      return processResult;
    } catch (ServiceDiscoveryException e) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.DOCKER_FULLNAME_ERROR, Level.SEVERE,
        String.format("Full docker image not found for image %s", settings.getTestConnectorImage()),
        e.getMessage(), e
      );
    } catch (IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.CONNECTION_CHECKER_LAUNCH_ERROR, Level.SEVERE,
        "Could not execute connection check", e.getMessage(), e);
    }
  }
  
}