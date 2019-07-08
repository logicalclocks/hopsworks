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

package io.hops.hopsworks.common.dao.featurestore.storageconnector.s3;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.featurestore.Featurestore;
import io.hops.hopsworks.common.dao.featurestore.settings.FeaturestoreClientSettingsDTO;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Class controlling the interaction with the feature_store_s3_connector table and required business logic
 */
@Stateless
public class FeaturestoreS3ConnectorController {
  @EJB
  private FeaturestoreS3ConnectorFacade featurestoreS3ConnectorFacade;
  
  /**
   * Stores an S3 connection as a backend for a feature store
   *
   * @param featurestore the featurestore
   * @param featurestoreS3ConnectorDTO the data to use when creating the storage connector
   * @return DTO of the created entity
   */
  public FeaturestoreS3ConnectorDTO createFeaturestoreS3Connector(
      Featurestore featurestore, FeaturestoreS3ConnectorDTO featurestoreS3ConnectorDTO){
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
   * Validates user input for creating a new S3 connector in a featurestore
   *
   * @param featurestore the featuerstore
   * @param featurestoreS3ConnectorDTO the data to use when creating the storage connector
   */
  public void verifyUserInput(Featurestore featurestore, FeaturestoreS3ConnectorDTO featurestoreS3ConnectorDTO){
  
    if (featurestore == null) {
      throw new IllegalArgumentException("Featurestore was not found");
    }

    if (featurestoreS3ConnectorDTO == null) {
      throw new IllegalArgumentException("Null input data");
    }
  
    if (Strings.isNullOrEmpty(featurestoreS3ConnectorDTO.getName())) {
      throw new IllegalArgumentException(
        RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_NAME.getMessage() + ", the storage connector name " +
          "cannot be empty");
    }
  
    Pattern namePattern = Pattern.compile(FeaturestoreClientSettingsDTO.FEATURESTORE_REGEX);
  
    if(featurestoreS3ConnectorDTO.getName().length() >
      FeaturestoreClientSettingsDTO.STORAGE_CONNECTOR_NAME_MAX_LENGTH ||
        !namePattern.matcher(featurestoreS3ConnectorDTO.getName()).matches()) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_NAME.getMessage()
        + ", the name should be less than "
        + FeaturestoreClientSettingsDTO.STORAGE_CONNECTOR_NAME_MAX_LENGTH + " characters and match " +
        "the regular expression: " +  FeaturestoreClientSettingsDTO.FEATURESTORE_REGEX);
    }
  
    if(featurestore.getFeaturestoreS3ConnectorConnections().stream()
      .anyMatch(s3Con -> s3Con.getName().equalsIgnoreCase(featurestoreS3ConnectorDTO.getName()))) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_NAME.getMessage() +
        ", the storage connector name should be unique, there already exists a S3 connector with the same name ");
    }
  
    if(featurestoreS3ConnectorDTO.getDescription().length()
      > FeaturestoreClientSettingsDTO.STORAGE_CONNECTOR_DESCRIPTION_MAX_LENGTH){
      throw new IllegalArgumentException(
        RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_DESCRIPTION.getMessage() +
        ", the description should be less than: " +
          FeaturestoreClientSettingsDTO.STORAGE_CONNECTOR_DESCRIPTION_MAX_LENGTH);
    }
    
    if(Strings.isNullOrEmpty(featurestoreS3ConnectorDTO.getBucket()) ||
        featurestoreS3ConnectorDTO.getBucket().length() >
          FeaturestoreClientSettingsDTO.S3_STORAGE_CONNECTOR_BUCKET_MAX_LENGTH) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_S3_CONNECTOR_BUCKET.getMessage() +
        ", the S3 bucket string should not be empty and not exceed: " +
        FeaturestoreClientSettingsDTO.S3_STORAGE_CONNECTOR_BUCKET_MAX_LENGTH + " characters");
    }
  
    if(!Strings.isNullOrEmpty(featurestoreS3ConnectorDTO.getAccessKey()) &&
        featurestoreS3ConnectorDTO.getAccessKey().length()
          > FeaturestoreClientSettingsDTO.S3_STORAGE_CONNECTOR_ACCESSKEY_MAX_LENGTH) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_S3_CONNECTOR_ACCESS_KEY.getMessage() +
        ", the S3 access key should not exceed: " +
        FeaturestoreClientSettingsDTO.S3_STORAGE_CONNECTOR_ACCESSKEY_MAX_LENGTH + " characters");
    }
  
    if(!Strings.isNullOrEmpty(featurestoreS3ConnectorDTO.getSecretKey()) &&
        featurestoreS3ConnectorDTO.getSecretKey().length() >
          FeaturestoreClientSettingsDTO.S3_STORAGE_CONNECTOR_SECRETKEY_MAX_LENGTH) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_S3_CONNECTOR_SECRET_KEY.getMessage() +
        ", the S3 secret key should not exceed: " +
        FeaturestoreClientSettingsDTO.S3_STORAGE_CONNECTOR_SECRETKEY_MAX_LENGTH + " characters");
    }
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
    FeaturestoreS3Connector featurestoreS3Connector =
        featurestoreS3ConnectorFacade.findByIdAndFeaturestore(id, featurestore);
    if (featurestoreS3Connector == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.S3_CONNECTOR_NOT_FOUND,
          Level.FINE, "s3ConnectorId: " + id);
    }
    return new FeaturestoreS3ConnectorDTO(featurestoreS3Connector);
  }
}
