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
import io.hops.hopsworks.common.dao.featurestore.storageconnectors.redshift.FeaturestoreRedshiftConnectorFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.security.secrets.SecretsFacade;
import io.hops.hopsworks.common.featurestore.FeaturestoreConstants;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.common.security.secrets.SecretsController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.redshift.FeatureStoreRedshiftConnector;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.secrets.Secret;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeaturestoreRedshiftConnectorController {
  private static final Logger LOGGER = Logger.getLogger(FeaturestoreRedshiftConnectorController.class.getName());
  
  @EJB
  private FeaturestoreRedshiftConnectorFacade featurestoreRedshiftConnectorFacade;
  @EJB
  private SecretsController secretsController;
  @EJB
  private SecretsFacade secretsFacade;
  @EJB
  private Settings settings;
  @EJB
  private UserFacade userFacade;
  
  /**
   *
   * @param featurestore
   * @return
   */
  public List<FeaturestoreStorageConnectorDTO> getConnectorsForFeaturestore(Users user, Featurestore featurestore) {
    List<FeatureStoreRedshiftConnector> featureStoreRedshiftConnectors = featurestoreRedshiftConnectorFacade
      .findByFeaturestore(featurestore);
    return featureStoreRedshiftConnectors.stream()
      .map(redshiftConnector -> getFeaturestoreRedshiftConnectorDTO(user, redshiftConnector))
      .collect(Collectors.toList());
  }
  
  /**
   *
   * @param featurestore
   * @param storageConnectorId
   * @return
   */
  public FeaturestoreRedshiftConnectorDTO getConnectorsWithIdAndFeaturestore(Users user, Featurestore featurestore,
    Integer storageConnectorId) throws FeaturestoreException {
    FeatureStoreRedshiftConnector featureStoreRedshiftConnector =
      featurestoreRedshiftConnectorFacade.findByIdAndFeaturestore(storageConnectorId, featurestore)
        .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.REDSHIFT_CONNECTOR_NOT_FOUND,
          Level.FINE, "Redshift Connector not found. ConnectorId: " + storageConnectorId));
    return getFeaturestoreRedshiftConnectorDTO(user, featureStoreRedshiftConnector);
  }
  
  /**
   *
   * @param user
   * @param featurestore
   * @param storageConnectorName
   * @return
   * @throws FeaturestoreException
   */
  public FeaturestoreStorageConnectorDTO getConnectorsWithNameAndFeaturestore(Users user, Featurestore featurestore,
    String storageConnectorName) throws FeaturestoreException {
    FeatureStoreRedshiftConnector featureStoreRedshiftConnector =
      featurestoreRedshiftConnectorFacade.findByNameAndFeaturestore(storageConnectorName, featurestore)
        .orElseThrow(() ->  new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.REDSHIFT_CONNECTOR_NOT_FOUND,
        Level.FINE, "Redshift Connector not found. Connector name: " + storageConnectorName));
    return getFeaturestoreRedshiftConnectorDTO(user, featureStoreRedshiftConnector);
  }
  
  private FeaturestoreRedshiftConnectorDTO getFeaturestoreRedshiftConnectorDTO(Users user,
    FeatureStoreRedshiftConnector featureStoreRedshiftConnector) {
    FeaturestoreRedshiftConnectorDTO featurestoreRedshiftConnectorDTO =
      new FeaturestoreRedshiftConnectorDTO(featureStoreRedshiftConnector);
    if (featureStoreRedshiftConnector.getSecret() != null) {
      Users owner = userFacade.find(featureStoreRedshiftConnector.getSecret().getId().getUid());
      try {
        featurestoreRedshiftConnectorDTO.setDatabasePassword(secretsController
          .getShared(user, owner.getUsername(), featureStoreRedshiftConnector.getSecret().getId().getName())
          .getPlaintext());
      } catch (UserException | ServiceException | ProjectException e) {
        //user can't access secret
      }
    }
    return featurestoreRedshiftConnectorDTO;
  }
  
  /**
   *
   * @param featurestore
   * @param featurestoreRedshiftConnectorDTO
   * @return
   */
  public FeaturestoreRedshiftConnectorDTO createFeaturestoreRedshiftConnector(Users user, Featurestore featurestore,
    FeaturestoreRedshiftConnectorDTO featurestoreRedshiftConnectorDTO) throws FeaturestoreException, UserException,
    ProjectException {
    verifyCreateDTO(featurestoreRedshiftConnectorDTO, featurestore);
    FeatureStoreRedshiftConnector featurestoreRedshiftConnector = new FeatureStoreRedshiftConnector();
    featurestoreRedshiftConnector.setName(featurestoreRedshiftConnectorDTO.getName());
    featurestoreRedshiftConnector.setDescription(featurestoreRedshiftConnectorDTO.getDescription());
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
    featurestoreRedshiftConnector.setFeatureStore(featurestore);
    featurestoreRedshiftConnectorFacade.save(featurestoreRedshiftConnector);
    return featurestoreRedshiftConnectorDTO;
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
  
  private void verifyCreateDTO(FeaturestoreRedshiftConnectorDTO featurestoreRedshiftConnectorDTO,
    Featurestore featurestore) throws FeaturestoreException {
    verifyName(featurestoreRedshiftConnectorDTO, featurestore);
    featurestoreRedshiftConnectorDTO.verifyDescription();
    if (Strings.isNullOrEmpty(featurestoreRedshiftConnectorDTO.getClusterIdentifier())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
        "Redshift connector cluster identifier cannot be empty");
    }
    if(!Strings.isNullOrEmpty(featurestoreRedshiftConnectorDTO.getArguments())
      && featurestoreRedshiftConnectorDTO.getArguments().length() >
      FeaturestoreConstants.JDBC_STORAGE_CONNECTOR_ARGUMENTS_MAX_LENGTH) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
        "Redshift connection arguments should not exceed: " +
          FeaturestoreConstants.JDBC_STORAGE_CONNECTOR_ARGUMENTS_MAX_LENGTH + " characters");
    }
    verifyPassword(featurestoreRedshiftConnectorDTO);
  }
  
  private void verifyName(FeaturestoreRedshiftConnectorDTO featurestoreRedshiftConnectorDTO, Featurestore featurestore)
    throws FeaturestoreException {
    featurestoreRedshiftConnectorDTO.verifyName();
    Optional<FeatureStoreRedshiftConnector> featureStoreRedshiftConnector =
      featurestoreRedshiftConnectorFacade
        .findByNameAndFeaturestore(featurestoreRedshiftConnectorDTO.getName(), featurestore);
    if (featureStoreRedshiftConnector.isPresent()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_NAME, Level.FINE,
        "Redshift connector with the same name already exists. Name=" + featurestoreRedshiftConnectorDTO.getName());
    }
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
  
  /**
   *
   * @param featurestore
   * @param featurestoreRedshiftConnectorDTO
   * @param storageConnectorName
   * @return
   */
  public FeaturestoreRedshiftConnectorDTO updateFeaturestoreRedshiftConnector(Users user, Featurestore featurestore,
    FeaturestoreRedshiftConnectorDTO featurestoreRedshiftConnectorDTO, String storageConnectorName)
    throws FeaturestoreException, UserException, ProjectException {
    Optional<FeatureStoreRedshiftConnector> featureStoreRedshiftConnectorOptional =
      featurestoreRedshiftConnectorFacade.findByNameAndFeaturestore(storageConnectorName, featurestore);
    if (!featureStoreRedshiftConnectorOptional.isPresent()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.REDSHIFT_CONNECTOR_NOT_FOUND,
        Level.FINE, "Connector name: " + storageConnectorName);
    }
    boolean updated = false;
    FeatureStoreRedshiftConnector featureStoreRedshiftConnector = featureStoreRedshiftConnectorOptional.get();
    FeaturestoreRedshiftConnectorDTO featurestoreRedshiftConnectorDTOExisting =
      getFeaturestoreRedshiftConnectorDTO(user, featureStoreRedshiftConnector);
    if (shouldUpdate(featurestoreRedshiftConnectorDTO.getName(), featurestoreRedshiftConnectorDTOExisting.getName())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG,
        Level.FINE, "Can not update connector name.");
    }
    //can set description to empty
    if (shouldUpdate(featurestoreRedshiftConnectorDTO.getDescription(),
      featurestoreRedshiftConnectorDTOExisting.getDescription())) {
      featurestoreRedshiftConnectorDTO.verifyDescription();
      featureStoreRedshiftConnector.setDescription(featurestoreRedshiftConnectorDTO.getDescription());
      updated = true;
    }
    if (!Strings.isNullOrEmpty(featurestoreRedshiftConnectorDTO.getClusterIdentifier()) &&
      !featurestoreRedshiftConnectorDTO.getClusterIdentifier()
        .equals(featurestoreRedshiftConnectorDTOExisting.getClusterIdentifier())) {
      featureStoreRedshiftConnector.setClusterIdentifier(featurestoreRedshiftConnectorDTO.getClusterIdentifier());
      updated = true;
    }
    if (shouldUpdate(featurestoreRedshiftConnectorDTOExisting.getDatabaseDriver(),
      featurestoreRedshiftConnectorDTO.getDatabaseDriver())) {
      featureStoreRedshiftConnector.setDatabaseDriver(featurestoreRedshiftConnectorDTO.getDatabaseDriver());
      updated = true;
    }
    if (shouldUpdate(featurestoreRedshiftConnectorDTOExisting.getDatabaseEndpoint(),
      featurestoreRedshiftConnectorDTO.getDatabaseEndpoint())) {
      featureStoreRedshiftConnector.setDatabaseEndpoint(featurestoreRedshiftConnectorDTO.getDatabaseEndpoint());
      updated = true;
    }
    if (shouldUpdate(featurestoreRedshiftConnectorDTOExisting.getDatabaseName(),
      featurestoreRedshiftConnectorDTO.getDatabaseName())) {
      featureStoreRedshiftConnector.setDatabaseName(featurestoreRedshiftConnectorDTO.getDatabaseName());
      updated = true;
    }
    if (shouldUpdate(featurestoreRedshiftConnectorDTO.getDatabaseUserName(),
      featurestoreRedshiftConnectorDTOExisting.getDatabaseUserName())) {
      featureStoreRedshiftConnector.setDatabaseUserName(featurestoreRedshiftConnectorDTO.getDatabaseUserName());
      updated = true;
    }
    if (shouldUpdate(featurestoreRedshiftConnectorDTOExisting.getDatabaseGroup(),
      featurestoreRedshiftConnectorDTO.getDatabaseGroup())) {
      featureStoreRedshiftConnector.setDatabaseGroup(featurestoreRedshiftConnectorDTO.getDatabaseGroup());
      updated = true;
    }
    if (featurestoreRedshiftConnectorDTO.getAutoCreate() != featurestoreRedshiftConnectorDTOExisting.getAutoCreate()) {
      featureStoreRedshiftConnector.setAutoCreate(featurestoreRedshiftConnectorDTO.getAutoCreate());
      updated = true;
    }
    if (shouldUpdate(featurestoreRedshiftConnectorDTO.getDatabasePort(),
      featurestoreRedshiftConnectorDTOExisting.getDatabasePort())) {
      featureStoreRedshiftConnector.setDatabasePort(featurestoreRedshiftConnectorDTO.getDatabasePort());
      updated = true;
    }
    if (shouldUpdate(featurestoreRedshiftConnectorDTOExisting.getTableName(),
      featurestoreRedshiftConnectorDTO.getTableName())) {
      featureStoreRedshiftConnector.setTableName(featurestoreRedshiftConnectorDTO.getTableName());
      updated = true;
    }
    if (shouldUpdate(featurestoreRedshiftConnectorDTOExisting.getIamRole(),
      featurestoreRedshiftConnectorDTO.getIamRole())) {
      featureStoreRedshiftConnector.setIamRole(featurestoreRedshiftConnectorDTO.getIamRole());
      updated = true;
    }
    Secret secret = null;
    if (shouldUpdate(featurestoreRedshiftConnectorDTOExisting.getDatabasePassword(),
      featurestoreRedshiftConnectorDTO.getDatabasePassword())) {
      secret = updatePassword(user, featurestoreRedshiftConnectorDTO, featurestore, featureStoreRedshiftConnector);
      updated = true;
    }
    
    if (shouldUpdate(featurestoreRedshiftConnectorDTOExisting.getArguments(),
      featurestoreRedshiftConnectorDTO.getArguments())) {
      featureStoreRedshiftConnector.setArguments(featurestoreRedshiftConnectorDTO.getArguments());
      updated = true;
    }
    if (updated) {
      //verify if password or iam role is set
      verifyPassword(featureStoreRedshiftConnector.getIamRole(), featureStoreRedshiftConnector.getSecret());
      featureStoreRedshiftConnector = featurestoreRedshiftConnectorFacade.update(featureStoreRedshiftConnector);
      if (featureStoreRedshiftConnector.getSecret() == null && secret != null) {
        secretsFacade.deleteSecret(secret.getId());
      }
      featurestoreRedshiftConnectorDTOExisting = getFeaturestoreRedshiftConnectorDTO(user,
        featureStoreRedshiftConnector);
    }
    return featurestoreRedshiftConnectorDTOExisting;
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
  
  /**
   *
   * @param storageConnectorId
   * @return
   */
  public void removeFeaturestoreRedshiftConnector(Users user, Integer storageConnectorId) throws ProjectException {
    FeatureStoreRedshiftConnector featureStoreRedshiftConnector =
      featurestoreRedshiftConnectorFacade.find(storageConnectorId);
    if (featureStoreRedshiftConnector != null) {
      Secret secret = featureStoreRedshiftConnector.getSecret();
      secretsController.checkCanAccessSecret(secret, user);
      featurestoreRedshiftConnectorFacade.remove(featureStoreRedshiftConnector);
      if (secret != null) {
        secretsFacade.deleteSecret(secret.getId());
      }
    }
  }
  
  /**
   *
   * @param storageConnectorName
   * @return
   */
  public void removeFeaturestoreRedshiftConnector(Users user, String storageConnectorName, Featurestore featurestore)
    throws ProjectException {
    Optional<FeatureStoreRedshiftConnector> featureStoreRedshiftConnector =
      featurestoreRedshiftConnectorFacade.findByNameAndFeaturestore(storageConnectorName, featurestore);
    if (featureStoreRedshiftConnector.isPresent()) {
      Secret secret = featureStoreRedshiftConnector.get().getSecret();
      secretsController.checkCanAccessSecret(secret, user);
      featurestoreRedshiftConnectorFacade.remove(featureStoreRedshiftConnector.get());
      if (secret != null) {
        secretsFacade.deleteSecret(secret.getId());
      }
    }
  }
}
