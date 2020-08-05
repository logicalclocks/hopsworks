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

import com.google.common.base.Strings;
import io.hops.hopsworks.common.featurestore.FeaturestoreConstants;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.s3.FeaturestoreS3Connector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.s3.FeaturestoreS3ConnectorEncryptionAlgorithm;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Class controlling the interaction with the feature_store_s3_connector table and required business logic
 */
@Stateless
public class FeaturestoreS3ConnectorController {

  @EJB
  private FeaturestoreS3ConnectorFacade featurestoreS3ConnectorFacade;
  @EJB
  private Settings settings;

  /**
   * Stores an S3 connection as a backend for a feature store
   *
   * @param featurestore the featurestore
   * @param featurestoreS3ConnectorDTO the data to use when creating the storage connector
   * @return DTO of the created entity
   * @throws FeaturestoreException
   */
  public FeaturestoreS3ConnectorDTO createFeaturestoreS3Connector(
      Featurestore featurestore, FeaturestoreS3ConnectorDTO featurestoreS3ConnectorDTO) throws FeaturestoreException {
    FeaturestoreS3ConnectorEncryptionAlgorithm encryptionAlgorithm =
      getEncryptionAlgorithm(featurestoreS3ConnectorDTO.getServerEncryptionAlgorithm());
    
    verifyUserInput(featurestore, featurestoreS3ConnectorDTO);
    FeaturestoreS3Connector featurestoreS3Connector = new FeaturestoreS3Connector();
    featurestoreS3Connector.setAccessKey(featurestoreS3ConnectorDTO.getAccessKey());
    featurestoreS3Connector.setBucket(featurestoreS3ConnectorDTO.getBucket());
    featurestoreS3Connector.setDescription(featurestoreS3ConnectorDTO.getDescription());
    featurestoreS3Connector.setName(featurestoreS3ConnectorDTO.getName());
    featurestoreS3Connector.setSecretKey(featurestoreS3ConnectorDTO.getSecretKey());
    featurestoreS3Connector.setFeaturestore(featurestore);
    featurestoreS3Connector.setServerEncryptionAlgorithm(encryptionAlgorithm);
    featurestoreS3Connector.setServerEncryptionKey(featurestoreS3ConnectorDTO.getServerEncryptionKey());
    featurestoreS3ConnectorFacade.persist(featurestoreS3Connector);
    return new FeaturestoreS3ConnectorDTO(featurestoreS3Connector);
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
  public FeaturestoreS3ConnectorDTO updateFeaturestoreS3Connector(
      Featurestore featurestore, FeaturestoreS3ConnectorDTO featurestoreS3ConnectorDTO,
      Integer storageConnectorId) throws FeaturestoreException {
    FeaturestoreS3Connector featurestoreS3Connector = verifyS3ConnectorId(storageConnectorId, featurestore);
    FeaturestoreS3ConnectorEncryptionAlgorithm serverEncryptionAlgorithm =
      getEncryptionAlgorithm(featurestoreS3ConnectorDTO.getServerEncryptionAlgorithm());
    
    verifyS3ConnectorName(featurestoreS3ConnectorDTO.getName(), featurestore, true);
    featurestoreS3Connector.setName(featurestoreS3ConnectorDTO.getName());
    verifyS3ConnectorName(featurestoreS3ConnectorDTO.getName(), featurestore, true);
    verifyS3ConnectorDescription(featurestoreS3ConnectorDTO.getDescription());
    featurestoreS3Connector.setDescription(featurestoreS3ConnectorDTO.getDescription());
    verifyS3ConnectorBucket(featurestoreS3ConnectorDTO.getBucket());
    featurestoreS3Connector.setBucket(featurestoreS3ConnectorDTO.getBucket());
    if(settings.isIAMRoleConfigured()){
      verifySecretAndAccessKeysForIamRole(featurestoreS3ConnectorDTO);
    } else{
      verifyS3ConnectorAccessKey(featurestoreS3ConnectorDTO.getAccessKey());
      verifyS3ConnectorSecretKey(featurestoreS3ConnectorDTO.getSecretKey());
      featurestoreS3Connector.setAccessKey(featurestoreS3ConnectorDTO.getAccessKey());
      featurestoreS3Connector.setSecretKey(featurestoreS3ConnectorDTO.getSecretKey());
    }
    if(featurestoreS3ConnectorDTO.getServerEncryptionAlgorithm() != null){
      featurestoreS3Connector.setServerEncryptionAlgorithm(serverEncryptionAlgorithm);
      if(serverEncryptionAlgorithm.isRequiresKey()){
        verifyS3ConnectorServerEncryptionKey(featurestoreS3ConnectorDTO.getServerEncryptionKey());
        featurestoreS3Connector.setServerEncryptionKey(featurestoreS3ConnectorDTO.getServerEncryptionKey());
      } else{ featurestoreS3Connector.setServerEncryptionKey(null); }
    } else{
      featurestoreS3Connector.setServerEncryptionAlgorithm(null);
      featurestoreS3Connector.setServerEncryptionKey(null);
    }
    
    if(featurestore != null){
      featurestoreS3Connector.setFeaturestore(featurestore);
    }
    
    FeaturestoreS3Connector updatedFeaturestoreS3Connector =
        featurestoreS3ConnectorFacade.updateS3Connector(featurestoreS3Connector);
    return new FeaturestoreS3ConnectorDTO(updatedFeaturestoreS3Connector);
  }
  
  /**
   * Removes an S3 connection from the feature store
   *
   * @param featurestoreS3Id id of the connection to remove
   */
  public FeaturestoreS3ConnectorDTO removeFeaturestoreS3Connector(Integer featurestoreS3Id){
    FeaturestoreS3Connector featurestoreS3Connector = featurestoreS3ConnectorFacade.find(featurestoreS3Id);
    FeaturestoreS3ConnectorDTO featurestoreS3ConnectorDTO = new FeaturestoreS3ConnectorDTO(featurestoreS3Connector);
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
    } else if (serverEncryptionKey.length() > FeaturestoreConstants.S3_STORAGE_SERVER_ENCRYPTION_KEY_MAX_LENGTH){
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
    } else {
      featurestoreS3ConnectorDTO.setServerEncryptionKey(null);
    }
  }
  
