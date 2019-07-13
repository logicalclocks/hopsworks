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

package io.hops.hopsworks.common.dao.featurestore.storageconnector.jdbc;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.featurestore.Featurestore;
import io.hops.hopsworks.common.dao.featurestore.settings.FeaturestoreClientSettingsDTO;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Class controlling the interaction with the feature_store_jdbc_connector table and required business logic
 */
@Stateless
public class FeaturestoreJdbcConnectorController {
  
  @EJB
  private FeaturestoreJdbcConnectorFacade featurestoreJdbcConnectorFacade;
  @EJB
  private Settings settings;
  
  
  /**
   * Persists a JDBC connection for the feature store
   *
   * @param featurestore the feature store
   * @param featurestoreJdbcConnectorDTO input to data to use when creating the storage connector
   * @return a DTO representing the created entity
   */
  public FeaturestoreJdbcConnectorDTO createFeaturestoreJdbcConnector(
      Featurestore featurestore, FeaturestoreJdbcConnectorDTO featurestoreJdbcConnectorDTO){
    verifyUserInput(featurestore, featurestoreJdbcConnectorDTO);
    FeaturestoreJdbcConnector featurestoreJdbcConnector = new FeaturestoreJdbcConnector();
    featurestoreJdbcConnector.setName(featurestoreJdbcConnectorDTO.getName());
    featurestoreJdbcConnector.setDescription(featurestoreJdbcConnectorDTO.getDescription());
    featurestoreJdbcConnector.setFeaturestore(featurestore);
    featurestoreJdbcConnector.setArguments(featurestoreJdbcConnectorDTO.getArguments());
    featurestoreJdbcConnector.setConnectionString(featurestoreJdbcConnectorDTO.getConnectionString());
    featurestoreJdbcConnectorFacade.persist(featurestoreJdbcConnector);
    return new FeaturestoreJdbcConnectorDTO(featurestoreJdbcConnector);
  }

  /**
   * Updates a JDBC connection for the feature store
   *
   * @param featurestore the feature store
   * @param featurestoreJdbcConnectorDTO input to data to use when updating the storage connector
   * @param storageConnectorId the id of the connector
   * @return a DTO representing the updated entity
   * @throws FeaturestoreException
   */
  public FeaturestoreJdbcConnectorDTO updateFeaturestoreJdbcConnector(
      Featurestore featurestore, FeaturestoreJdbcConnectorDTO featurestoreJdbcConnectorDTO,
      Integer storageConnectorId) throws FeaturestoreException {

    FeaturestoreJdbcConnector featurestoreJdbcConnector = verifyJdbcConnectorId(storageConnectorId, featurestore);

    if(!Strings.isNullOrEmpty(featurestoreJdbcConnectorDTO.getName())) {
      verifyJdbcConnectorName(featurestoreJdbcConnectorDTO.getName(), featurestore, true);
      featurestoreJdbcConnector.setName(featurestoreJdbcConnectorDTO.getName());
    }

    if(!Strings.isNullOrEmpty(featurestoreJdbcConnectorDTO.getDescription())) {
      verifyJdbcConnectorDescription(featurestoreJdbcConnectorDTO.getDescription());
      featurestoreJdbcConnector.setDescription(featurestoreJdbcConnectorDTO.getDescription());
    }

    if(!Strings.isNullOrEmpty(featurestoreJdbcConnectorDTO.getConnectionString())) {
      verifyJdbcConnectorConnectionString(featurestoreJdbcConnectorDTO.getConnectionString());
      featurestoreJdbcConnector.setConnectionString(featurestoreJdbcConnectorDTO.getConnectionString());
    }

    if(!Strings.isNullOrEmpty(featurestoreJdbcConnectorDTO.getArguments())) {
      verifyJdbcConnectorArguments(featurestoreJdbcConnectorDTO.getArguments());
      featurestoreJdbcConnector.setArguments(featurestoreJdbcConnectorDTO.getArguments());
    }

    if(featurestore != null) {
      featurestoreJdbcConnector.setFeaturestore(featurestore);
    }

    FeaturestoreJdbcConnector updatedConnector =
        featurestoreJdbcConnectorFacade.updateJdbcConnector(featurestoreJdbcConnector);
    return new FeaturestoreJdbcConnectorDTO(updatedConnector);
  }
  
