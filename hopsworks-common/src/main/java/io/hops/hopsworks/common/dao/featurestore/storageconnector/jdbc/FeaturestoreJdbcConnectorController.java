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
import io.hops.hopsworks.common.dao.featurestore.storageconnector.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;
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
   * Creates a JDBC connection for the Featurestore-Hive Database in Hopsworks for a featurestore
   *
   * @param featurestore the featurestore
   * @param databaseName name of the Hive database
   */
  public void createJdbcConnectorForFeaturestore(Featurestore featurestore, String databaseName) {
    String hiveEndpoint = settings.getHiveServerHostName(false);
    String connectionString = "jdbc:hive2://" + hiveEndpoint + "/" + databaseName + ";" +
      "auth=noSasl;ssl=true;twoWay=true;";
    String arguments = "sslTrustStore,trustStorePassword,sslKeyStore,keyStorePassword";
    String name = databaseName + ".jdbc";
    String description = "JDBC connection to Hopsworks Project Feature Store Hive Database";
    FeaturestoreJdbcConnectorDTO featurestoreJdbcConnectorDTO = new FeaturestoreJdbcConnectorDTO();
    featurestoreJdbcConnectorDTO.setName(name);
    featurestoreJdbcConnectorDTO.setDescription(description);
    featurestoreJdbcConnectorDTO.setConnectionString(connectionString);
    featurestoreJdbcConnectorDTO.setArguments(arguments);
    createFeaturestoreJdbcConnector(featurestore,featurestoreJdbcConnectorDTO);
  }
  
  /**
   * Creates a JDBC connection for the Hive-Warehouse Database in Hopsworks
   *
   * @param featurestore the featurestore
   * @param databaseName name of the Hive database
   */
  public void createJdbcConnectorForHiveWarehouse(Featurestore featurestore, String databaseName) {
    String hiveEndpoint = settings.getHiveServerHostName(false);
    String connectionString = "jdbc:hive2://" + hiveEndpoint + "/" + databaseName + ";" +
      "auth=noSasl;ssl=true;twoWay=true;";
    String arguments = "sslTrustStore,trustStorePassword,sslKeyStore,keyStorePassword";
    String name = databaseName + ".jdbc";
    String description = "JDBC connection to Hopsworks Project Hive Warehouse";
    FeaturestoreJdbcConnectorDTO featurestoreJdbcConnectorDTO = new FeaturestoreJdbcConnectorDTO();
    featurestoreJdbcConnectorDTO.setName(name);
    featurestoreJdbcConnectorDTO.setDescription(description);
    featurestoreJdbcConnectorDTO.setConnectionString(connectionString);
    featurestoreJdbcConnectorDTO.setArguments(arguments);
    createFeaturestoreJdbcConnector(featurestore, featurestoreJdbcConnectorDTO);
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
   * Validates user input for creating a new JDBC connector in a featurestore
   *
   * @param featurestore the featurestore
   * @param featurestoreJdbcConnectorDTO input to data to use when creating the storage connector
   */
  public void verifyUserInput(Featurestore featurestore, FeaturestoreJdbcConnectorDTO featurestoreJdbcConnectorDTO){
    if (featurestore == null) {
      throw new IllegalArgumentException("Featurestore was not found");
    }

    if(featurestoreJdbcConnectorDTO == null){
      throw new IllegalArgumentException("Input data is null");
    }
    
    if (Strings.isNullOrEmpty(featurestoreJdbcConnectorDTO.getName())) {
      throw new IllegalArgumentException(
        RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_NAME.getMessage() + ", the storage connector name " +
          "cannot be empty");
    }
  
    Pattern namePattern = Pattern.compile(Settings.HOPS_FEATURESTORE_REGEX);
  
    if(featurestoreJdbcConnectorDTO.getName().length() > Settings.HOPS_STORAGE_CONNECTOR_NAME_MAX_LENGTH ||
        !namePattern.matcher(featurestoreJdbcConnectorDTO.getName()).matches()) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_NAME.getMessage()
      + ", the name should be less than " + Settings.HOPS_STORAGE_CONNECTOR_NAME_MAX_LENGTH + " characters and match " +
        "the regular expression: " +  Settings.HOPS_FEATURESTORE_REGEX);
    }
  
    if(featurestore.getFeaturestoreJdbcConnectorConnections().stream()
      .anyMatch(jdbcCon -> jdbcCon.getName().equalsIgnoreCase(featurestoreJdbcConnectorDTO.getName()))) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_NAME.getMessage() +
        ", the storage connector name should be unique, there already exists a JDBC connector with the same name ");
    }
    
    if(featurestoreJdbcConnectorDTO.getDescription().length() > Settings.HOPS_STORAGE_CONNECTOR_DESCRIPTION_MAX_LENGTH){
      throw new IllegalArgumentException(
        RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_DESCRIPTION.getMessage() +
        ", the description should be less than: " + Settings.HOPS_STORAGE_CONNECTOR_DESCRIPTION_MAX_LENGTH);
    }
    
    if(Strings.isNullOrEmpty(featurestoreJdbcConnectorDTO.getConnectionString())
      || featurestoreJdbcConnectorDTO.getConnectionString().length()
        > Settings.HOPS_JDBC_STORAGE_CONNECTOR_CONNECTIONSTRING_MAX_LENGTH) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_JDBC_CONNECTION_STRING.getMessage() +
        ", the JDBC connection string should not be empty and not exceed: " +
        Settings.HOPS_JDBC_STORAGE_CONNECTOR_CONNECTIONSTRING_MAX_LENGTH + " characters");
    }
  
    if(!Strings.isNullOrEmpty(featurestoreJdbcConnectorDTO.getArguments())
        && featurestoreJdbcConnectorDTO.getArguments().length() >
      Settings.HOPS_JDBC_STORAGE_CONNECTOR_ARGUMENTS_MAX_LENGTH) {
      throw new IllegalArgumentException(
        RESTCodes.FeaturestoreErrorCode.ILLEGAL_JDBC_CONNECTION_ARGUMENTS.getMessage() +
        ", the JDBC connection arguments should not exceed: " +
        Settings.HOPS_JDBC_STORAGE_CONNECTOR_ARGUMENTS_MAX_LENGTH + " characters");
    }
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
    FeaturestoreJdbcConnector featurestoreJdbcConnector =
        featurestoreJdbcConnectorFacade.findByIdAndFeaturestore(id, featurestore);
    if (featurestoreJdbcConnector == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.JDBC_CONNECTOR_NOT_FOUND,
          Level.FINE, "jdbcConnectorId: " + id);
    }
    return new FeaturestoreJdbcConnectorDTO(featurestoreJdbcConnector);
  }

}
