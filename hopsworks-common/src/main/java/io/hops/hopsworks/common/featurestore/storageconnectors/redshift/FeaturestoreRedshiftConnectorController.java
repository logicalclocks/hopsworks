/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.featurestore.storageconnectors.redshift;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.user.security.secrets.SecretsFacade;
import io.hops.hopsworks.common.featurestore.FeaturestoreConstants;
import io.hops.hopsworks.common.featurestore.storageconnectors.StorageConnectorUtil;
import io.hops.hopsworks.common.security.secrets.SecretsController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.redshift.FeatureStoreRedshiftConnector;
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
public class FeaturestoreRedshiftConnectorController {

  private static final Logger LOGGER = Logger.getLogger(FeaturestoreRedshiftConnectorController.class.getName());
  
  @EJB
  private SecretsController secretsController;
  @EJB
  private SecretsFacade secretsFacade;
  @EJB
  private Settings settings;
  @EJB
  private StorageConnectorUtil storageConnectorUtil;

  public FeaturestoreRedshiftConnectorDTO getRedshiftConnectorDTO(FeaturestoreConnector featurestoreConnector)
      throws FeaturestoreException {
    FeaturestoreRedshiftConnectorDTO featurestoreRedshiftConnectorDTO =
        new FeaturestoreRedshiftConnectorDTO(featurestoreConnector);
    featurestoreRedshiftConnectorDTO.setDatabasePassword(storageConnectorUtil.getSecret(
      featurestoreConnector.getRedshiftConnector().getSecret(), String.class));
    featurestoreRedshiftConnectorDTO.setArguments(
      storageConnectorUtil.toOptions(featurestoreConnector.getRedshiftConnector().getArguments()));
    return featurestoreRedshiftConnectorDTO;
  }
  
  /**
   *
   * @param featurestore
   * @param featurestoreRedshiftConnectorDTO
   * @return
   */
  public FeatureStoreRedshiftConnector createFeaturestoreRedshiftConnector(Users user, Featurestore featurestore,
      FeaturestoreRedshiftConnectorDTO featurestoreRedshiftConnectorDTO)
      throws FeaturestoreException, UserException, ProjectException {
    verifyCreateDTO(featurestoreRedshiftConnectorDTO);
    FeatureStoreRedshiftConnector featurestoreRedshiftConnector = new FeatureStoreRedshiftConnector();
    setConnector(featurestoreRedshiftConnector, featurestoreRedshiftConnectorDTO);
    setPassword(user, featurestoreRedshiftConnectorDTO, featurestore, featurestoreRedshiftConnector);
    return featurestoreRedshiftConnector;
  }
  
  private void setConnector(FeatureStoreRedshiftConnector featurestoreRedshiftConnector,
      FeaturestoreRedshiftConnectorDTO featurestoreRedshiftConnectorDTO) {
    featurestoreRedshiftConnector.setClusterIdentifier(
      storageConnectorUtil.getValueOrNull(featurestoreRedshiftConnectorDTO.getClusterIdentifier()));
    featurestoreRedshiftConnector.setDatabaseDriver(
      storageConnectorUtil.getValueOrNull(featurestoreRedshiftConnectorDTO.getDatabaseDriver()));
    featurestoreRedshiftConnector.setDatabaseEndpoint(
      storageConnectorUtil.getValueOrNull(featurestoreRedshiftConnectorDTO.getDatabaseEndpoint()));
    featurestoreRedshiftConnector.setDatabaseName(
      storageConnectorUtil.getValueOrNull(featurestoreRedshiftConnectorDTO.getDatabaseName()));
    featurestoreRedshiftConnector.setDatabasePort(featurestoreRedshiftConnectorDTO.getDatabasePort());
    featurestoreRedshiftConnector.setTableName(
      storageConnectorUtil.getValueOrNull(featurestoreRedshiftConnectorDTO.getTableName()));
    featurestoreRedshiftConnector.setDatabaseUserName(
      storageConnectorUtil.getValueOrNull(featurestoreRedshiftConnectorDTO.getDatabaseUserName()));
    featurestoreRedshiftConnector.setIamRole(
      storageConnectorUtil.getValueOrNull(featurestoreRedshiftConnectorDTO.getIamRole()));
    featurestoreRedshiftConnector.setAutoCreate(featurestoreRedshiftConnectorDTO.getAutoCreate());
    featurestoreRedshiftConnector.setDatabaseGroup(
      storageConnectorUtil.getValueOrNull(featurestoreRedshiftConnectorDTO.getDatabaseGroup()));
    featurestoreRedshiftConnector.setArguments(
      storageConnectorUtil.getValueOrNull(
        storageConnectorUtil.fromOptions(featurestoreRedshiftConnectorDTO.getArguments())));
  }
  
  private void setPassword(Users user, FeaturestoreRedshiftConnectorDTO featurestoreRedshiftConnectorDTO,
    Featurestore featurestore, FeatureStoreRedshiftConnector featurestoreRedshiftConnector)
    throws UserException, ProjectException {
    if (!Strings.isNullOrEmpty(featurestoreRedshiftConnectorDTO.getDatabasePassword())) {
      Secret secret = secretsController
        .createSecretForProject(user,
          storageConnectorUtil.createSecretName(featurestore.getId(), featurestoreRedshiftConnectorDTO.getName(),
            featurestoreRedshiftConnectorDTO.getStorageConnectorType()),
          featurestoreRedshiftConnectorDTO.getDatabasePassword(), featurestore.getProject().getId());
      featurestoreRedshiftConnector.setSecret(secret);
    }
  }
  
