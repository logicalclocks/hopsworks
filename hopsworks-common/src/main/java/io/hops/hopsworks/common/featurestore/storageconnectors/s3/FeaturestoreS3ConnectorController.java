/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.storageconnectors.s3;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.security.secrets.SecretPlaintext;
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
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.s3.FeaturestoreS3Connector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.s3.FeaturestoreS3ConnectorAccessAndSecretKey;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.s3.FeaturestoreS3ConnectorEncryptionAlgorithm;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.secrets.Secret;
import io.hops.hopsworks.restutils.RESTCodes;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class controlling the interaction with the feature_store_s3_connector table and required business logic
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeaturestoreS3ConnectorController {
  private static final Logger LOGGER = Logger.getLogger(FeaturestoreS3ConnectorController.class.getName());
  @EJB
  private FeaturestoreS3ConnectorFacade featurestoreS3ConnectorFacade;
  @EJB
  private Settings settings;
  @EJB
  private SecretsController secretsController;
  @EJB
  private SecretsFacade secretsFacade;
  @EJB
  private UserFacade userFacade;
  
  /**
   * Stores an S3 connection as a backend for a feature store
   *
   * @param user the user making the request
   * @param featurestore the featurestore
   * @param featurestoreS3ConnectorDTO the data to use when creating the storage connector
   * @return DTO of the created entity
   * @throws FeaturestoreException
   */
  public FeaturestoreS3ConnectorDTO createFeaturestoreS3Connector(Users user, Featurestore featurestore,
    FeaturestoreS3ConnectorDTO featurestoreS3ConnectorDTO) throws FeaturestoreException, UserException,
    ProjectException {
    FeaturestoreS3ConnectorEncryptionAlgorithm encryptionAlgorithm =
      getEncryptionAlgorithm(featurestoreS3ConnectorDTO.getServerEncryptionAlgorithm());
    verifyUserInput(featurestore, featurestoreS3ConnectorDTO);
    FeaturestoreS3Connector featurestoreS3Connector = new FeaturestoreS3Connector();
    featurestoreS3Connector.setDescription(featurestoreS3ConnectorDTO.getDescription());
    featurestoreS3Connector.setName(featurestoreS3ConnectorDTO.getName());
    featurestoreS3Connector.setBucket(featurestoreS3ConnectorDTO.getBucket());
    featurestoreS3Connector.setFeaturestore(featurestore);
    featurestoreS3Connector.setServerEncryptionAlgorithm(encryptionAlgorithm);
    featurestoreS3Connector.setServerEncryptionKey(featurestoreS3ConnectorDTO.getServerEncryptionKey());
    featurestoreS3Connector.setIamRole(featurestoreS3ConnectorDTO.getIamRole());
    setSecret(user, featurestoreS3ConnectorDTO, featurestoreS3Connector, featurestore);
    featurestoreS3ConnectorFacade.persist(featurestoreS3Connector);
    FeaturestoreS3ConnectorDTO createdFeaturestoreS3ConnectorDTO =
      new FeaturestoreS3ConnectorDTO(featurestoreS3Connector);
    createdFeaturestoreS3ConnectorDTO.setAccessKey(featurestoreS3ConnectorDTO.getAccessKey());
    createdFeaturestoreS3ConnectorDTO.setSecretKey(featurestoreS3ConnectorDTO.getSecretKey());
    return createdFeaturestoreS3ConnectorDTO;
  }
  
  private void setSecret(Users user, FeaturestoreS3ConnectorDTO featurestoreS3ConnectorDTO,
    FeaturestoreS3Connector featurestoreS3Connector, Featurestore featurestore) throws UserException, ProjectException {
    if (keysNotNullOrEmpty(featurestoreS3ConnectorDTO)) {
      String jsonSecretString = createS3AccessAndSecretKeysSecret(featurestoreS3ConnectorDTO.getAccessKey(),
        featurestoreS3ConnectorDTO.getSecretKey());
      String secretName = createSecretName(featurestore.getId(), featurestoreS3ConnectorDTO.getName());
      Integer projectId = featurestore.getProject().getId();
      Secret secret = secretsController.createSecretForProject(user, secretName, jsonSecretString, projectId);
      featurestoreS3Connector.setSecret(secret);
    }
  }
  
  /**
   * Creates the secret name as a concatenation of featurestoreId and connector name
   * @param featurestoreId
   * @param connectorName
   * @return the secret name: connectorName_featurestoreId
   */
  private String createSecretName(Integer featurestoreId, String connectorName) {
    return "s3_" + connectorName.replaceAll(" ", "_").toLowerCase() + "_" + featurestoreId;
  }
  
  /**
   * Updates a S3 connection as a backend for a feature store
   *
   * @param featurestore the featurestore
   * @param featurestoreS3ConnectorDTO the data to use when creating the storage connector
   * @param storageConnectorName the name of the connector
   * @return DTO of the updated entity
   * @throws FeaturestoreException
   */
  public FeaturestoreS3ConnectorDTO updateFeaturestoreS3Connector(Users user, Featurestore featurestore,
    FeaturestoreS3ConnectorDTO featurestoreS3ConnectorDTO, String storageConnectorName) throws FeaturestoreException,
    UserException, ProjectException {
    FeaturestoreS3Connector featurestoreS3Connector = verifyS3ConnectorName(storageConnectorName, featurestore);
    FeaturestoreS3ConnectorDTO featurestoreS3ConnectorDTOExisting =
      getS3ConnectorWithIdAndFeaturestore(user, featurestoreS3Connector);
    boolean updated = false;
    if (shouldUpdate(featurestoreS3ConnectorDTOExisting.getName(), featurestoreS3ConnectorDTO.getName())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG,
        Level.FINE, "Can not update connector name.");
    }
    //can set description to empty
    if (shouldUpdate(featurestoreS3ConnectorDTOExisting.getDescription(),
                     featurestoreS3ConnectorDTO.getDescription())) {
      featurestoreS3ConnectorDTO.verifyDescription();
      featurestoreS3Connector.setDescription(featurestoreS3ConnectorDTO.getDescription());
      updated = true;
    }
    if (shouldUpdate(featurestoreS3ConnectorDTOExisting.getBucket(), featurestoreS3ConnectorDTO.getBucket())) {
      verifyS3ConnectorBucket(featurestoreS3ConnectorDTO.getBucket());
      featurestoreS3Connector.setBucket(featurestoreS3ConnectorDTO.getBucket());
      updated = true;
    }
    if (shouldUpdate(featurestoreS3ConnectorDTOExisting.getIamRole(), featurestoreS3ConnectorDTO.getIamRole())) {
      featurestoreS3Connector.setIamRole(featurestoreS3ConnectorDTO.getIamRole());
      updated = true;
    }
  
    Secret secret = null;
    if (shouldUpdate(featurestoreS3ConnectorDTOExisting.getAccessKey(), featurestoreS3ConnectorDTO.getAccessKey()) ||
      shouldUpdate(featurestoreS3ConnectorDTOExisting.getSecretKey(), featurestoreS3ConnectorDTO.getSecretKey())) {
      secret = updateSecret(user, featurestoreS3ConnectorDTO, featurestore, featurestoreS3Connector);
      updated = true;
    }
  
    if (shouldUpdate(featurestoreS3ConnectorDTOExisting.getServerEncryptionAlgorithm(),
                     featurestoreS3ConnectorDTO.getServerEncryptionAlgorithm())) {
      if (featurestoreS3ConnectorDTO.getServerEncryptionAlgorithm() != null) {
        FeaturestoreS3ConnectorEncryptionAlgorithm serverEncryptionAlgorithm =
          getEncryptionAlgorithm(featurestoreS3ConnectorDTO.getServerEncryptionAlgorithm());
        featurestoreS3Connector.setServerEncryptionAlgorithm(serverEncryptionAlgorithm);
        if (serverEncryptionAlgorithm != null && serverEncryptionAlgorithm.isRequiresKey()) {
          verifyS3ConnectorServerEncryptionKey(featurestoreS3ConnectorDTO.getServerEncryptionKey());
          featurestoreS3Connector.setServerEncryptionKey(featurestoreS3ConnectorDTO.getServerEncryptionKey());
        } else {
          featurestoreS3Connector.setServerEncryptionKey(null);
        }
      } else if (!Strings.isNullOrEmpty(featurestoreS3ConnectorDTO.getServerEncryptionKey())) {
        throw new FeaturestoreException(
          RESTCodes.FeaturestoreErrorCode.ILLEGAL_S3_CONNECTOR_SERVER_ENCRYPTION_ALGORITHM,
          Level.FINE, "Illegal server encryption algorithm, encryption algorithm not provided");
      } else {
        featurestoreS3Connector.setServerEncryptionAlgorithm(null);
        featurestoreS3Connector.setServerEncryptionKey(null);
      }
      updated = true;
    }

    if (updated) {
      //verify if key or iam role is set
      verifyKeyAndIAMRole(featurestoreS3Connector.getIamRole(), featurestoreS3Connector.getSecret());
      featurestoreS3Connector = featurestoreS3ConnectorFacade.updateS3Connector(featurestoreS3Connector);
      if (featurestoreS3Connector.getSecret() == null && secret != null) {
        secretsFacade.deleteSecret(secret.getId());
      }
      featurestoreS3ConnectorDTOExisting = getS3ConnectorWithIdAndFeaturestore(user, featurestoreS3Connector);
    }
    return featurestoreS3ConnectorDTOExisting;
  }
  
  private void verifyKeyAndIAMRole(String iamRole, Secret secret) throws FeaturestoreException {
    boolean needPassword = !settings.isIAMRoleConfigured() && Strings.isNullOrEmpty(iamRole);
    if (needPassword && secret == null) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE, "S3 Access Keys are not set.");
    } else if (!needPassword && secret != null) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE, "S3 Access Keys are not allowed");
    }
  }
  
  private Secret updateSecret(Users user, FeaturestoreS3ConnectorDTO featurestoreS3ConnectorDTO,
    Featurestore featurestore, FeaturestoreS3Connector featurestoreS3Connector) throws UserException,
    FeaturestoreException, ProjectException {
    Secret secret = featurestoreS3Connector.getSecret();
    if (secret != null) {
      secretsController.checkCanAccessSecret(secret, user);
    }
    if (secret == null && keysNotNullOrEmpty(featurestoreS3ConnectorDTO)) {
      verifyS3ConnectorAccessKey(featurestoreS3ConnectorDTO.getAccessKey());
      verifyS3ConnectorSecretKey(featurestoreS3ConnectorDTO.getSecretKey());
      setSecret(user, featurestoreS3ConnectorDTO, featurestoreS3Connector, featurestore);
    } else if (keysNotNullOrEmpty(featurestoreS3ConnectorDTO)) {
      try {
        verifyS3ConnectorAccessKey(featurestoreS3ConnectorDTO.getAccessKey());
        verifyS3ConnectorSecretKey(featurestoreS3ConnectorDTO.getSecretKey());
        String jsonSecretString = createS3AccessAndSecretKeysSecret(featurestoreS3ConnectorDTO.getAccessKey(),
          featurestoreS3ConnectorDTO.getSecretKey());
        secret.setSecret(secretsController.encryptSecret(jsonSecretString));
      } catch (IOException | GeneralSecurityException e) {
        throw new UserException(RESTCodes.UserErrorCode.SECRET_ENCRYPTION_ERROR, Level.SEVERE,
          "Error encrypting secret", "Could not encrypt Secret " + secret.getId().getName(), e);
      }
    } else {
      featurestoreS3Connector.setSecret(null);
      //Secret can't be removed here b/c of ON DELETE RESTRICT
    }
    return secret;
    
  }
  
  private boolean shouldUpdate(String oldVal, String newVal) {
    return (oldVal == null && newVal != null) || (oldVal != null && !oldVal.equals(newVal));
  }
  
  private boolean keysNotNullOrEmpty(FeaturestoreS3ConnectorDTO featurestoreS3ConnectorDTO) {
    return !Strings.isNullOrEmpty(featurestoreS3ConnectorDTO.getSecretKey()) &&
      !Strings.isNullOrEmpty(featurestoreS3ConnectorDTO.getAccessKey());
  }

  /**
   * Removes an S3 connector from the feature store
   *
   * @param featurestoreS3Id id of the connection to remove
   */
  public void removeFeaturestoreS3Connector(Users user, Integer featurestoreS3Id) throws ProjectException {
    FeaturestoreS3Connector featurestoreS3Connector = featurestoreS3ConnectorFacade.find(featurestoreS3Id);
    if (featurestoreS3Connector != null) {
      Secret secret = featurestoreS3Connector.getSecret();
      secretsController.checkCanAccessSecret(secret, user);
      featurestoreS3ConnectorFacade.remove(featurestoreS3Connector);
      if (secret != null) {
        secretsFacade.deleteSecret(secret.getId());
      }
    }
  }
  
  /**
   * Removes an S3 connector from the feature store
   *
   * @param featurestore
   * @param storageConnectorName
   */
  public void removeFeaturestoreS3Connector(Users user, Featurestore featurestore, String storageConnectorName)
    throws ProjectException {
    Optional<FeaturestoreS3Connector> featurestoreS3Connector =
      featurestoreS3ConnectorFacade.findByNameAndFeaturestore(storageConnectorName, featurestore);
    if (featurestoreS3Connector.isPresent()) {
      Secret secret = featurestoreS3Connector.get().getSecret();
      secretsController.checkCanAccessSecret(secret, user);
      featurestoreS3ConnectorFacade.remove(featurestoreS3Connector.get());
      if (secret != null) {
        secretsFacade.deleteSecret(secret.getId());
      }
    }
  }
  
  private FeaturestoreS3Connector verifyS3ConnectorName(String name, Featurestore featurestore)
    throws FeaturestoreException {
    return featurestoreS3ConnectorFacade.findByNameAndFeaturestore(name, featurestore)
      .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.S3_CONNECTOR_NOT_FOUND,
        Level.FINE, "S3 Connector not found, S3 connector with name: " + name));
  }

  /**
   * Validates the featurestore to query
   *
   * @param featurestore the featurestore to validate
   */
  private void verifyFeaturestore(Featurestore featurestore){
    if (featurestore == null) {
      throw new IllegalArgumentException("Featurestore was not found");
    }
  }

  /**
   * Validates user input name string
   * @param featurestoreS3ConnectorDTO the input to validate
   * @param featurestore the featurestore of the connector
   * @throws FeaturestoreException
   */
  private void verifyS3ConnectorName(FeaturestoreS3ConnectorDTO featurestoreS3ConnectorDTO, Featurestore featurestore)
    throws FeaturestoreException {
    featurestoreS3ConnectorDTO.verifyName();
    if (featurestoreS3ConnectorFacade.findByNameAndFeaturestore(featurestoreS3ConnectorDTO.getName(), featurestore)
      .isPresent()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_NAME, Level.FINE,
        "Illegal storage connector name, the storage connector name should be unique, there already exists a S3 " +
          "connector with the same name ");
    }
  }

  /**
   * Validates user input bucket
   *
   * @param bucket the input to validate
   * @throws FeaturestoreException
   */
  private void verifyS3ConnectorBucket(String bucket) throws FeaturestoreException {
    if (Strings.isNullOrEmpty(bucket) ||
      bucket.length() > FeaturestoreConstants.S3_STORAGE_CONNECTOR_BUCKET_MAX_LENGTH) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_S3_CONNECTOR_BUCKET, Level.FINE,
          "Illegal S3 connector bucket, the S3 bucket string should not be empty and not exceed: " +
            FeaturestoreConstants.S3_STORAGE_CONNECTOR_BUCKET_MAX_LENGTH + " characters");
    }
  }

  /**
   * Validates user input access key string
   *
   * @param accessKey the input to validate
   * @throws FeaturestoreException
   */
  private void verifyS3ConnectorAccessKey(String accessKey) throws FeaturestoreException {
    if (Strings.isNullOrEmpty(accessKey)) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_S3_CONNECTOR_ACCESS_KEY,
        Level.FINE, "The S3 access key cannot be empty and must be less than "
        + FeaturestoreConstants.S3_STORAGE_CONNECTOR_ACCESSKEY_MAX_LENGTH);
    } else if(accessKey.length() > FeaturestoreConstants.S3_STORAGE_CONNECTOR_ACCESSKEY_MAX_LENGTH) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_S3_CONNECTOR_ACCESS_KEY, Level.FINE,
          "Illegal S3 connector access key, the S3 access key should not exceed: " +
            FeaturestoreConstants.S3_STORAGE_CONNECTOR_ACCESSKEY_MAX_LENGTH + " characters");
    }
  }

  /**
   * Validates user input secret key string
   *
   * @param secretKey the input to validate
   * @throws FeaturestoreException
   */
  private void verifyS3ConnectorSecretKey(String secretKey) throws FeaturestoreException {
    if (Strings.isNullOrEmpty(secretKey)) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_S3_CONNECTOR_SECRET_KEY,
        Level.FINE, "The S3 secret key cannot be empty and must be less than "
        + FeaturestoreConstants.S3_STORAGE_CONNECTOR_SECRETKEY_MAX_LENGTH);
    } else if (secretKey.length() > FeaturestoreConstants.S3_STORAGE_CONNECTOR_SECRETKEY_MAX_LENGTH) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_S3_CONNECTOR_SECRET_KEY, Level.FINE,
          "Illegal S3 connector secret key, the S3 secret key should not exceed: " +
            FeaturestoreConstants.S3_STORAGE_CONNECTOR_SECRETKEY_MAX_LENGTH + " characters");
    }
  }

  /**
   *  Validates user input for server encryption key string
   *  Called when the user provides the server encryption key or server encryption algorithm
   *
   * @param serverEncryptionKey the input to validate
   * @throws FeaturestoreException
   */
  private void verifyS3ConnectorServerEncryptionKey(String serverEncryptionKey) throws FeaturestoreException{
    if (Strings.isNullOrEmpty(serverEncryptionKey)) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_S3_CONNECTOR_SERVER_ENCRYPTION_KEY,
        Level.FINE, "S3 server encryption key cannot be empty"
      );
    } else if (serverEncryptionKey.length() > FeaturestoreConstants.S3_STORAGE_SERVER_ENCRYPTION_KEY_MAX_LENGTH) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_S3_CONNECTOR_SERVER_ENCRYPTION_KEY,
        Level.FINE, "Illegal server encryption key provided, the S3 server encryption key should not exceed: " +
        FeaturestoreConstants.S3_STORAGE_SERVER_ENCRYPTION_KEY_MAX_LENGTH + " characters");
    }
  }
  
  /**
   * Validates user input for creating a new S3 connector in a featurestore
   *
   * @param featurestore the featurestore
   * @param featurestoreS3ConnectorDTO the data to use when creating the storage connector
   * @throws FeaturestoreException
   */
  private void verifyUserInput(Featurestore featurestore, FeaturestoreS3ConnectorDTO featurestoreS3ConnectorDTO)
    throws FeaturestoreException {
    if (featurestoreS3ConnectorDTO == null) {
      throw new IllegalArgumentException("Null input data");
    }
    verifyFeaturestore(featurestore);
    verifyS3ConnectorName(featurestoreS3ConnectorDTO,featurestore);
    featurestoreS3ConnectorDTO.verifyDescription();
    verifyS3ConnectorBucket(featurestoreS3ConnectorDTO.getBucket());
    
    if (settings.isIAMRoleConfigured() || !Strings.isNullOrEmpty(featurestoreS3ConnectorDTO.getIamRole())) {
      verifySecretAndAccessKeysForIamRole(featurestoreS3ConnectorDTO);
    } else {
      verifyS3ConnectorAccessKey(featurestoreS3ConnectorDTO.getAccessKey());
      verifyS3ConnectorSecretKey(featurestoreS3ConnectorDTO.getSecretKey());
    }
    
    FeaturestoreS3ConnectorEncryptionAlgorithm encryptionAlgorithm =
      getEncryptionAlgorithm(featurestoreS3ConnectorDTO.getServerEncryptionAlgorithm());
    if (encryptionAlgorithm != null){
      if (encryptionAlgorithm.isRequiresKey()) {
        verifyS3ConnectorServerEncryptionKey(featurestoreS3ConnectorDTO.getServerEncryptionKey());
      } else {
        featurestoreS3ConnectorDTO.setServerEncryptionKey(null);
      }
    } else if (!Strings.isNullOrEmpty(featurestoreS3ConnectorDTO.getServerEncryptionKey())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_S3_CONNECTOR_SERVER_ENCRYPTION_ALGORITHM,
        Level.FINE, "Illegal server encryption algorithm, encryption algorithm not provided");
    }
  }
  
  /**
   * If the user has IAM Role configured to true, they cannot provide access and secret keys
   * @param featurestoreS3ConnectorDTO
   * @throws FeaturestoreException
   */
  private void verifySecretAndAccessKeysForIamRole(FeaturestoreS3ConnectorDTO featurestoreS3ConnectorDTO)
    throws FeaturestoreException{
    if ((settings.isIAMRoleConfigured() || !Strings.isNullOrEmpty(featurestoreS3ConnectorDTO.getIamRole())) &&
      (!Strings.isNullOrEmpty(featurestoreS3ConnectorDTO.getAccessKey()) ||
      !Strings.isNullOrEmpty(featurestoreS3ConnectorDTO.getSecretKey()))) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.S3_KEYS_FORBIDDEN, Level.FINE,
        "S3 Access Keys are not allowed");
    }
    if (!settings.isIAMRoleConfigured() && Strings.isNullOrEmpty(featurestoreS3ConnectorDTO.getIamRole())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
        "S3 IAM role not set.");
    }
  }
  
  /**
   * @param s
   * @return
   * @throws FeaturestoreException
   */
  private FeaturestoreS3ConnectorEncryptionAlgorithm getEncryptionAlgorithm(String s) throws FeaturestoreException {
    if (Strings.isNullOrEmpty(s)) {
      return null;
    }
    FeaturestoreS3ConnectorEncryptionAlgorithm serverEncryptionAlgrithm = null;
    try {
      serverEncryptionAlgrithm = FeaturestoreS3ConnectorEncryptionAlgorithm.fromValue(s);
    } catch (IllegalArgumentException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_S3_CONNECTOR_SERVER_ENCRYPTION_ALGORITHM,
        Level.FINE, "Illegal server encryption algorithm provided, " + e.getMessage());
    }
    return serverEncryptionAlgrithm;
  }
  
  /**
   * Creates the user secret for the bucket access and secret key
   *
   * @param accessKey
   * @param secretKey
   * @return the FeaturestoreS3ConnectorAccessAndSecretKey object as JSON
   * @throws UserException
   */
  public String createS3AccessAndSecretKeysSecret(String accessKey, String secretKey) {
    return new JSONObject(new FeaturestoreS3ConnectorAccessAndSecretKey(accessKey, secretKey)).toString();
  }
  
  /**
   * Gets all S3 connectors for a particular featurestore and project
   *
   * @param featurestore featurestore to query for s3 connectors
   * @return list of XML/JSON DTOs of the s3 connectors
   */
  public List<FeaturestoreStorageConnectorDTO> getS3ConnectorsForFeaturestore(Users user, Featurestore featurestore) {
    List<FeaturestoreStorageConnectorDTO> s3ConnectorDTOs = new ArrayList<>();
    List<FeaturestoreS3Connector> s3Connectors = featurestoreS3ConnectorFacade.findByFeaturestore(featurestore);
    for(FeaturestoreS3Connector s3Connector : s3Connectors) {
      s3ConnectorDTOs.add(getS3ConnectorWithIdAndFeaturestore(user, s3Connector));
    }
    return s3ConnectorDTOs;
  }
  
  /**
   *
   * @param user
   * @param featurestore
   * @param name
   * @return
   * @throws FeaturestoreException
   */
  public FeaturestoreS3ConnectorDTO getS3ConnectorWithNameAndFeaturestore(Users user, Featurestore featurestore,
    String name) throws FeaturestoreException {
    FeaturestoreS3Connector featurestoreS3Connector = verifyS3ConnectorName(name, featurestore);
    return getS3ConnectorWithIdAndFeaturestore(user, featurestoreS3Connector);
  }
  
  private FeaturestoreS3ConnectorDTO getS3ConnectorWithIdAndFeaturestore(Users user,
    FeaturestoreS3Connector featurestoreS3Connector) {
    FeaturestoreS3ConnectorDTO featurestoreS3ConnectorDTO = new FeaturestoreS3ConnectorDTO(featurestoreS3Connector);
    if (featurestoreS3Connector.getSecret() != null) {
      FeaturestoreS3ConnectorAccessAndSecretKey accessAndSecretKey =
        getS3AccessAndSecretKeySecretForConnector(user, featurestoreS3Connector);
      setAccessAndSecretKeysInDTO(featurestoreS3ConnectorDTO, accessAndSecretKey);
    }
    return featurestoreS3ConnectorDTO;
  }
  
  /**
   * Get the access and secret key pair from the secret store
   *
   * @param featurestoreS3Connector the s3 connector
   * @return FeaturestoreS3ConnectorAccessAndSecretKey object
   * @throws FeaturestoreException
   */
  private FeaturestoreS3ConnectorAccessAndSecretKey getS3AccessAndSecretKeySecretForConnector(Users user,
    FeaturestoreS3Connector featurestoreS3Connector) {
    Secret secret = featurestoreS3Connector.getSecret();
    FeaturestoreS3ConnectorAccessAndSecretKey featurestoreS3ConnectorAccessAndSecretKey =
      new FeaturestoreS3ConnectorAccessAndSecretKey();
    if (secret != null) {
      try {
        Users owner = userFacade.find(secret.getId().getUid());
        SecretPlaintext plainText = secretsController.getShared(user, owner.getUsername(), secret.getId().getName());
        featurestoreS3ConnectorAccessAndSecretKey = new ObjectMapper().readValue(plainText.getPlaintext(),
          FeaturestoreS3ConnectorAccessAndSecretKey.class);
      } catch (UserException | IOException | ServiceException | ProjectException e) {
        //Just return empty
      }
    }
    return featurestoreS3ConnectorAccessAndSecretKey;
  }
  
  /**
   * Set the access and secret key in the DTO
   *
   * @param featurestoreS3ConnectorDTO
   * @param featurestoreS3ConnectorAccessAndSecretKey
   */
  private void setAccessAndSecretKeysInDTO(FeaturestoreS3ConnectorDTO featurestoreS3ConnectorDTO,
    FeaturestoreS3ConnectorAccessAndSecretKey featurestoreS3ConnectorAccessAndSecretKey){
    featurestoreS3ConnectorDTO.setAccessKey(featurestoreS3ConnectorAccessAndSecretKey.getAccessKey());
    featurestoreS3ConnectorDTO.setSecretKey(featurestoreS3ConnectorAccessAndSecretKey.getSecretKey());
  }
}