  /**
   * Utility function for creating default JDBC connectors for a Featurestore-project in Hopsworks
   * for a featurestore
   *
   * @param featurestore the featurestore
   * @param databaseName name of the Hive database
   */
  public void createDefaultJdbcConnectorForFeaturestore(Featurestore featurestore, String databaseName,
    String description) {
    String hiveEndpoint = settings.getHiveServerHostName(false);
    String connectionString = "jdbc:hive2://" + hiveEndpoint + "/" + databaseName + ";" +
      "auth=noSasl;ssl=true;twoWay=true;";
    String arguments = "sslTrustStore,trustStorePassword,sslKeyStore,keyStorePassword";
    String name = databaseName;
    FeaturestoreJdbcConnectorDTO featurestoreJdbcConnectorDTO = new FeaturestoreJdbcConnectorDTO();
    featurestoreJdbcConnectorDTO.setName(name);
    featurestoreJdbcConnectorDTO.setDescription(description);
    featurestoreJdbcConnectorDTO.setConnectionString(connectionString);
    featurestoreJdbcConnectorDTO.setArguments(arguments);
    createFeaturestoreJdbcConnector(featurestore,featurestoreJdbcConnectorDTO);
  }
  
  /**
   * Removes a JDBC connection from the database with a particular id
   *
   * @param featurestoreJdbcId id of the JDBC connection
   * @return DTO of the deleted entity
   */
  public FeaturestoreJdbcConnectorDTO removeFeaturestoreJdbcConnector(Integer featurestoreJdbcId){
    FeaturestoreJdbcConnector featurestoreJdbcConnector =
      featurestoreJdbcConnectorFacade.find(featurestoreJdbcId);
    FeaturestoreJdbcConnectorDTO featurestoreJdbcConnectorDTO =
      new FeaturestoreJdbcConnectorDTO(featurestoreJdbcConnector);
    featurestoreJdbcConnectorFacade.remove(featurestoreJdbcConnector);
    return featurestoreJdbcConnectorDTO;
  }

