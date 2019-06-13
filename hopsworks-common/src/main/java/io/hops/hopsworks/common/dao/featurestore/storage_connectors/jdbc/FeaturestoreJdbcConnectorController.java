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

package io.hops.hopsworks.common.dao.featurestore.storage_connectors.jdbc;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.featurestore.Featurestore;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.regex.Pattern;

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
   * @param name name of the connection
   * @param description description of the connection
   * @param featurestore the feature store
   * @param arguments name of missing arguments for the JDBC string (e.g passwords and similar that should not be
   *                  stored plaintext in the db)
   * @param connectionString the JDBC connection string (arguments will be appended at runtime)
   * @return a DTO representing the created entity
   */
  public FeaturestoreJdbcConnectorDTO createFeaturestoreJdbcConnector(String name, String description,
    Featurestore featurestore, String arguments, String connectionString){
    FeaturestoreJdbcConnector featurestoreJdbcConnector = new FeaturestoreJdbcConnector();
    featurestoreJdbcConnector.setName(name);
    featurestoreJdbcConnector.setDescription(description);
    featurestoreJdbcConnector.setFeaturestore(featurestore);
    featurestoreJdbcConnector.setArguments(arguments);
    featurestoreJdbcConnector.setConnectionString(connectionString);
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
    createFeaturestoreJdbcConnector(name, description, featurestore, arguments, connectionString);
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
    createFeaturestoreJdbcConnector(name, description, featurestore, arguments, connectionString);
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
   * @param name the name of the JDBC connector to create
   * @param description the description of the connector
   * @param featurestore the featurestore
   * @param arguments the JDBC connector arguments
   * @param connectionString the JDBC connection string
   */
  public void verifyUserInput(String name, String description,
    Featurestore featurestore, String arguments, String connectionString){
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
      + ", the name should be less than " + Settings.HOPS_STORAGE_CONNECTOR_NAME_MAX_LENGTH + " characters and match " +
        "the regular expression: " +  Settings.HOPS_FEATURESTORE_REGEX);
    }
  
    if(featurestore.getFeaturestoreJdbcConnectorConnections().stream()
      .anyMatch(jdbcCon -> jdbcCon.getName().equalsIgnoreCase(name))) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_NAME.getMessage() +
        ", the storage connector name should be unique, there already exists a JDBC connector with the same name ");
    }
    
    if(description.length() > Settings.HOPS_STORAGE_CONNECTOR_DESCRIPTION_MAX_LENGTH){
      throw new IllegalArgumentException(
        RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_DESCRIPTION.getMessage() +
        ", the description should be less than: " + Settings.HOPS_STORAGE_CONNECTOR_DESCRIPTION_MAX_LENGTH);
    }
    
    if(Strings.isNullOrEmpty(connectionString)
      || connectionString.length() > Settings.HOPS_JDBC_STORAGE_CONNECTOR_CONNECTIONSTRING_MAX_LENGTH) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_JDBC_CONNECTION_STRING.getMessage() +
        ", the JDBC connection string should not be empty and not exceed: " +
        Settings.HOPS_JDBC_STORAGE_CONNECTOR_CONNECTIONSTRING_MAX_LENGTH + " characters");
    }
  
    if(!Strings.isNullOrEmpty(arguments) && arguments.length() >
      Settings.HOPS_JDBC_STORAGE_CONNECTOR_ARGUMENTS_MAX_LENGTH) {
      throw new IllegalArgumentException(
        RESTCodes.FeaturestoreErrorCode.ILLEGAL_JDBC_CONNECTION_ARGUMENTS.getMessage() +
        ", the JDBC connection arguments should not exceed: " +
        Settings.HOPS_JDBC_STORAGE_CONNECTOR_ARGUMENTS_MAX_LENGTH + " characters");
    }
  }

}
