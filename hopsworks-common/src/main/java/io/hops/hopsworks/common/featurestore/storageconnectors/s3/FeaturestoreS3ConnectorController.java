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
import io.hops.hopsworks.persistence.entity.user.security.secrets.SecretId;
import io.hops.hopsworks.persistence.entity.user.security.secrets.VisibilityType;
import io.hops.hopsworks.restutils.RESTCodes;
import org.json.JSONObject;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Class controlling the interaction with the feature_store_s3_connector table and required business logic
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeaturestoreS3ConnectorController {
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
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public FeaturestoreS3ConnectorDTO createFeaturestoreS3Connector(
      Users user, Featurestore featurestore, FeaturestoreS3ConnectorDTO featurestoreS3ConnectorDTO)
    throws FeaturestoreException, UserException {
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

    if (!settings.isIAMRoleConfigured()) {
      String jsonSecretString = createS3AccessAndSecretKeysSecret(featurestoreS3ConnectorDTO.getAccessKey(),
        featurestoreS3ConnectorDTO.getSecretKey());
      String secretName = createSecretName(featurestore.getId(),
        featurestoreS3ConnectorDTO.getName());
      Integer projectId = featurestore.getProject().getId();
      secretsController.add(user, secretName, jsonSecretString, VisibilityType.PROJECT, projectId);
    }
    featurestoreS3ConnectorFacade.persist(featurestoreS3Connector);
    FeaturestoreS3ConnectorDTO createdFeaturestoreS3ConnectorDTO =
      new FeaturestoreS3ConnectorDTO(featurestoreS3Connector);
    createdFeaturestoreS3ConnectorDTO.setAccessKey(featurestoreS3ConnectorDTO.getAccessKey());
    createdFeaturestoreS3ConnectorDTO.setSecretKey(featurestoreS3ConnectorDTO.getSecretKey());
    return createdFeaturestoreS3ConnectorDTO;
  }
  
  /**
   * Creates the secret name as a concatenation of featurestoreId and connector name
   * @param featurestoreId
   * @param connectorName
   * @return the secret name: connectorName_featurestoreId
   */
  private String createSecretName(Integer featurestoreId, String connectorName) {
    return connectorName.replaceAll(" ", "_").toLowerCase() + "_" + featurestoreId;
  }
  
  /**
   * Updates a S3 connection as a backend for a feature store
   *
   * @param featurestore the featurestore
   * @param featurestoreS3ConnectorDTO the data to use when creating the storage connector
   * @param storageConnectorId the id of the connector
   * @return DTO of the updated entity
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public FeaturestoreS3ConnectorDTO updateFeaturestoreS3Connector(
      Users user, Featurestore featurestore, FeaturestoreS3ConnectorDTO featurestoreS3ConnectorDTO,
      Integer storageConnectorId) throws FeaturestoreException, UserException {
    FeaturestoreS3Connector featurestoreS3Connector = verifyS3ConnectorId(storageConnectorId, featurestore);
    String oldConnectorName = featurestoreS3Connector.getName();
    
    verifyS3ConnectorName(featurestoreS3ConnectorDTO.getName(), featurestore, true);
    featurestoreS3Connector.setName(featurestoreS3ConnectorDTO.getName());
    verifyS3ConnectorDescription(featurestoreS3ConnectorDTO.getDescription());
    featurestoreS3Connector.setDescription(featurestoreS3ConnectorDTO.getDescription());
    verifyS3ConnectorBucket(featurestoreS3ConnectorDTO.getBucket());
    featurestoreS3Connector.setBucket(featurestoreS3ConnectorDTO.getBucket());

    if (featurestoreS3ConnectorDTO.getServerEncryptionAlgorithm() != null) {
      FeaturestoreS3ConnectorEncryptionAlgorithm serverEncryptionAlgorithm =
          getEncryptionAlgorithm(featurestoreS3ConnectorDTO.getServerEncryptionAlgorithm());
      featurestoreS3Connector.setServerEncryptionAlgorithm(serverEncryptionAlgorithm);
      if (serverEncryptionAlgorithm.isRequiresKey()) {
        verifyS3ConnectorServerEncryptionKey(featurestoreS3ConnectorDTO.getServerEncryptionKey());
        featurestoreS3Connector.setServerEncryptionKey(featurestoreS3ConnectorDTO.getServerEncryptionKey());
      } else { featurestoreS3Connector.setServerEncryptionKey(null); }
    } else if (!Strings.isNullOrEmpty(featurestoreS3ConnectorDTO.getServerEncryptionKey())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_S3_CONNECTOR_SERVER_ENCRYPTION_ALGORITHM,
        Level.FINE, ", encryption algorithm not provided");
    } else {
      featurestoreS3Connector.setServerEncryptionAlgorithm(null);
      featurestoreS3Connector.setServerEncryptionKey(null);
    }

    if (featurestore != null) {
      featurestoreS3Connector.setFeaturestore(featurestore);
    }
  
    String oldSecretName = createSecretName(featurestore.getId(), oldConnectorName);
    String newSecretName = createSecretName(featurestore.getId(), featurestoreS3Connector.getName());
    Optional<Secret> secretOptional = secretsFacade.findByName(oldSecretName);

    if (!settings.isIAMRoleConfigured()) {
      verifyS3ConnectorAccessKey(featurestoreS3ConnectorDTO.getAccessKey());
      verifyS3ConnectorSecretKey(featurestoreS3ConnectorDTO.getSecretKey());
      String secretString = createS3AccessAndSecretKeysSecret(featurestoreS3ConnectorDTO.getAccessKey(),
        featurestoreS3ConnectorDTO.getSecretKey());
      Secret newSecret = secretsController.validateAndCreateSecret(new SecretId(user.getUid(), newSecretName), user,
              secretString, VisibilityType.PROJECT, featurestore.getProject().getId());
      if (secretOptional.isPresent() && !newSecretName.equals(oldSecretName)) {
        secretsFacade.deleteSecret(secretOptional.get().getId());
        secretsFacade.persist(newSecret);
      } else if (secretOptional.isPresent()) {
        secretOptional.get().setSecret(newSecret.getSecret());
        secretsFacade.update(secretOptional.get());
      } else {
        secretsFacade.persist(newSecret);
      }
    } else {
      verifySecretAndAccessKeysForIamRole(featurestoreS3ConnectorDTO);
    }
    FeaturestoreS3Connector updatedFeaturestoreS3Connector =
      featurestoreS3ConnectorFacade.updateS3Connector(featurestoreS3Connector);
    FeaturestoreS3ConnectorDTO updatedFeaturestoreS3ConnectorDTO =
      new FeaturestoreS3ConnectorDTO(updatedFeaturestoreS3Connector);
    updatedFeaturestoreS3ConnectorDTO.setAccessKey(featurestoreS3ConnectorDTO.getSecretKey());
    updatedFeaturestoreS3ConnectorDTO.setSecretKey(featurestoreS3ConnectorDTO.getAccessKey());
    return updatedFeaturestoreS3ConnectorDTO;
  }

  /**
   * Removes an S3 connector from the feature store
   *
   * @param featurestoreS3Id id of the connection to remove
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public FeaturestoreS3ConnectorDTO removeFeaturestoreS3Connector(Users user, Integer featurestoreS3Id) {
    FeaturestoreS3Connector featurestoreS3Connector = featurestoreS3ConnectorFacade.find(featurestoreS3Id);
    FeaturestoreS3ConnectorDTO featurestoreS3ConnectorDTO = new FeaturestoreS3ConnectorDTO(featurestoreS3Connector);
    if (!settings.isIAMRoleConfigured()) {
      try {
        FeaturestoreS3ConnectorAccessAndSecretKey accessAndSecretKey =
            getS3AccessAndSecretKeySecretForConnector(user, featurestoreS3ConnectorDTO);
        setAccessAndSecretKeysInDTO(featurestoreS3ConnectorDTO, accessAndSecretKey);
      } catch (FeaturestoreException e) {
        //Can still proceed to delete if user is member regardless of the error. Maybe the secret was not properly
        // configured
      }
      Optional<Secret> secret = secretsFacade.findByName(
          createSecretName(featurestoreS3Connector.getFeaturestore().getId(), featurestoreS3Connector.getName()));
      secret.ifPresent(s -> secretsFacade.deleteSecret(s.getId()));
    }
    featurestoreS3ConnectorFacade.remove(featurestoreS3Connector);
    return featurestoreS3ConnectorDTO;
  }

  /**
   * Verifies a storage connector id
   *
   * @param s3ConnectorId the id to verify
   * @param featurestore the featurestore to query
   * @return the storage connector with the given id
   * @throws FeaturestoreException
   */
  private FeaturestoreS3Connector verifyS3ConnectorId(
      Integer s3ConnectorId, Featurestore featurestore) throws FeaturestoreException {
    return featurestoreS3ConnectorFacade.findByIdAndFeaturestore(s3ConnectorId, featurestore)
        .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.S3_CONNECTOR_NOT_FOUND,
            Level.FINE, "S3 connector id: " + s3ConnectorId));
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
   * @param name the input to validate
   * @param featurestore the featurestore of the connector
   * @param edit boolean flag whether the validation if for updating an existing connector or creating a new one
   * @throws FeaturestoreException
   */
  private void verifyS3ConnectorName(String name, Featurestore featurestore, Boolean edit)
    throws FeaturestoreException {
    if (Strings.isNullOrEmpty(name)) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_NAME, Level.FINE,
              ", the storage connector name cannot be empty");
    }
    if(name.length() >
      FeaturestoreConstants.STORAGE_CONNECTOR_NAME_MAX_LENGTH) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_NAME, Level.FINE,
          ", the name should be less than " + FeaturestoreConstants.STORAGE_CONNECTOR_NAME_MAX_LENGTH + " " +
            "characters.");
    }
    if(!edit){
      if(featurestore.getFeaturestoreS3ConnectorConnections().stream()
          .anyMatch(s3Con -> s3Con.getName().equalsIgnoreCase(name))) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_NAME, Level.FINE,
            ", the storage connector name should be unique, there already exists a S3 connector with the same name ");
      }
    }
  }

  /**
   * Validates user input description string
   * @param  description the input to validate
   * @throws FeaturestoreException
   */
  private void verifyS3ConnectorDescription(String description) throws FeaturestoreException {
    if(description.length()
        > FeaturestoreConstants.STORAGE_CONNECTOR_DESCRIPTION_MAX_LENGTH){
      throw new FeaturestoreException(
          RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_DESCRIPTION, Level.FINE,
              ", the description should be less than: " +
                FeaturestoreConstants.STORAGE_CONNECTOR_DESCRIPTION_MAX_LENGTH);
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
        bucket.length() >
          FeaturestoreConstants.S3_STORAGE_CONNECTOR_BUCKET_MAX_LENGTH) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_S3_CONNECTOR_BUCKET, Level.FINE,
          ", the S3 bucket string should not be empty and not exceed: " +
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
          ", the S3 access key should not exceed: " +
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
          ", the S3 secret key should not exceed: " +
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
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.ILLEGAL_S3_CONNECTOR_SERVER_ENCRYPTION_KEY,
        Level.FINE,
        "S3 server encryption key cannot be empty"
      );
    } else if (serverEncryptionKey.length() > FeaturestoreConstants.S3_STORAGE_SERVER_ENCRYPTION_KEY_MAX_LENGTH) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_S3_CONNECTOR_SERVER_ENCRYPTION_KEY,
        Level.FINE, ", the S3 server encryption key should not exceed: " +
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
    verifyS3ConnectorName(featurestoreS3ConnectorDTO.getName(),featurestore, false);
    verifyS3ConnectorDescription(featurestoreS3ConnectorDTO.getDescription());
    verifyS3ConnectorBucket(featurestoreS3ConnectorDTO.getBucket());
    
    if (settings.isIAMRoleConfigured()) {
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
        Level.FINE, ", encryption algorithm not provided");
    }
  }
  
  /**
   * If the user has IAM Role configured to true, they cannot provide access and secret keys
   * @param featurestoreS3ConnectorDTO
   * @throws FeaturestoreException
   */
  private void verifySecretAndAccessKeysForIamRole(FeaturestoreS3ConnectorDTO featurestoreS3ConnectorDTO)
      throws FeaturestoreException{
    if (!Strings.isNullOrEmpty(featurestoreS3ConnectorDTO.getAccessKey()) ||
        !Strings.isNullOrEmpty(featurestoreS3ConnectorDTO.getSecretKey())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.S3_KEYS_FORBIDDEN, Level.FINE,
        "S3 Access Keys are not allowed"
      );
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
        Level.FINE, ", " + e.getMessage());
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
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public List<FeaturestoreStorageConnectorDTO> getS3ConnectorsForFeaturestore(Users user, Featurestore featurestore)
    throws FeaturestoreException {
    List<FeaturestoreStorageConnectorDTO> s3ConnectorDTOs = new ArrayList<>();
    List<FeaturestoreS3Connector> s3Connectors = featurestoreS3ConnectorFacade.findByFeaturestore(featurestore);
    for(FeaturestoreS3Connector s3Connector : s3Connectors) {
      FeaturestoreS3ConnectorDTO featurestoreS3ConnectorDTO = new FeaturestoreS3ConnectorDTO(s3Connector);
      if (!settings.isIAMRoleConfigured()) {
        FeaturestoreS3ConnectorAccessAndSecretKey accessAndSecretKey =
            getS3AccessAndSecretKeySecretForConnector(user, featurestoreS3ConnectorDTO);
        setAccessAndSecretKeysInDTO(featurestoreS3ConnectorDTO, accessAndSecretKey);
      }
      s3ConnectorDTOs.add(featurestoreS3ConnectorDTO);
    }
    return s3ConnectorDTOs;
  }

  /**
   * Retrieves a S3 Connector with a particular id from a particular featurestore
   *
   * @param user the user making the request
   * @param id           id of the s3 connector
   * @param featurestore the featurestore that the connector belongs to
   * @return XML/JSON DTO of the S3 Connector
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public FeaturestoreS3ConnectorDTO getS3ConnectorWithIdAndFeaturestore(Users user, Featurestore featurestore,
    Integer id)
      throws FeaturestoreException {
    FeaturestoreS3Connector featurestoreS3Connector = verifyS3ConnectorId(id, featurestore);
    FeaturestoreS3ConnectorDTO featurestoreS3ConnectorDTO = new FeaturestoreS3ConnectorDTO(featurestoreS3Connector);
    if (!settings.isIAMRoleConfigured()) {
      FeaturestoreS3ConnectorAccessAndSecretKey accessAndSecretKey =
          getS3AccessAndSecretKeySecretForConnector(user, featurestoreS3ConnectorDTO);
      setAccessAndSecretKeysInDTO(featurestoreS3ConnectorDTO, accessAndSecretKey);
    }
    return  featurestoreS3ConnectorDTO;
  }
  
  /**
   * Get the access and secret key pair from the secret store
   *
   * @param user the user making the request
   * @param featurestoreS3ConnectorDTO
   * @return FeaturestoreS3ConnectorAccessAndSecretKey object
   * @throws FeaturestoreException
   */
  private FeaturestoreS3ConnectorAccessAndSecretKey getS3AccessAndSecretKeySecretForConnector(
      Users user, FeaturestoreS3ConnectorDTO featurestoreS3ConnectorDTO) throws FeaturestoreException {
    String secretName =
        createSecretName(featurestoreS3ConnectorDTO.getFeaturestoreId(), featurestoreS3ConnectorDTO.getName());
    Secret secret = secretsFacade.findByName(secretName)
        .orElseThrow(() -> new FeaturestoreException(
            RESTCodes.FeaturestoreErrorCode.ERROR_GETTING_S3_CONNECTOR_ACCESS_AND_SECRET_KEY_FROM_SECRET,
            Level.FINE, "Could not find the secret name for connector " + featurestoreS3ConnectorDTO.getName()));

    try {
      Users ownerUser = userFacade.find(secret.getId().getUid());
      SecretPlaintext plainText = secretsController.getShared(user, ownerUser.getUsername(), secretName);
      return new ObjectMapper().readValue(plainText.getPlaintext(), FeaturestoreS3ConnectorAccessAndSecretKey.class);
    } catch (UserException | ProjectException | ServiceException | IOException e ) {
      //Just return empty
      return new FeaturestoreS3ConnectorAccessAndSecretKey();
    }
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
