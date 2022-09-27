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

package io.hops.hopsworks.common.featurestore.storageconnectors.snowflake;

import io.hops.hopsworks.common.featurestore.storageconnectors.StorageConnectorUtil;
import com.google.common.base.Strings;
import io.hops.hopsworks.common.security.secrets.SecretsController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.snowflake.FeaturestoreSnowflakeConnector;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.secrets.Secret;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.Transactional;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeaturestoreSnowflakeConnectorController {
  
  private static final Logger LOGGER = Logger.getLogger(FeaturestoreSnowflakeConnectorController.class.getName());
  
  @EJB
  private SecretsController secretsController;
  @EJB
  private StorageConnectorUtil storageConnectorUtil;

  // added for unit testing
  public FeaturestoreSnowflakeConnectorController(SecretsController secretsController,
      StorageConnectorUtil storageConnectorUtil) {
    this.secretsController = secretsController;
    this.storageConnectorUtil = storageConnectorUtil;
  }
  
  public FeaturestoreSnowflakeConnectorController() {
  }
  
  public FeaturestoreSnowflakeConnectorDTO getConnector(FeaturestoreConnector featurestoreConnector)
      throws FeaturestoreException {
    FeaturestoreSnowflakeConnectorDTO snowflakeConnectorDTO =
        new FeaturestoreSnowflakeConnectorDTO(featurestoreConnector);
    snowflakeConnectorDTO.setSfOptions(
      storageConnectorUtil.toOptions(featurestoreConnector.getSnowflakeConnector().getArguments()));
    snowflakeConnectorDTO.setPassword(
      storageConnectorUtil.getSecret(featurestoreConnector.getSnowflakeConnector().getPwdSecret(), String.class));
    snowflakeConnectorDTO.setToken(
      storageConnectorUtil.getSecret(featurestoreConnector.getSnowflakeConnector().getTokenSecret(), String.class));
    return snowflakeConnectorDTO;
  }
  
  public FeaturestoreSnowflakeConnector createConnector(Users user, Featurestore featurestore,
      FeaturestoreSnowflakeConnectorDTO featurestoreSnowflakeConnectorDTO) throws FeaturestoreException, UserException,
      ProjectException {
    verifyConnectorDTO(featurestoreSnowflakeConnectorDTO);
    Secret secret = createSecret(user, featurestore, featurestoreSnowflakeConnectorDTO);
    FeaturestoreSnowflakeConnector snowflakeConnector = new FeaturestoreSnowflakeConnector();
    setConnector(snowflakeConnector, secret, featurestoreSnowflakeConnectorDTO);
    return snowflakeConnector;
  }
  
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  @Transactional(rollbackOn = FeaturestoreException.class)
  public FeaturestoreSnowflakeConnector updateConnector(Users user,
      FeaturestoreSnowflakeConnectorDTO featurestoreSnowflakeConnectorDTO,
      FeaturestoreSnowflakeConnector snowflakeConnector) throws FeaturestoreException, UserException,
      ProjectException {
    verifyConnectorDTO(featurestoreSnowflakeConnectorDTO);
    Secret secret = updateSecret(user, featurestoreSnowflakeConnectorDTO, snowflakeConnector);
    setConnector(snowflakeConnector, secret, featurestoreSnowflakeConnectorDTO);
    return snowflakeConnector;
  }
  
  private void setConnector(FeaturestoreSnowflakeConnector snowflakeConnector, Secret secret,
      FeaturestoreSnowflakeConnectorDTO featurestoreSnowflakeConnectorDTO) {
    snowflakeConnector.setUrl(storageConnectorUtil.getValueOrNull(featurestoreSnowflakeConnectorDTO.getUrl()));
    snowflakeConnector.setDatabaseUser(
      storageConnectorUtil.getValueOrNull(featurestoreSnowflakeConnectorDTO.getUser()));
    snowflakeConnector.setDatabaseName(
      storageConnectorUtil.getValueOrNull(featurestoreSnowflakeConnectorDTO.getDatabase()));
    snowflakeConnector.setDatabaseSchema(
      storageConnectorUtil.getValueOrNull(featurestoreSnowflakeConnectorDTO.getSchema()));
    snowflakeConnector.setTableName(storageConnectorUtil.getValueOrNull(featurestoreSnowflakeConnectorDTO.getTable()));
    snowflakeConnector.setRole(storageConnectorUtil.getValueOrNull(featurestoreSnowflakeConnectorDTO.getRole()));
    snowflakeConnector.setWarehouse(
      storageConnectorUtil.getValueOrNull(featurestoreSnowflakeConnectorDTO.getWarehouse()));
    snowflakeConnector.setArguments(storageConnectorUtil.fromOptions(featurestoreSnowflakeConnectorDTO.getSfOptions()));
    snowflakeConnector.setApplication(featurestoreSnowflakeConnectorDTO.getApplication());
    if (!Strings.isNullOrEmpty(featurestoreSnowflakeConnectorDTO.getPassword())) {
      snowflakeConnector.setPwdSecret(secret);
      snowflakeConnector.setTokenSecret(null);
    } else {
      snowflakeConnector.setTokenSecret(secret);
      snowflakeConnector.setPwdSecret(null);
    }
  }

  private Secret getSecret(FeaturestoreSnowflakeConnector snowflakeConnector) {
    if (snowflakeConnector.getPwdSecret() != null) {
      return snowflakeConnector.getPwdSecret();
    } else {
      return snowflakeConnector.getTokenSecret();
    }
  }
  
  private Secret createSecret(Users user, Featurestore featurestore,
      FeaturestoreSnowflakeConnectorDTO featurestoreSnowflakeConnectorDTO) throws ProjectException, UserException {
    String secretName = storageConnectorUtil.createSecretName(featurestore.getId(),
      featurestoreSnowflakeConnectorDTO.getName(), featurestoreSnowflakeConnectorDTO.getStorageConnectorType());
    Secret secret;
    if (!Strings.isNullOrEmpty(featurestoreSnowflakeConnectorDTO.getPassword())) {
      secret = secretsController.createSecretForProject(user, secretName,
          featurestoreSnowflakeConnectorDTO.getPassword(), featurestore.getProject().getId());
    } else {
      secret = secretsController.createSecretForProject(user, secretName,
          featurestoreSnowflakeConnectorDTO.getToken(), featurestore.getProject().getId());
    }
    return secret;
  }
  
  private Secret updateSecret(Users user, FeaturestoreSnowflakeConnectorDTO featurestoreSnowflakeConnectorDTO,
      FeaturestoreSnowflakeConnector snowflakeConnector) throws UserException, ProjectException {
    String secret;
    Secret existingSecret = getSecret(snowflakeConnector);
    secretsController.checkCanAccessSecret(existingSecret, user);
    if (!Strings.isNullOrEmpty(featurestoreSnowflakeConnectorDTO.getPassword())) {
      secret = featurestoreSnowflakeConnectorDTO.getPassword();
    } else {
      secret = featurestoreSnowflakeConnectorDTO.getToken();
    }
    try {
      existingSecret.setSecret(secretsController.encryptSecret(secret));
    } catch (IOException | GeneralSecurityException e) {
      throw new UserException(RESTCodes.UserErrorCode.SECRET_ENCRYPTION_ERROR, Level.SEVERE,
          "Error encrypting secret", "Could not encrypt Secret " + existingSecret.getId().getName(), e);
    }
    return existingSecret;
  }
  
  public void verifyConnectorDTO(FeaturestoreSnowflakeConnectorDTO featurestoreSnowflakeConnectorDTO)
      throws FeaturestoreException {
    if (featurestoreSnowflakeConnectorDTO == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
          "Null input data");
    }
    if (storageConnectorUtil.isNullOrWhitespace(featurestoreSnowflakeConnectorDTO.getUrl())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
          "Url can not be empty");
    }
    if (storageConnectorUtil.isNullOrWhitespace(featurestoreSnowflakeConnectorDTO.getUser())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
          "User can not be empty");
    }
    if (storageConnectorUtil.isNullOrWhitespace(featurestoreSnowflakeConnectorDTO.getSchema())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
          "Schema can not be empty");
    }
    if (storageConnectorUtil.isNullOrWhitespace(featurestoreSnowflakeConnectorDTO.getDatabase())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
          "Database can not be empty");
    }
    if (Strings.isNullOrEmpty(featurestoreSnowflakeConnectorDTO.getPassword()) &&
        Strings.isNullOrEmpty(featurestoreSnowflakeConnectorDTO.getToken())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
          "Password or OAuth token must be set");
    }
    if (!Strings.isNullOrEmpty(featurestoreSnowflakeConnectorDTO.getPassword()) &&
        !Strings.isNullOrEmpty(featurestoreSnowflakeConnectorDTO.getToken())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
          "Only one authentication method is allowed");
    }
    if (storageConnectorUtil.isNullOrWhitespace(featurestoreSnowflakeConnectorDTO.getWarehouse())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
        "Warehouse can not be empty");
    }
  }
}
