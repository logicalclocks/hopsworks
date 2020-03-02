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
    verifyUserInput(featurestore, featurestoreS3ConnectorDTO);
    FeaturestoreS3Connector featurestoreS3Connector = new FeaturestoreS3Connector();
    featurestoreS3Connector.setAccessKey(featurestoreS3ConnectorDTO.getAccessKey());
    featurestoreS3Connector.setBucket(featurestoreS3ConnectorDTO.getBucket());
    featurestoreS3Connector.setDescription(featurestoreS3ConnectorDTO.getDescription());
    featurestoreS3Connector.setName(featurestoreS3ConnectorDTO.getName());
    featurestoreS3Connector.setSecretKey(featurestoreS3ConnectorDTO.getSecretKey());
    featurestoreS3Connector.setFeaturestore(featurestore);
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
    if(!Strings.isNullOrEmpty(featurestoreS3ConnectorDTO.getName())){
      verifyS3ConnectorName(featurestoreS3ConnectorDTO.getName(), featurestore, true);
      featurestoreS3Connector.setName(featurestoreS3ConnectorDTO.getName());
    }
    if(!Strings.isNullOrEmpty(featurestoreS3ConnectorDTO.getDescription())){
      verifyS3ConnectorDescription(featurestoreS3ConnectorDTO.getDescription());
      featurestoreS3Connector.setDescription(featurestoreS3ConnectorDTO.getDescription());
    }
    if(!Strings.isNullOrEmpty(featurestoreS3ConnectorDTO.getAccessKey())){
      verifyS3ConnectorAccessKey(featurestoreS3ConnectorDTO.getAccessKey());
      featurestoreS3Connector.setAccessKey(featurestoreS3ConnectorDTO.getAccessKey());
    }
    if(!Strings.isNullOrEmpty(featurestoreS3ConnectorDTO.getSecretKey())){
      verifyS3ConnectorSecretKey(featurestoreS3ConnectorDTO.getSecretKey());
      featurestoreS3Connector.setSecretKey(featurestoreS3ConnectorDTO.getSecretKey());
    }
    if(!Strings.isNullOrEmpty(featurestoreS3ConnectorDTO.getBucket())){
      verifyS3ConnectorSecretKey(featurestoreS3ConnectorDTO.getBucket());
      featurestoreS3Connector.setBucket(featurestoreS3ConnectorDTO.getBucket());
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
    FeaturestoreS3Connector featurestoreS3Connector =
        featurestoreS3ConnectorFacade.findByIdAndFeaturestore(s3ConnectorId, featurestore);
    if (featurestoreS3Connector == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.S3_CONNECTOR_NOT_FOUND,
          Level.FINE, "s3ConnectorId: " + s3ConnectorId);
    }
    return featurestoreS3Connector;
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
    if(Strings.isNullOrEmpty(bucket) ||
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
    if(!Strings.isNullOrEmpty(accessKey) &&
        accessKey.length() > FeaturestoreConstants.S3_STORAGE_CONNECTOR_ACCESSKEY_MAX_LENGTH) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_S3_CONNECTOR_ACCESS_KEY, Level.FINE,
          ", the S3 access key should not exceed: " +
            FeaturestoreConstants.S3_STORAGE_CONNECTOR_ACCESSKEY_MAX_LENGTH + " characters");
    } else if (!Strings.isNullOrEmpty(accessKey) && settings.isIAMRoleConfigured()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.S3_KEYS_FORBIDDEN, Level.FINE,
          "S3 access key not allowed");
    }
  }

  /**
   * Validates user input secret key string
   *
   * @param secretKey the input to validate
   * @throws FeaturestoreException
   */
  private void verifyS3ConnectorSecretKey(String secretKey) throws FeaturestoreException {
    if(!Strings.isNullOrEmpty(secretKey) &&
        secretKey.length() > FeaturestoreConstants.S3_STORAGE_CONNECTOR_SECRETKEY_MAX_LENGTH) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_S3_CONNECTOR_SECRET_KEY, Level.FINE,
          ", the S3 secret key should not exceed: " +
            FeaturestoreConstants.S3_STORAGE_CONNECTOR_SECRETKEY_MAX_LENGTH + " characters");
    } else if (!Strings.isNullOrEmpty(secretKey) && settings.isIAMRoleConfigured()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.S3_KEYS_FORBIDDEN, Level.FINE,
          "S3 secret key not allowed");
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
    verifyS3ConnectorAccessKey(featurestoreS3ConnectorDTO.getAccessKey());
    verifyS3ConnectorSecretKey(featurestoreS3ConnectorDTO.getSecretKey());
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
