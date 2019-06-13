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

package io.hops.hopsworks.common.dao.featurestore.storage_connectors.s3;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.featurestore.Featurestore;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.regex.Pattern;

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
   * @param accessKey the S3 access key
   * @param secretKey the S3 secret key
   * @param bucket the S3 bucket
   * @param description the S3 description
   * @param name the S3 name
   * @return DTO of the created entity
   */
  public FeaturestoreS3ConnectorDTO createFeaturestoreS3Connector(Featurestore featurestore, String accessKey,
    String secretKey,  String bucket, String description, String name){
    FeaturestoreS3Connector featurestoreS3Connector = new FeaturestoreS3Connector();
    featurestoreS3Connector.setAccessKey(accessKey);
    featurestoreS3Connector.setBucket(bucket);
    featurestoreS3Connector.setDescription(description);
    featurestoreS3Connector.setName(name);
    featurestoreS3Connector.setSecretKey(secretKey);
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
   * @param accessKey the S3 Access Key
   * @param secretKey the S3 Secret Key
   * @param s3Bucket the S3 Bucket
   * @param description the description
   * @param name the name of the connector
   */
  public void verifyUserInput(Featurestore featurestore, String accessKey, String secretKey, String s3Bucket,
    String description, String name){
  
    if (featurestore == null) {
      throw new IllegalArgumentException("Featurestore was not found");
    }
  
    if (Strings.isNullOrEmpty(name)) {
      throw new IllegalArgumentException(
        RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_NAME.getMessage() + ", the storage connector name " +
          "cannot be empty");
    }
  
    Pattern namePattern = Pattern.compile(Settings.HOPS_FEATURESTORE_REGEX);
  
    if(name.length() > Settings.HOPS_STORAGE_CONNECTOR_NAME_MAX_LENGTH || !namePattern.matcher(name).matches()) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_NAME.getMessage()
        + ", the name should be less than "
        + Settings.HOPS_STORAGE_CONNECTOR_NAME_MAX_LENGTH + " characters and match " +
        "the regular expression: " +  Settings.HOPS_FEATURESTORE_REGEX);
    }
  
    if(featurestore.getFeaturestoreS3ConnectorConnections().stream()
      .anyMatch(s3Con -> s3Con.getName().equalsIgnoreCase(name))) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_NAME.getMessage() +
        ", the storage connector name should be unique, there already exists a S3 connector with the same name ");
    }
  
    if(description.length() > Settings.HOPS_STORAGE_CONNECTOR_DESCRIPTION_MAX_LENGTH){
      throw new IllegalArgumentException(
        RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_DESCRIPTION.getMessage() +
        ", the description should be less than: " + Settings.HOPS_STORAGE_CONNECTOR_DESCRIPTION_MAX_LENGTH);
    }
    
    if(Strings.isNullOrEmpty(s3Bucket) || s3Bucket.length() > Settings.HOPS_S3_STORAGE_CONNECTOR_BUCKET_MAX_LENGTH) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_S3_CONNECTOR_BUCKET.getMessage() +
        ", the S3 bucket string should not be empty and not exceed: " +
        Settings.HOPS_S3_STORAGE_CONNECTOR_BUCKET_MAX_LENGTH + " characters");
    }
  
    if(!Strings.isNullOrEmpty(accessKey) &&
      accessKey.length() > Settings.HOPS_S3_STORAGE_CONNECTOR_ACCESSKEY_MAX_LENGTH) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_S3_CONNECTOR_ACCESS_KEY.getMessage() +
        ", the S3 access key should not exceed: " +
        Settings.HOPS_S3_STORAGE_CONNECTOR_ACCESSKEY_MAX_LENGTH + " characters");
    }
  
    if(!Strings.isNullOrEmpty(secretKey) && secretKey.length() >
      Settings.HOPS_S3_STORAGE_CONNECTOR_SECRETKEY_MAX_LENGTH) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_S3_CONNECTOR_SECRET_KEY.getMessage() +
        ", the S3 secret key should not exceed: " +
        Settings.HOPS_S3_STORAGE_CONNECTOR_SECRETKEY_MAX_LENGTH + " characters");
    }
  }
}
