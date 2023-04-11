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

package io.hops.hopsworks.common.featurestore.storageconnectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.security.secrets.SecretPlaintext;
import io.hops.hopsworks.common.featurestore.OptionDTO;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.security.secrets.SecretsController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnectorType;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.secrets.Secret;
import io.hops.hopsworks.restutils.RESTCodes;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

@Stateless
public class StorageConnectorUtil {
  @EJB
  private SecretsController secretsController;
  @EJB
  private UserFacade userFacade;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private Settings settings;
  
  private ObjectMapper objectMapper = new ObjectMapper();

  public StorageConnectorUtil() { }

  @VisibleForTesting
  public StorageConnectorUtil(Settings settings) {
    this.settings = settings;
  }

  @VisibleForTesting
  public StorageConnectorUtil(Settings settings, DistributedFsService dfs) {
    this.settings = settings;
    this.dfs = dfs;
  }

  /**
   * Creates the secret name as a concatenation of type name and feature store id
   * @param featurestoreId
   * @param connectorName
   * @param connectorType
   * @return
   */
  public String createSecretName(Integer featurestoreId, String connectorName,
                                 FeaturestoreConnectorType connectorType) {
    return connectorType.toString().toLowerCase() + "_" + connectorName.replaceAll(" ", "_").toLowerCase() + "_" +
      featurestoreId;
  }
  
