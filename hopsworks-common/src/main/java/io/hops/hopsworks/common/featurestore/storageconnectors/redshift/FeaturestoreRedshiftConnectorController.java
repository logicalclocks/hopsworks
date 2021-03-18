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
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.security.secrets.SecretsFacade;
import io.hops.hopsworks.common.featurestore.FeaturestoreConstants;
import io.hops.hopsworks.common.security.secrets.SecretsController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;
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
  private UserFacade userFacade;

  public FeaturestoreRedshiftConnectorDTO getRedshiftConnectorDTO(Users user,
                                                                  FeaturestoreConnector featurestoreConnector) {
    FeaturestoreRedshiftConnectorDTO featurestoreRedshiftConnectorDTO =
        new FeaturestoreRedshiftConnectorDTO(featurestoreConnector);
    featurestoreRedshiftConnectorDTO.setDatabasePassword(
        getDatabasePassword(featurestoreConnector.getRedshiftConnector(), user));
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
    featurestoreRedshiftConnector.setClusterIdentifier(featurestoreRedshiftConnectorDTO.getClusterIdentifier());
    featurestoreRedshiftConnector.setDatabaseDriver(featurestoreRedshiftConnectorDTO.getDatabaseDriver());
    featurestoreRedshiftConnector.setDatabaseEndpoint(featurestoreRedshiftConnectorDTO.getDatabaseEndpoint());
    featurestoreRedshiftConnector.setDatabaseName(featurestoreRedshiftConnectorDTO.getDatabaseName());
    featurestoreRedshiftConnector.setDatabasePort(featurestoreRedshiftConnectorDTO.getDatabasePort());
    featurestoreRedshiftConnector.setTableName(featurestoreRedshiftConnectorDTO.getTableName());
    featurestoreRedshiftConnector.setDatabaseUserName(featurestoreRedshiftConnectorDTO.getDatabaseUserName());
    setPassword(user, featurestoreRedshiftConnectorDTO, featurestore, featurestoreRedshiftConnector);
    featurestoreRedshiftConnector.setIamRole(featurestoreRedshiftConnectorDTO.getIamRole());
    featurestoreRedshiftConnector.setAutoCreate(featurestoreRedshiftConnectorDTO.getAutoCreate());
    featurestoreRedshiftConnector.setDatabaseGroup(featurestoreRedshiftConnectorDTO.getDatabaseGroup());
    featurestoreRedshiftConnector.setArguments(featurestoreRedshiftConnectorDTO.getArguments());
    return featurestoreRedshiftConnector;
  }

  private void setPassword(Users user, FeaturestoreRedshiftConnectorDTO featurestoreRedshiftConnectorDTO,
    Featurestore featurestore, FeatureStoreRedshiftConnector featurestoreRedshiftConnector)
    throws UserException, ProjectException {
    if (!Strings.isNullOrEmpty(featurestoreRedshiftConnectorDTO.getDatabasePassword())) {
      Secret secret = secretsController
        .createSecretForProject(user, createSecretName(featurestore, featurestoreRedshiftConnectorDTO.getName()),
          featurestoreRedshiftConnectorDTO.getDatabasePassword(), featurestore.getProject().getId());
      featurestoreRedshiftConnector.setSecret(secret);
    }
  }

  private String createSecretName(Featurestore featurestore, String connectorName) {
    return "redshift_" + connectorName.replaceAll(" ", "_").toLowerCase() + "_" + featurestore.getId();
  }

  private void verifyCreateDTO(FeaturestoreRedshiftConnectorDTO featurestoreRedshiftConnectorDTO)
      throws FeaturestoreException {
    if(!Strings.isNullOrEmpty(featurestoreRedshiftConnectorDTO.getArguments())
      && featurestoreRedshiftConnectorDTO.getArguments().length() >
      FeaturestoreConstants.JDBC_STORAGE_CONNECTOR_ARGUMENTS_MAX_LENGTH) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
        "Redshift connection arguments should not exceed: " +
          FeaturestoreConstants.JDBC_STORAGE_CONNECTOR_ARGUMENTS_MAX_LENGTH + " characters");
    }
    verifyPassword(featurestoreRedshiftConnectorDTO);
  }

  private void verifyPassword(FeaturestoreRedshiftConnectorDTO featurestoreRedshiftConnectorDTO)
      throws FeaturestoreException {
    verifyPassword(featurestoreRedshiftConnectorDTO.getIamRole(),
        featurestoreRedshiftConnectorDTO.getDatabasePassword());
  }

  private void verifyPassword(String iamRole, String password) throws FeaturestoreException {
    boolean needPassword = !settings.isIAMRoleConfigured() && Strings.isNullOrEmpty(iamRole);
    if (needPassword && Strings.isNullOrEmpty(password)) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE, "Database password not set.");
    } else if (!needPassword && !Strings.isNullOrEmpty(password)) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE, "Database password is not allowed.");
    }
  }

  private void verifyPassword(String iamRole, Secret secret) throws FeaturestoreException {
    boolean needPassword = !settings.isIAMRoleConfigured() && Strings.isNullOrEmpty(iamRole);
    if (needPassword && secret == null) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE, "Database password not set.");
    } else if (!needPassword && secret != null) {
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

    if (!Strings.isNullOrEmpty(featurestoreRedshiftConnectorDTO.getClusterIdentifier()) &&
        !featurestoreRedshiftConnectorDTO.getClusterIdentifier()
            .equals(featureStoreRedshiftConnector.getClusterIdentifier())) {
      featureStoreRedshiftConnector.setClusterIdentifier(featurestoreRedshiftConnectorDTO.getClusterIdentifier());
    }
    if (shouldUpdate(featureStoreRedshiftConnector.getDatabaseDriver(),
        featurestoreRedshiftConnectorDTO.getDatabaseDriver())) {
      featureStoreRedshiftConnector.setDatabaseDriver(featurestoreRedshiftConnectorDTO.getDatabaseDriver());
    }
    if (shouldUpdate(featureStoreRedshiftConnector.getDatabaseEndpoint(),
        featurestoreRedshiftConnectorDTO.getDatabaseEndpoint())) {
      featureStoreRedshiftConnector.setDatabaseEndpoint(featurestoreRedshiftConnectorDTO.getDatabaseEndpoint());
    }
    if (shouldUpdate(featureStoreRedshiftConnector.getDatabaseName(),
        featurestoreRedshiftConnectorDTO.getDatabaseName())) {
      featureStoreRedshiftConnector.setDatabaseName(featurestoreRedshiftConnectorDTO.getDatabaseName());
    }
    if (shouldUpdate(featureStoreRedshiftConnector.getDatabaseUserName(),
        featurestoreRedshiftConnectorDTO.getDatabaseUserName())) {
      featureStoreRedshiftConnector.setDatabaseUserName(featurestoreRedshiftConnectorDTO.getDatabaseUserName());
    }
    if (shouldUpdate(featureStoreRedshiftConnector.getDatabaseGroup(),
        featurestoreRedshiftConnectorDTO.getDatabaseGroup())) {
      featureStoreRedshiftConnector.setDatabaseGroup(featurestoreRedshiftConnectorDTO.getDatabaseGroup());
    }
    if (featurestoreRedshiftConnectorDTO.getAutoCreate() != featureStoreRedshiftConnector.getAutoCreate()) {
      featureStoreRedshiftConnector.setAutoCreate(featurestoreRedshiftConnectorDTO.getAutoCreate());
    }
    if (shouldUpdate(featurestoreRedshiftConnectorDTO.getDatabasePort(),
        featureStoreRedshiftConnector.getDatabasePort())) {
      featureStoreRedshiftConnector.setDatabasePort(featurestoreRedshiftConnectorDTO.getDatabasePort());
    }
    if (shouldUpdate(featureStoreRedshiftConnector.getTableName(),
        featurestoreRedshiftConnectorDTO.getTableName())) {
      featureStoreRedshiftConnector.setTableName(featurestoreRedshiftConnectorDTO.getTableName());
    }
    if (shouldUpdate(featureStoreRedshiftConnector.getIamRole(),
        featurestoreRedshiftConnectorDTO.getIamRole())) {
      featureStoreRedshiftConnector.setIamRole(featurestoreRedshiftConnectorDTO.getIamRole());
    }

    Secret secret = null;
    if (shouldUpdate(getDatabasePassword(featureStoreRedshiftConnector, user),
            featurestoreRedshiftConnectorDTO.getDatabasePassword())) {
      secret = updatePassword(user, featurestoreRedshiftConnectorDTO, featurestore, featureStoreRedshiftConnector);
    }

    if (shouldUpdate(featureStoreRedshiftConnector.getArguments(),
      featurestoreRedshiftConnectorDTO.getArguments())) {
      featureStoreRedshiftConnector.setArguments(featurestoreRedshiftConnectorDTO.getArguments());
    }

    verifyPassword(featureStoreRedshiftConnector.getIamRole(), featureStoreRedshiftConnector.getSecret());
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

  private boolean shouldUpdate(String oldVal, String newVal) {
    return (oldVal == null && newVal != null) || (oldVal != null && !oldVal.equals(newVal));
  }

  private boolean shouldUpdate(Integer oldVal, Integer newVal) {
    return (oldVal == null && newVal != null) || (oldVal != null && !oldVal.equals(newVal));
  }


  private String getDatabasePassword(FeatureStoreRedshiftConnector featureStoreRedshiftConnector, Users user) {
    if (featureStoreRedshiftConnector.getSecret() == null) {
      return null;
    }

    Users owner = userFacade.find(featureStoreRedshiftConnector.getSecret().getId().getUid());
    try {
      return secretsController
          .getShared(user, owner, featureStoreRedshiftConnector.getSecret().getId().getName())
          .getPlaintext();
    } catch (UserException | ServiceException | ProjectException e) {
      //user can't access secret
    }
    return "";
  }
}