  private void verifyCreateDTO(FeaturestoreRedshiftConnectorDTO featurestoreRedshiftConnectorDTO)
      throws FeaturestoreException {
    if (featurestoreRedshiftConnectorDTO == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
          "Null input data");
    }
    if (storageConnectorUtil.isNullOrWhitespace(featurestoreRedshiftConnectorDTO.getClusterIdentifier())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
          "Cluster identifier can not be empty.");
    }
    if (storageConnectorUtil.isNullOrWhitespace(featurestoreRedshiftConnectorDTO.getDatabaseDriver())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
          "Database driver can not be empty.");
    }
    if (storageConnectorUtil.isNullOrWhitespace(featurestoreRedshiftConnectorDTO.getDatabaseEndpoint())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
          "Database endpoint can not be empty.");
    }
    if (storageConnectorUtil.isNullOrWhitespace(featurestoreRedshiftConnectorDTO.getDatabaseName())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
          "Database name can not be empty.");
    }
    if (featurestoreRedshiftConnectorDTO.getDatabasePort() == null ||
        featurestoreRedshiftConnectorDTO.getDatabasePort() < 1150 ||
        featurestoreRedshiftConnectorDTO.getDatabasePort() > 65535) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
          "Database port should be between 1150 and 65535.");
    }
    // if auto create is null, assume user sent a database user name
    if ((featurestoreRedshiftConnectorDTO.getAutoCreate() == null || !featurestoreRedshiftConnectorDTO.getAutoCreate())
      && storageConnectorUtil.isNullOrWhitespace(featurestoreRedshiftConnectorDTO.getDatabaseUserName())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
          "Database username can not be empty.");
    }
    String arguments = storageConnectorUtil.fromOptions(featurestoreRedshiftConnectorDTO.getArguments());
    if(!Strings.isNullOrEmpty(arguments)
      && arguments.length() > FeaturestoreConstants.STORAGE_CONNECTOR_ARGUMENTS_MAX_LENGTH) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
        "Redshift connection arguments should not exceed: " +
          FeaturestoreConstants.STORAGE_CONNECTOR_ARGUMENTS_MAX_LENGTH + " characters");
    }
    verifyPassword(featurestoreRedshiftConnectorDTO);
  }

  private void verifyPassword(FeaturestoreRedshiftConnectorDTO featurestoreRedshiftConnectorDTO)
      throws FeaturestoreException {
    verifyPassword(featurestoreRedshiftConnectorDTO.getIamRole(),
        featurestoreRedshiftConnectorDTO.getDatabasePassword());
  }
  
  private void verifyPassword(String iamRole, String password) throws FeaturestoreException {
    if (!Strings.isNullOrEmpty(iamRole) && !Strings.isNullOrEmpty(password)) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE, "Database password is not allowed.");
    }
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  @Transactional(rollbackOn = FeaturestoreException.class)
  public FeatureStoreRedshiftConnector updateFeaturestoreRedshiftConnector(Users user, Featurestore featurestore,
      FeaturestoreRedshiftConnectorDTO featurestoreRedshiftConnectorDTO,
      FeatureStoreRedshiftConnector featureStoreRedshiftConnector)
      throws FeaturestoreException, UserException, ProjectException {

    verifyCreateDTO(featurestoreRedshiftConnectorDTO);
    setConnector(featureStoreRedshiftConnector, featurestoreRedshiftConnectorDTO);

    Secret secret = null;
    if (storageConnectorUtil.shouldUpdate(
      storageConnectorUtil.getSecret(featureStoreRedshiftConnector.getSecret(), String.class),
      featurestoreRedshiftConnectorDTO.getDatabasePassword())) {
      secret = updatePassword(user, featurestoreRedshiftConnectorDTO, featurestore, featureStoreRedshiftConnector);
    }
    
    featureStoreRedshiftConnector.setArguments(
      storageConnectorUtil.fromOptions(featurestoreRedshiftConnectorDTO.getArguments()));
    
    if (featureStoreRedshiftConnector.getSecret() == null && secret != null) {
      secretsFacade.deleteSecret(secret.getId());
    }

    return featureStoreRedshiftConnector;
  }
  
  private Secret updatePassword(Users user, FeaturestoreRedshiftConnectorDTO featurestoreRedshiftConnectorDTO,
    Featurestore featurestore, FeatureStoreRedshiftConnector featureStoreRedshiftConnector)
    throws UserException, ProjectException {
    Secret secret = featureStoreRedshiftConnector.getSecret();
    if (secret != null) {
      secretsController.checkCanAccessSecret(secret, user);
    }
    if (secret == null && !Strings.isNullOrEmpty(featurestoreRedshiftConnectorDTO.getDatabasePassword())) {
      setPassword(user, featurestoreRedshiftConnectorDTO, featurestore, featureStoreRedshiftConnector);
    } else if (!Strings.isNullOrEmpty(featurestoreRedshiftConnectorDTO.getDatabasePassword())) {
      try {
        secret.setSecret(secretsController.encryptSecret(featurestoreRedshiftConnectorDTO.getDatabasePassword()));
      } catch (IOException | GeneralSecurityException e) {
        throw new UserException(RESTCodes.UserErrorCode.SECRET_ENCRYPTION_ERROR, Level.SEVERE,
          "Error encrypting secret", "Could not encrypt Secret " + secret.getId().getName(), e);
      }
    } else {
      featureStoreRedshiftConnector.setSecret(null);
      //Secret can't be removed here b/c of ON DELETE RESTRICT
    }
    return secret;
  }
}