  public <T> T getSecret(Secret secret, Class<T> valueType)
      throws FeaturestoreException {
    T secretClass = null;
    if (secret != null) {
      try {
        Users owner = userFacade.find(secret.getId().getUid());
        
        // check if the calling user is part of the project with the shared feature store is done in feature store
        // service, so we can get the secret here with owner/owner
        SecretPlaintext plainText = secretsController.getShared(owner, owner, secret.getId().getName());
        if (valueType == String.class) {
          secretClass = (T) plainText.getPlaintext();
        } else {
          secretClass = objectMapper.readValue(plainText.getPlaintext(), valueType);
        }
      } catch (UserException | IOException | ServiceException | ProjectException e) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.STORAGE_CONNECTOR_GET_ERROR, Level.FINE,
          "Unable to retrieve Secret " + secret.getId().getName() + " for this storage connector.", e.getMessage());
      }
    }
    return secretClass;
  }
  
  public <T> Secret createProjectSecret(Users user, String secretName, Featurestore featureStore, T secretClass)
      throws ProjectException, UserException {
    if (secretClass != null) {
      String jsonSecretString = serializeSecretClass(secretClass);
      return secretsController.createSecretForProject(user, secretName, jsonSecretString,
        featureStore.getProject().getId());
    }
    return null;
  }
  
  public <T> Secret updateProjectSecret(Users user, Secret secret, String secretName, Featurestore featureStore,
                                        T secretClass)
      throws ProjectException, UserException {
    if (secretClass != null && secret != null) {
      secretsController.checkCanAccessSecret(secret, user);
      try {
        secret.setSecret(secretsController.encryptSecret(serializeSecretClass(secretClass)));
      } catch (IOException | GeneralSecurityException e) {
        throw new UserException(RESTCodes.UserErrorCode.SECRET_ENCRYPTION_ERROR, Level.SEVERE,
          "Error encrypting secret", "Could not encrypt Secret " + secret.getId().getName(), e);
      }
      return secret;
    } else if (secretClass != null){
      return createProjectSecret(user, secretName, featureStore, secretClass);
    } else {
      return null;
    }
  }
  
  public <T> String serializeSecretClass(T secretClass) {
    if (secretClass.getClass() == String.class) {
      return (String) secretClass;
    } else {
      return new JSONObject(secretClass).toString();
    }
  }
  
  public List<OptionDTO> toOptions(String arguments) throws FeaturestoreException {
    if (Strings.isNullOrEmpty(arguments)) {
      return null;
    }

    try {
      OptionDTO[] optionArray = objectMapper.readValue(arguments, OptionDTO[].class);
      return Arrays.asList(optionArray);
    } catch (JsonProcessingException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.STORAGE_CONNECTOR_GET_ERROR, Level.SEVERE,
        "Error deserializing options list provided with connector", e.getMessage());
    }
  }
  
  public String fromOptions(List<OptionDTO> options) {
    if (options == null || options.isEmpty()) {
      return null;
    }
    return new JSONArray(options).toString();
  }
  
  public boolean shouldUpdate(String oldVal, String newVal) {
    return (oldVal == null && newVal != null) || (oldVal != null && !oldVal.equals(newVal));
  }
  
  public boolean shouldUpdate(Integer oldVal, Integer newVal) {
    return (oldVal == null && newVal != null) || (oldVal != null && !oldVal.equals(newVal));
  }

  public boolean isNullOrWhitespace(String val) {
    return Strings.isNullOrEmpty(val) || Strings.isNullOrEmpty(val.trim());
  }

  public String getValueOrNull(String val) {
    return isNullOrWhitespace(val)? null : val.trim();
  }

  public void validatePath(Project project, Users user, String path, String fileType) throws FeaturestoreException {
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps(project, user);
      validatePath(dfso, path, fileType);
    } catch (IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.INFO,
          "Error validating " + fileType, e.getMessage(), e);
    } finally {
      dfs.closeDfsClient(dfso);
    }
  }

  public void validatePath(DistributedFileSystemOps dfso, String path, String fileType)
      throws IOException, FeaturestoreException {
    if (Strings.isNullOrEmpty(path)) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
          fileType + " is mandatory");
    } else if (!dfso.exists(path)) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
          fileType + " does not exists");
    }
  }

  public void removeHdfsFile(Project project, Users user, String keyPath) throws FeaturestoreException {
    if (Strings.isNullOrEmpty(keyPath)){
      throw new IllegalArgumentException("File Path to delete cannot be null or empty");
    }
    DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUsersController.getHdfsUserName(project, user));
    try {
      udfso.rm(keyPath, false);
    } catch (IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.  FAILURE_HDFS_USER_OPERATION, Level.SEVERE,
        "Error deleting file", e.getMessage());
    } finally {
      dfs.closeDfsClient(udfso);
    }
  }

  public Set<FeaturestoreConnectorType> getEnabledStorageConnectorTypes() {
    Set<FeaturestoreConnectorType> types = new HashSet<>();
    // jdbc and hopsfs connectors are always enabled
    types.add(FeaturestoreConnectorType.JDBC);
    types.add(FeaturestoreConnectorType.HOPSFS);
    types.add(FeaturestoreConnectorType.S3);

    if (settings.isSnowflakeStorageConnectorsEnabled()) {
      types.add(FeaturestoreConnectorType.SNOWFLAKE);
    }
    if (settings.isRedshiftStorageConnectorsEnabled()) {
      types.add(FeaturestoreConnectorType.REDSHIFT);
    }
    if (settings.isAdlsStorageConnectorsEnabled()) {
      types.add(FeaturestoreConnectorType.ADLS);
    }
    if (settings.isKafkaStorageConnectorsEnabled()) {
      types.add(FeaturestoreConnectorType.KAFKA);
    }
    if (settings.isGcsStorageConnectorsEnabled()) {
      types.add(FeaturestoreConnectorType.GCS);
    }
    if (settings.isBigqueryStorageConnectorsEnabled()) {
      types.add(FeaturestoreConnectorType.BIGQUERY);
    }
    return types;
  }

  public boolean isStorageConnectorTypeEnabled(FeaturestoreConnectorType storageConnectorType) {
    switch (storageConnectorType) {
      case JDBC:
      case HOPSFS:
      case S3:
        return true;  // always enabled
      case REDSHIFT:
        return settings.isRedshiftStorageConnectorsEnabled();
      case ADLS:
        return settings.isAdlsStorageConnectorsEnabled();
      case SNOWFLAKE:
        return settings.isSnowflakeStorageConnectorsEnabled();
      case KAFKA:
        return settings.isKafkaStorageConnectorsEnabled();
      case GCS:
        return settings.isGcsStorageConnectorsEnabled();
      case BIGQUERY:
        return settings.isBigqueryStorageConnectorsEnabled();
      default:
        throw new IllegalArgumentException("Unknown storage connector type: " + storageConnectorType);
    }
  }
}