  /**
   * Verifies a storage connector id
   *
   * @param jdbcConnectorId the id to verify
   * @param featurestore the featurestore to query
   * @return the connector with the given id
   * @throws FeaturestoreException
   */
  private FeaturestoreJdbcConnector verifyJdbcConnectorId(
      Integer jdbcConnectorId, Featurestore featurestore) throws FeaturestoreException {
    FeaturestoreJdbcConnector featurestoreJdbcConnector =
        featurestoreJdbcConnectorFacade.findByIdAndFeaturestore(jdbcConnectorId, featurestore);
    if (featurestoreJdbcConnector == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.JDBC_CONNECTOR_NOT_FOUND,
          Level.FINE, "jdbcConnectorId: " + jdbcConnectorId);
    }
    return featurestoreJdbcConnector;
  }

  /**
   * Verifies user input connector name string
   *
   * @param name the user input to validate
   * @param featurestore the featurestore to query
   * @param edit boolean flag whether the validation if for updating an existing connector or creating a new one
   */
  private void verifyJdbcConnectorName(String name, Featurestore featurestore, Boolean edit){
    if (Strings.isNullOrEmpty(name)) {
      throw new IllegalArgumentException(
          RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_NAME.getMessage() +
              ", the storage connector name cannot be empty");
    }
    if(name.length() >
        FeaturestoreClientSettingsDTO.STORAGE_CONNECTOR_NAME_MAX_LENGTH) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_NAME.getMessage()
          + ", the name should be less than " + FeaturestoreClientSettingsDTO.STORAGE_CONNECTOR_NAME_MAX_LENGTH
          + " characters, the provided name was: " + name);
    }
    if(!edit){
      if(featurestore.getFeaturestoreJdbcConnectorConnections().stream()
          .anyMatch(jdbcCon -> jdbcCon.getName().equalsIgnoreCase(name))) {
        throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_NAME.getMessage() +
            ", the storage connector name should be unique, there already exists a JDBC connector with the same name ");
      }
    }
  }

  /**
   * Verifies user input featurestore to query
   *
   * @param featurestore the user input to validate
   */
  private void verifyFeaturestoreInput(Featurestore featurestore){
    if (featurestore == null) {
      throw new IllegalArgumentException("Featurestore was not found");
    }
  }

  /**
   * Verifies user input description
   *
   * @param description the user input to validate
   */
  private void verifyJdbcConnectorDescription(String description) {
    if(description.length() >
        FeaturestoreClientSettingsDTO.STORAGE_CONNECTOR_DESCRIPTION_MAX_LENGTH){
      throw new IllegalArgumentException(
          RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_DESCRIPTION.getMessage() +
              ", the description should be less than: " +
              FeaturestoreClientSettingsDTO.STORAGE_CONNECTOR_DESCRIPTION_MAX_LENGTH);
    }
  }

  /**
   * Verifies user input JDBC connection string
   *
   * @param connectionString the user input to validate
   */
  private void verifyJdbcConnectorConnectionString(String connectionString) {
    if(Strings.isNullOrEmpty(connectionString)
        || connectionString.length()
        > FeaturestoreClientSettingsDTO.JDBC_STORAGE_CONNECTOR_CONNECTIONSTRING_MAX_LENGTH) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_JDBC_CONNECTION_STRING.getMessage() +
          ", the JDBC connection string should not be empty and not exceed: " +
          FeaturestoreClientSettingsDTO.JDBC_STORAGE_CONNECTOR_CONNECTIONSTRING_MAX_LENGTH + " characters");
    }
  }

  /**
   * Verifies user input JDBC arguments
   *
   * @param arguments the user input to validate
   */
  private void verifyJdbcConnectorArguments(String arguments) {
    if(!Strings.isNullOrEmpty(arguments)
        && arguments.length() >
        FeaturestoreClientSettingsDTO.JDBC_STORAGE_CONNECTOR_ARGUMENTS_MAX_LENGTH) {
      throw new IllegalArgumentException(
          RESTCodes.FeaturestoreErrorCode.ILLEGAL_JDBC_CONNECTION_ARGUMENTS.getMessage() +
              ", the JDBC connection arguments should not exceed: " +
              FeaturestoreClientSettingsDTO.JDBC_STORAGE_CONNECTOR_ARGUMENTS_MAX_LENGTH + " characters");
    }
  }
  
  /**
   * Validates user input for creating a new JDBC connector in a featurestore
   *
   * @param featurestore the featurestore
   * @param featurestoreJdbcConnectorDTO input to data to use when creating the storage connector
   */
  private void verifyUserInput(Featurestore featurestore, FeaturestoreJdbcConnectorDTO featurestoreJdbcConnectorDTO){
    if(featurestoreJdbcConnectorDTO == null){
      throw new IllegalArgumentException("Input data is null");
    }
    verifyFeaturestoreInput(featurestore);
    verifyJdbcConnectorName(featurestoreJdbcConnectorDTO.getName(), featurestore, false);
    verifyJdbcConnectorDescription(featurestoreJdbcConnectorDTO.getDescription());
    verifyJdbcConnectorConnectionString(featurestoreJdbcConnectorDTO.getConnectionString());
    verifyJdbcConnectorArguments(featurestoreJdbcConnectorDTO.getArguments());
  }

  /**
   * Gets all JDBC connectors for a particular featurestore and project
   *
   * @param featurestore featurestore to query for jdbc connectors
   * @return list of XML/JSON DTOs of the jdbc connectors
   */
  public List<FeaturestoreStorageConnectorDTO> getJdbcConnectorsForFeaturestore(Featurestore featurestore) {
    List<FeaturestoreJdbcConnector> jdbcConnectors = featurestoreJdbcConnectorFacade.findByFeaturestore(featurestore);
    return jdbcConnectors.stream().map(jdbcConnector -> new FeaturestoreJdbcConnectorDTO(jdbcConnector))
        .collect(Collectors.toList());
  }

  /**
   * Retrieves a JDBC Connector with a particular id from a particular featurestore
   *
   * @param id           id of the jdbc connector
   * @param featurestore the featurestore that the connector belongs to
   * @return XML/JSON DTO of the JDBC Connector
   */
  public FeaturestoreJdbcConnectorDTO getJdbcConnectorWithIdAndFeaturestore(Featurestore featurestore, Integer id)
      throws FeaturestoreException {
    FeaturestoreJdbcConnector featurestoreJdbcConnector = verifyJdbcConnectorId(id, featurestore);
    return new FeaturestoreJdbcConnectorDTO(featurestoreJdbcConnector);
  }

}