  /**
   * If the user has IAM Role configured to true, they cannot provide access and secret keys
   * @param featurestoreS3ConnectorDTO
   * @throws FeaturestoreException
   */
  private void verifySecretAndAccessKeysForIamRole(FeaturestoreS3ConnectorDTO featurestoreS3ConnectorDTO)
    throws FeaturestoreException{
    if (settings.isIAMRoleConfigured() && (!Strings.isNullOrEmpty(featurestoreS3ConnectorDTO.getAccessKey()) ||
      !Strings.isNullOrEmpty(featurestoreS3ConnectorDTO.getSecretKey()))) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.S3_KEYS_FORBIDDEN,
        Level.FINE,
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
   * Gets all S3 connectors for a particular featurestore and project
   *
   * @param featurestore featurestore to query for s3 connectors
   * @return list of XML/JSON DTOs of the s3 connectors
   */
  public List<FeaturestoreStorageConnectorDTO> getS3ConnectorsForFeaturestore(Featurestore featurestore) {
    List<FeaturestoreS3Connector> s3Connectors = featurestoreS3ConnectorFacade.findByFeaturestore(featurestore);
    return s3Connectors.stream().map(s3Connector -> new FeaturestoreS3ConnectorDTO(s3Connector))
        .collect(Collectors.toList());
  }
  
  /**
   * Retrieves a S3 Connector with a particular id from a particular featurestore
   *
   * @param id           id of the s3 connector
   * @param featurestore the featurestore that the connector belongs to
   * @return XML/JSON DTO of the S3 Connector
   */
  public FeaturestoreS3ConnectorDTO getS3ConnectorWithIdAndFeaturestore(Featurestore featurestore, Integer id)
      throws FeaturestoreException {
    FeaturestoreS3Connector featurestoreS3Connector = verifyS3ConnectorId(id, featurestore);
    return new FeaturestoreS3ConnectorDTO(featurestoreS3Connector);
  }
}
