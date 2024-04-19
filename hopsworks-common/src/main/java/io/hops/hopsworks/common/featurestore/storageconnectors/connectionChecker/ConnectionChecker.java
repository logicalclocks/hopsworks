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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.common.cloud.TemporaryCredentialsHelper;
import io.hops.hopsworks.common.featurestore.OptionDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.bigquery.FeaturestoreBigqueryConnectorController;
import io.hops.hopsworks.common.featurestore.storageconnectors.gcs.FeatureStoreGcsConnectorController;
import io.hops.hopsworks.common.featurestore.storageconnectors.jdbc.FeaturestoreJdbcConnectorController;
import io.hops.hopsworks.common.featurestore.storageconnectors.jdbc.FeaturestoreJdbcConnectorDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.redshift.FeaturestoreRedshiftConnectorController;
import io.hops.hopsworks.common.featurestore.storageconnectors.s3.FeaturestoreS3ConnectorController;
import io.hops.hopsworks.common.featurestore.storageconnectors.snowflake.FeaturestoreSnowflakeConnectorController;
import io.hops.hopsworks.common.featurestore.storageconnectors.snowflake.FeaturestoreSnowflakeConnectorDTO;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
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
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.file.PathUtils;

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
import java.util.List;
import java.util.StringJoiner;
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
  @EJB
  private HttpClient httpClient;
  @EJB
  private FeaturestoreRedshiftConnectorController featurestoreRedshiftConnectorController;
  @EJB
  private FeaturestoreS3ConnectorController featurestoreS3ConnectorController;
  @EJB
  private FeaturestoreBigqueryConnectorController featurestoreBigqueryConnectorController;
  @EJB
  private FeatureStoreGcsConnectorController featureStoreGcsConnectorController;
  @EJB
  private TemporaryCredentialsHelper temporaryCredentialsHelper;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private FeaturestoreJdbcConnectorController featurestoreJdbcConnectorController;
  private Path STAGING_PATH;
  private Path REQUEST_SCRATCH_DIR;
  
  @PostConstruct
  public void init() {
    STAGING_PATH = Paths.get(settings.getStagingDir(), "connectors");
  }
  
  public ConnectionCheckerDTO checkConnection(Users user, Project project, Featurestore featurestore,
    FeaturestoreStorageConnectorDTO storageConnectorDto)
    throws FeaturestoreException {
    
    File jsonFile = null;
    // create request scratch directory under STAGING_PATH
    REQUEST_SCRATCH_DIR = Paths.get(STAGING_PATH.toString(),
      String.format("user_%s_project_%d_fs_%d_connector_%s", user.getUsername(), project.getId(),
        featurestore.getId(), storageConnectorDto.getName()));
    try {
      Files.createDirectories(REQUEST_SCRATCH_DIR);
    } catch (IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.CONNECTION_CHECKER_ERROR, Level.SEVERE,
        "Failed to create staging directory", e.getMessage(), e);
    }
    
    switch (storageConnectorDto.getStorageConnectorType()) {
      case SNOWFLAKE:
        // verify dto
        snowflakeConnectorController.verifyConnectorDTO((FeaturestoreSnowflakeConnectorDTO) storageConnectorDto);
        break;
      case JDBC:
        FeaturestoreJdbcConnectorDTO dto = (FeaturestoreJdbcConnectorDTO) storageConnectorDto;
        List<OptionDTO> optionsList = dto.getArguments();
        if (!optionsList.isEmpty()) {
          // append arguments as query parameters to connection string
          dto.setConnectionString(getQueryParamsUrl(optionsList, dto.getConnectionString()));
        }
        if (!Strings.isNullOrEmpty(dto.getDriverPath())) {
          copyKeyFile(project, user, dto.getDriverPath());
        }
        break;
      default:
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_TYPE, Level.FINE,
          "Storage connector type '" + storageConnectorDto.getStorageConnectorType() + "' is not yet supported");
    }
    
    try {
      jsonFile = new File(buildInputFilePath(user, project, featurestore).toUri());
      LOGGER.log(Level.FINE, "Creating input JSON at path {}", jsonFile.getAbsolutePath());
      // create input json file from dto
      ObjectMapper objMapper = httpClient.getObjectMapper();
      // necessary for java.time.Instant which is not supported by default and used by RedshiftDTO expiration field
      objMapper.registerModule(new JavaTimeModule());
      objMapper.writeValue(jsonFile, storageConnectorDto);
      // execute transaction
      ProcessResult result = execute(jsonFile.toPath());
      LOGGER.log(
        Level.FINE, () ->
          String.format("Output response for connection test: %s%s", result.getStdout(), result.getStderr())
      );
      // set result
      ConnectionCheckerDTO outputDto = new ConnectionCheckerDTO();
      outputDto.setConnectionOutput(String.format("%s%s", result.getStdout(), result.getStderr()));
      outputDto.setStatusCode(result.getExitCode());
      return outputDto;
    } catch (IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.CONNECTION_CHECKER_ERROR, Level.SEVERE,
        "Failed to create JSON file from input request", e.getMessage(), e);
    } finally {
      deleteStagedDir();
    }
  }
  
  private static String getQueryParamsUrl(List<OptionDTO> optionsList, String connectionString) {
    StringJoiner sj;
    if (connectionString.contains("?")) {
      sj = new StringJoiner("&", "&", "");
    } else {
      sj = new StringJoiner("&", "?", "");
    }
    // Loop through the optionsList and add each name-value pair to the StringJoiner
    for (OptionDTO args : optionsList) {
      sj.add(args.getName() + "=" + args.getValue());
    }
    // Append the StringJoiner result to the connectionString
    connectionString += sj.toString();
    return connectionString;
  }
  
  private void copyKeyFile(Project project, Users user, String keyPath) throws FeaturestoreException {
    DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUsersController.getHdfsUserName(project, user));
    try {
      udfso.copyToLocal(keyPath, REQUEST_SCRATCH_DIR.toString());
    } catch (IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.CONNECTION_CHECKER_ERROR, Level.SEVERE,
        "Failed to copy key file from HDFS", "", e);
    } finally {
      dfs.closeDfsClient(udfso);
    }
  }
  
  
  private Path buildInputFilePath(Users user, Project project, Featurestore fs) throws FeaturestoreException {
    try {
      Files.createDirectories(REQUEST_SCRATCH_DIR);
    } catch (IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.CONNECTION_CHECKER_ERROR, Level.SEVERE,
        "Failed to create staging directory", e.getMessage(), e);
    }
    return Paths.get(REQUEST_SCRATCH_DIR.toString(), String.format("%s_%s_%s_%s.json", user.getUsername(),
      project.getId(), fs.getId(), System.currentTimeMillis()));
  }
  
  private void getContainerLogs(Path logFile) throws FeaturestoreException {
    if (Files.exists(logFile)) {
      try {
        LOGGER.log(Level.FINE, "Reading container logs from path {}", logFile.toAbsolutePath());
        String logLines = String.join("\n", Files.readAllLines(logFile));
        if (!Strings.isNullOrEmpty(logLines)) {
          LOGGER.log(Level.INFO, logLines);
        }
      } catch (IOException e) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FILE_READ_ERROR, Level.SEVERE,
          String.format("Failure during reading container logs %s", logFile), "", e);
      } finally {
        deleteStagedDir();
      }
    }
  }
  
  private void deleteStagedDir() throws FeaturestoreException {
    try {
      PathUtils.delete(REQUEST_SCRATCH_DIR);
      LOGGER.log(Level.FINE, "Deleted staged directory: {}", REQUEST_SCRATCH_DIR);
    } catch (IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FILE_DELETION_ERROR, Level.SEVERE,
        String.format("Failure during cleaning up staged directory %s", REQUEST_SCRATCH_DIR), "", e);
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
      if (processResult.getExitCode() != 0) {
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