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

package io.hops.hopsworks.common.featurestore.storageconnectors;

import io.hops.hopsworks.common.featurestore.FeaturestoreConstants;
import io.hops.hopsworks.common.featurestore.storageconnectors.hopsfs.FeaturestoreHopsfsConnectorController;
import io.hops.hopsworks.common.featurestore.storageconnectors.hopsfs.FeaturestoreHopsfsConnectorDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.jdbc.FeaturestoreJdbcConnectorController;
import io.hops.hopsworks.common.featurestore.storageconnectors.jdbc.FeaturestoreJdbcConnectorDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.s3.FeaturestoreS3ConnectorController;
import io.hops.hopsworks.common.featurestore.storageconnectors.s3.FeaturestoreS3ConnectorDTO;
import io.hops.hopsworks.common.security.secrets.SecretsController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Controller class for operations on storage controller in the Hopsworks Feature Store
 */
@Stateless
public class FeaturestoreStorageConnectorController {
  @EJB
  private FeaturestoreHopsfsConnectorController featurestoreHopsfsConnectorController;
  @EJB
  private FeaturestoreJdbcConnectorController featurestoreJdbcConnectorController;
  @EJB
  private FeaturestoreS3ConnectorController featurestoreS3ConnectorController;
  @EJB
  private SecretsController secretsController;


  /**
   * Returns a list with DTOs of all storage connectors for a featurestore
   *
   * @param featurestore the featurestore to query
   * @return List of JSON/XML DTOs of the storage connectors
   */
  public List<FeaturestoreStorageConnectorDTO> getAllStorageConnectorsForFeaturestore(Featurestore featurestore) {
    List<FeaturestoreStorageConnectorDTO> featurestoreStorageConnectorDTOS = new ArrayList<>();
    featurestoreStorageConnectorDTOS.addAll(
        featurestoreJdbcConnectorController.getJdbcConnectorsForFeaturestore(featurestore));
    featurestoreStorageConnectorDTOS.addAll(
        featurestoreS3ConnectorController.getS3ConnectorsForFeaturestore(featurestore));
    featurestoreStorageConnectorDTOS.addAll(featurestoreHopsfsConnectorController.getHopsfsConnectors(featurestore));
    return featurestoreStorageConnectorDTOS;
  }

  /**
   * Returns a list with DTOs of all storage connectors for a featurestore with a specific type
   *
   * @param featurestore the featurestore to query
   * @param featurestoreStorageConnectorType the type of the storage connector
   * @return List of JSON/XML DTOs of the storage connectors
   */
  public List<FeaturestoreStorageConnectorDTO> getAllStorageConnectorsForFeaturestoreWithType(
      Featurestore featurestore, FeaturestoreStorageConnectorType featurestoreStorageConnectorType) {
    switch(featurestoreStorageConnectorType) {
      case S3:
        return featurestoreS3ConnectorController.getS3ConnectorsForFeaturestore(featurestore);
      case JDBC:
        return featurestoreJdbcConnectorController.getJdbcConnectorsForFeaturestore(featurestore);
      case HOPSFS:
        return featurestoreHopsfsConnectorController.getHopsfsConnectors(featurestore);
      default:
        throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_TYPE.getMessage()
            + ", Recognized storage connector types are: " + FeaturestoreStorageConnectorType.HOPSFS + ", " +
            FeaturestoreStorageConnectorType.S3 + ", and " + FeaturestoreStorageConnectorType.JDBC
            + ". The provided training dataset type was not recognized: " + featurestoreStorageConnectorType);
    }
  }

  /**
   * Returns a DTO of a storage connectors for a featurestore with a specific type and id
   *
   * @param featurestore the featurestore to query
   * @param featurestoreStorageConnectorType the type of the storage connector
   * @param storageConnectorId id of the storage connector
   * @return JSON/XML DTOs of the storage connector
   */
  public FeaturestoreStorageConnectorDTO getStorageConnectorForFeaturestoreWithTypeAndId(
      Featurestore featurestore, FeaturestoreStorageConnectorType featurestoreStorageConnectorType,
      Integer storageConnectorId) throws FeaturestoreException {
    switch(featurestoreStorageConnectorType) {
      case S3:
        return featurestoreS3ConnectorController.getS3ConnectorWithIdAndFeaturestore(featurestore, storageConnectorId);
      case JDBC:
        return featurestoreJdbcConnectorController.getJdbcConnectorWithIdAndFeaturestore(featurestore,
            storageConnectorId);
      case HOPSFS:
        return featurestoreHopsfsConnectorController.getHopsFsConnectorWithIdAndFeaturestore(featurestore,
            storageConnectorId);
      default:
        throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_TYPE.getMessage()
            + ", Recognized storage connector types are: " + FeaturestoreStorageConnectorType.HOPSFS + ", " +
            FeaturestoreStorageConnectorType.S3 + ", and " + FeaturestoreStorageConnectorType.JDBC
            + ". The provided training dataset type was not recognized: " + featurestoreStorageConnectorType);
    }
  }

  /**
   * Creates a new Storage Connector of a specific type in a feature store
   *
   * @param featurestore the featurestore to create the new connector
   * @param featurestoreStorageConnectorType the type of the storage connector
   * @param featurestoreStorageConnectorDTO the data to use when creating the storage connector
   * @return A JSON/XML DTOs representation of the created storage connector
   * @throws FeaturestoreException
   */
  public FeaturestoreStorageConnectorDTO createStorageConnectorWithType(
      Featurestore featurestore, FeaturestoreStorageConnectorType featurestoreStorageConnectorType,
      FeaturestoreStorageConnectorDTO featurestoreStorageConnectorDTO) throws FeaturestoreException {
    switch(featurestoreStorageConnectorType) {
      case S3:
        return featurestoreS3ConnectorController.createFeaturestoreS3Connector(featurestore,
            (FeaturestoreS3ConnectorDTO) featurestoreStorageConnectorDTO);
      case JDBC:
        return featurestoreJdbcConnectorController.createFeaturestoreJdbcConnector(featurestore,
            (FeaturestoreJdbcConnectorDTO) featurestoreStorageConnectorDTO);
      case HOPSFS:
        return featurestoreHopsfsConnectorController.createFeaturestoreHopsfsConnector(featurestore,
            (FeaturestoreHopsfsConnectorDTO) featurestoreStorageConnectorDTO);
      default:
        throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_TYPE.getMessage()
            + ", Recognized storage connector types are: " + FeaturestoreStorageConnectorType.HOPSFS + ", " +
            FeaturestoreStorageConnectorType.S3 + ", and " + FeaturestoreStorageConnectorType.JDBC
            + ". The provided training dataset type was not recognized: " + featurestoreStorageConnectorType);
    }
  }

  /**
   * Updates an existing Storage Connector of a specific type in a feature store
   *
   * @param featurestore the featurestore where the connector exists
   * @param featurestoreStorageConnectorType the type of the storage connector
   * @param featurestoreStorageConnectorDTO the data to use when updating the storage connector
   * @param storageConnectorId id of the connector
   * @return A JSON/XML DTOs representation of the updated storage connector
   */
  public FeaturestoreStorageConnectorDTO updateStorageConnectorWithType(
      Featurestore featurestore, FeaturestoreStorageConnectorType featurestoreStorageConnectorType,
      FeaturestoreStorageConnectorDTO featurestoreStorageConnectorDTO, Integer storageConnectorId)
      throws FeaturestoreException {
    switch(featurestoreStorageConnectorType) {
      case S3:
        return featurestoreS3ConnectorController.updateFeaturestoreS3Connector(featurestore,
            (FeaturestoreS3ConnectorDTO) featurestoreStorageConnectorDTO, storageConnectorId);
      case JDBC:
        return featurestoreJdbcConnectorController.updateFeaturestoreJdbcConnector(featurestore,
            (FeaturestoreJdbcConnectorDTO) featurestoreStorageConnectorDTO, storageConnectorId);
      case HOPSFS:
        return featurestoreHopsfsConnectorController.updateFeaturestoreHopsfsConnector(featurestore,
            (FeaturestoreHopsfsConnectorDTO) featurestoreStorageConnectorDTO, storageConnectorId);
      default:
        throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_TYPE.getMessage()
            + ", Recognized storage connector types are: " + FeaturestoreStorageConnectorType.HOPSFS + ", " +
            FeaturestoreStorageConnectorType.S3 + ", and " + FeaturestoreStorageConnectorType.JDBC
            + ". The provided training dataset type was not recognized: " + featurestoreStorageConnectorType);
    }
  }

  /**
   * Deletes a storage connector with a specific type and id in a feature store
   *
   * @param featurestoreStorageConnectorType the type of the storage connector
   * @param storageConnectorId id of the storage connector
   * @return JSON/XML DTOs of the deleted storage connector
   */
  public FeaturestoreStorageConnectorDTO deleteStorageConnectorWithTypeAndId(
      FeaturestoreStorageConnectorType featurestoreStorageConnectorType, Integer storageConnectorId) {
    switch(featurestoreStorageConnectorType) {
      case S3:
        return featurestoreS3ConnectorController.removeFeaturestoreS3Connector(storageConnectorId);
      case JDBC:
        return featurestoreJdbcConnectorController.removeFeaturestoreJdbcConnector(storageConnectorId);
      case HOPSFS:
        return featurestoreHopsfsConnectorController.removeFeaturestoreHopsfsConnector(storageConnectorId);
      default:
        throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_TYPE.getMessage()
            + ", Recognized storage connector types are: " + FeaturestoreStorageConnectorType.HOPSFS + ", " +
            FeaturestoreStorageConnectorType.S3 + ", and " + FeaturestoreStorageConnectorType.JDBC
            + ". The provided training dataset type was not recognized: " + featurestoreStorageConnectorType);
    }
  }
  
  /**
   * Gets the JDBC connector of the online featurestore for a particular user and project. This connector is different
   * from other connectors in that it includes a password reference to the secretsmanager that needs to be resolved.
   *
   * @param user         the user making the request
   * @param project      the project of the user
   * @param dbUsername   the database username
   * @param featurestore the featurestore metadata
   * @return a JDBC DTO connector for the online featurestore.
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public FeaturestoreJdbcConnectorDTO getOnlineFeaturestoreConnector(Users user, Project project, String dbUsername,
    Featurestore featurestore, String dbName) throws FeaturestoreException {
    
    //Step 1 Get the connector from the database
    FeaturestoreJdbcConnectorDTO featurestoreJdbcConnectorDTO = null;
    String onlineFeaturestoreConnectorName = dbUsername + FeaturestoreConstants.ONLINE_FEATURE_STORE_CONNECTOR_SUFFIX;
    List<FeaturestoreStorageConnectorDTO> jdbcConnectorDTOS =
      featurestoreJdbcConnectorController.getJdbcConnectorsForFeaturestore(featurestore);
    List<FeaturestoreStorageConnectorDTO> matchingConnectors = jdbcConnectorDTOS.stream().filter(connector ->
      connector.getName().equalsIgnoreCase(onlineFeaturestoreConnectorName)).collect(Collectors.toList());
    if(matchingConnectors.isEmpty()) {
      featurestoreJdbcConnectorDTO =
        featurestoreJdbcConnectorController.createJdbcConnectorForOnlineFeaturestore(dbUsername,
        featurestore, dbName);
    } else {
      featurestoreJdbcConnectorDTO = (FeaturestoreJdbcConnectorDTO) matchingConnectors.get(0);
    }
    
    //Step 2: replace the placeholder with the password and return it
    try {
      String password = secretsController.get(user, dbUsername).getPlaintext();
      featurestoreJdbcConnectorDTO.setArguments(featurestoreJdbcConnectorDTO.getArguments()
        .replaceFirst(FeaturestoreConstants.ONLINE_FEATURE_STORE_CONNECTOR_PASSWORD_TEMPLATE, password));
    } catch (UserException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ONLINE_SECRETS_ERROR,
        Level.SEVERE, "Problem getting secrets for the JDBC connection to the online FS");
    }
    return featurestoreJdbcConnectorDTO;
  }

}
