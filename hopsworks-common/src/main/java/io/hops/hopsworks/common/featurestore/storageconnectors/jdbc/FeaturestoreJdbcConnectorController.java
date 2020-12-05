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

package io.hops.hopsworks.common.featurestore.storageconnectors.jdbc;

import com.google.common.base.Strings;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.common.dao.user.security.secrets.SecretPlaintext;
import io.hops.hopsworks.common.featurestore.FeaturestoreConstants;
import io.hops.hopsworks.common.featurestore.online.OnlineFeaturestoreController;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorType;
import io.hops.hopsworks.common.hive.HiveController;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.common.security.secrets.SecretsController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.jdbc.FeaturestoreJdbcConnector;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class controlling the interaction with the feature_store_jdbc_connector table and required business logic
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeaturestoreJdbcConnectorController {
  
  @EJB
  private FeaturestoreJdbcConnectorFacade featurestoreJdbcConnectorFacade;
  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;
  @EJB
  private HiveController hiveController;
  @EJB
  private Settings settings;
  @EJB
  private SecretsController secretsController;
  @EJB
  private OnlineFeaturestoreController onlineFeaturestoreController;

  private static final Logger LOGGER = Logger.getLogger(FeaturestoreJdbcConnectorController.class.getName());
  
  /**
   * Persists a JDBC connection for the feature store
   *
   * @param featurestore the feature store
   * @param featurestoreJdbcConnectorDTO input to data to use when creating the storage connector
   * @return a DTO representing the created entity
   * @throws FeaturestoreException
   */
  public FeaturestoreJdbcConnectorDTO createFeaturestoreJdbcConnector(
      Featurestore featurestore, FeaturestoreJdbcConnectorDTO featurestoreJdbcConnectorDTO)
    throws FeaturestoreException {
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
   * @param storageConnectorName the name of the connector
   * @return a DTO representing the updated entity
   * @throws FeaturestoreException
   */
  public FeaturestoreJdbcConnectorDTO updateFeaturestoreJdbcConnector(
      Featurestore featurestore, FeaturestoreJdbcConnectorDTO featurestoreJdbcConnectorDTO,
      String storageConnectorName) throws FeaturestoreException {

    FeaturestoreJdbcConnector featurestoreJdbcConnector = verifyJdbcConnectorName(storageConnectorName, featurestore);

    if(!Strings.isNullOrEmpty(featurestoreJdbcConnectorDTO.getName())) {
      verifyJdbcConnectorName(featurestoreJdbcConnectorDTO, featurestore, true);
      featurestoreJdbcConnector.setName(featurestoreJdbcConnectorDTO.getName());
    }

    if(!Strings.isNullOrEmpty(featurestoreJdbcConnectorDTO.getDescription())) {
      featurestoreJdbcConnectorDTO.verifyDescription();
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
   * Utility function for creating default JDBC connector for the offline Featurestore-project in Hopsworks
   *
   * @param featurestore the featurestore
   * @param databaseName name of the Hive database
   * @throws FeaturestoreException
   */
  public void createDefaultJdbcConnectorForOfflineFeaturestore(Featurestore featurestore, String databaseName,
    String description) throws FeaturestoreException {
    try {
      String hiveEndpoint = hiveController.getHiveServerInternalEndpoint();
      String connectionString = "jdbc:hive2://" + hiveEndpoint + "/" + databaseName + ";" +
          "auth=noSasl;ssl=true;twoWay=true;";
      String arguments = "sslTrustStore,trustStorePassword,sslKeyStore,keyStorePassword";
      FeaturestoreJdbcConnectorDTO featurestoreJdbcConnectorDTO = new FeaturestoreJdbcConnectorDTO();
      featurestoreJdbcConnectorDTO.setName(databaseName);
      featurestoreJdbcConnectorDTO.setDescription(description);
      featurestoreJdbcConnectorDTO.setConnectionString(connectionString);
      featurestoreJdbcConnectorDTO.setArguments(arguments);
      createFeaturestoreJdbcConnector(featurestore, featurestoreJdbcConnectorDTO);
    } catch (ServiceDiscoveryException ex) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.JDBC_CONNECTOR_NOT_FOUND,
          Level.SEVERE, "Could not create Hive connection string",
          ex.getMessage(), ex);
    }
  }
  
  /**
   * Removes a JDBC connection from the database with a particular id
   *
   * @param featurestoreJdbcId id of the JDBC connection
   * @return DTO of the deleted entity
   */
  public void removeFeaturestoreJdbcConnector(Integer featurestoreJdbcId){
    FeaturestoreJdbcConnector featurestoreJdbcConnector = featurestoreJdbcConnectorFacade.find(featurestoreJdbcId);
    if (featurestoreJdbcConnector != null) {
      featurestoreJdbcConnectorFacade.remove(featurestoreJdbcConnector);
    }
  }

  public void removeFeaturestoreJdbcConnector(String featurestoreJdbcName, Featurestore featurestore){
    Optional<FeaturestoreJdbcConnector> featurestoreJdbcConnector =
        featurestoreJdbcConnectorFacade.findByNameAndFeaturestore(featurestoreJdbcName, featurestore);
    featurestoreJdbcConnector.ifPresent(jdbcConnector -> featurestoreJdbcConnectorFacade.remove(jdbcConnector));
  }
  
  private FeaturestoreJdbcConnector verifyJdbcConnectorName(String jdbcConnectorName, Featurestore featurestore)
    throws FeaturestoreException {
    return featurestoreJdbcConnectorFacade.findByNameAndFeaturestore(jdbcConnectorName, featurestore)
      .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.JDBC_CONNECTOR_NOT_FOUND,
        Level.FINE, "jdbcConnector name: " + jdbcConnectorName));
  }

  /**
   * Verifies user input connector name string
   *
   * @param featurestoreJdbcConnectorDTO the user input to validate
   * @param featurestore the featurestore to query
   * @param edit boolean flag whether the validation if for updating an existing connector or creating a new one
   * @throws FeaturestoreException
   */
  private void verifyJdbcConnectorName(FeaturestoreJdbcConnectorDTO featurestoreJdbcConnectorDTO,
    Featurestore featurestore, Boolean edit) throws FeaturestoreException {
    featurestoreJdbcConnectorDTO.verifyName();
    if (!edit) {
      if (featurestoreJdbcConnectorFacade
        .findByNameAndFeaturestore(featurestoreJdbcConnectorDTO.getName(), featurestore).isPresent()) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_NAME, Level.FINE,
          ", the storage connector name should be unique, there already exists a JDBC connector with the same name ");
      }
    }
  }

  /**
   * Verifies user input JDBC connection string
   *
   * @param connectionString the user input to validate
   * @throws FeaturestoreException
   */
  private void verifyJdbcConnectorConnectionString(String connectionString) throws FeaturestoreException {
    if(Strings.isNullOrEmpty(connectionString)
        || connectionString.length()
        > FeaturestoreConstants.JDBC_STORAGE_CONNECTOR_CONNECTIONSTRING_MAX_LENGTH) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_JDBC_CONNECTION_STRING, Level.FINE,
          ", the JDBC connection string should not be empty and not exceed: " +
            FeaturestoreConstants.JDBC_STORAGE_CONNECTOR_CONNECTIONSTRING_MAX_LENGTH + " characters");
    }
  }

  /**
   * Verifies user input JDBC arguments
   *
   * @param arguments the user input to validate
   * @throws FeaturestoreException
   */
  private void verifyJdbcConnectorArguments(String arguments) throws FeaturestoreException {
    if (!Strings.isNullOrEmpty(arguments)
      && arguments.length() > FeaturestoreConstants.JDBC_STORAGE_CONNECTOR_ARGUMENTS_MAX_LENGTH) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_JDBC_CONNECTION_ARGUMENTS, Level.FINE,
        "Illegal JDBC Connection Arguments, the JDBC connection arguments should not exceed: " +
          FeaturestoreConstants.JDBC_STORAGE_CONNECTOR_ARGUMENTS_MAX_LENGTH + " characters");
    }
  }
  
  /**
   * Validates user input for creating a new JDBC connector in a featurestore
   *
   * @param featurestore the featurestore
   * @param featurestoreJdbcConnectorDTO input to data to use when creating the storage connector
   * @throws FeaturestoreException
   */
  private void verifyUserInput(Featurestore featurestore, FeaturestoreJdbcConnectorDTO featurestoreJdbcConnectorDTO)
    throws FeaturestoreException {
    if(featurestoreJdbcConnectorDTO == null){
      throw new IllegalArgumentException("Input data is null");
    }
    verifyJdbcConnectorName(featurestoreJdbcConnectorDTO, featurestore, false);
    featurestoreJdbcConnectorDTO.verifyDescription();
    verifyJdbcConnectorConnectionString(featurestoreJdbcConnectorDTO.getConnectionString());
    verifyJdbcConnectorArguments(featurestoreJdbcConnectorDTO.getArguments());
  }

  /**
   * Gets all JDBC connectors for a particular featurestore and project
   *
   * @param user
   * @param featurestore featurestore to query for jdbc connectors
   * @return list of XML/JSON DTOs of the jdbc connectors
   */
  public List<FeaturestoreStorageConnectorDTO> getJdbcConnectorsForFeaturestore(Users user, Featurestore featurestore)
      throws FeaturestoreException {
    List<FeaturestoreStorageConnectorDTO> jdbcConnectorDTOs = new ArrayList<>();
    List<FeaturestoreJdbcConnector> jdbcConnectors = featurestoreJdbcConnectorFacade.findByFeaturestore(featurestore);

    for (FeaturestoreJdbcConnector jdbcConnector : jdbcConnectors) {
      FeaturestoreJdbcConnectorDTO jdbcConnectorDTO =
          replaceOnlineFsConnectorUrl(new FeaturestoreJdbcConnectorDTO(jdbcConnector));

      String userOnlineConnectorName = onlineFeaturestoreController.onlineDbUsername(featurestore.getProject(), user)
              + FeaturestoreConstants.ONLINE_FEATURE_STORE_CONNECTOR_SUFFIX;
      if (jdbcConnectorDTO.getName().equals(userOnlineConnectorName)) {
        setPasswordPlainTextForOnlineJdbcConnector(user, jdbcConnectorDTO, featurestore.getProject().getName());
      }

      jdbcConnectorDTOs.add(jdbcConnectorDTO);
    }

    return jdbcConnectorDTOs;
  }
  
  private FeaturestoreJdbcConnectorDTO getJdbcConnectorDTO(Users user, Featurestore featurestore,
    FeaturestoreJdbcConnector featurestoreJdbcConnector) throws FeaturestoreException {
    FeaturestoreJdbcConnectorDTO featurestoreJdbcConnectorDTO =
      new FeaturestoreJdbcConnectorDTO(featurestoreJdbcConnector);
    if(featurestoreJdbcConnectorDTO.getName().equals(onlineFeaturestoreController.onlineDbUsername(
      featurestore.getProject(), user) + FeaturestoreConstants.ONLINE_FEATURE_STORE_CONNECTOR_SUFFIX)) {
      setPasswordPlainTextForOnlineJdbcConnector(user, featurestoreJdbcConnectorDTO,
        featurestore.getProject().getName());
    }
    return replaceOnlineFsConnectorUrl(new FeaturestoreJdbcConnectorDTO(featurestoreJdbcConnector));
  }
  
  /**
   * Utility function for create a JDBC connection to the online featurestore for a particular user.
   *
   * @param onlineDbUsername the db-username of the connection
   * @param featurestore the featurestore metadata
   * @param dbName name of the MySQL database
   * @return DTO of the newly created connector
   * @throws FeaturestoreException
   */
  public FeaturestoreJdbcConnectorDTO createJdbcConnectorForOnlineFeaturestore(String onlineDbUsername,
    Featurestore featurestore, String dbName) throws FeaturestoreException {
    String connectorName = onlineDbUsername + FeaturestoreConstants.ONLINE_FEATURE_STORE_CONNECTOR_SUFFIX;
    if (featurestoreJdbcConnectorFacade.findByNameAndFeaturestore(connectorName, featurestore).isPresent()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_NAME, Level.FINE,
          "Illegal storage connector name, a storage connector with that name already exists");
    }

    FeaturestoreJdbcConnectorDTO featurestoreJdbcConnectorDTO = new FeaturestoreJdbcConnectorDTO();
    featurestoreJdbcConnectorDTO
        .setConnectionString(settings.getFeaturestoreJdbcUrl() + dbName + "?useSSL=false&allowPublicKeyRetrieval=true");
    featurestoreJdbcConnectorDTO.setDescription("JDBC connection to Hopsworks Project Online " +
      "Feature Store NDB Database for user: " + onlineDbUsername);
    featurestoreJdbcConnectorDTO.setArguments(
        FeaturestoreConstants.ONLINE_FEATURE_STORE_JDBC_PASSWORD_ARG + "=" +
            FeaturestoreConstants.ONLINE_FEATURE_STORE_CONNECTOR_PASSWORD_TEMPLATE + "," +
            FeaturestoreConstants.ONLINE_FEATURE_STORE_JDBC_USER_ARG + "=" + onlineDbUsername +
            ",isolationLevel=NONE,batchsize=500");
    featurestoreJdbcConnectorDTO.setStorageConnectorType(FeaturestoreStorageConnectorType.JDBC);
    featurestoreJdbcConnectorDTO.setName(connectorName);
    featurestoreJdbcConnectorDTO.setFeaturestoreId(featurestore.getId());
    return createFeaturestoreJdbcConnector(featurestore, featurestoreJdbcConnectorDTO);
  }
  
  /**
   * Gets the plain text password for the connector from the secret
   * @param user
   * @param projectName
   * @return
   */
  private String getConnectorPlainPasswordFromSecret(Users user, String projectName){
    String secretName = projectName.concat("_").concat(user.getUsername());
    try {
      SecretPlaintext plaintext = secretsController.get(user, secretName);
      return plaintext.getPlaintext();
    } catch (UserException e) {
      LOGGER.log(Level.SEVERE, "Could not get the online jdbc connector password for project: " +
        projectName + ", " + "user: " + user.getEmail());
      return null;
    }
  }
  
  /**
   * Set the password in argument to plain text only if the connector is an online feature store connector
   * @param user
   * @param featurestoreJdbcConnectorDTO
   * @param projectName
   * @returnsetPasswordInArgumentToPlainText
   */
  private void setPasswordPlainTextForOnlineJdbcConnector(Users user,
                                                          FeaturestoreJdbcConnectorDTO featurestoreJdbcConnectorDTO,
                                                          String projectName) {
    String connectorPassword = getConnectorPlainPasswordFromSecret(user, projectName);
    if(!Strings.isNullOrEmpty(featurestoreJdbcConnectorDTO.getArguments())
      && !Strings.isNullOrEmpty(connectorPassword)) {
      featurestoreJdbcConnectorDTO.setArguments(featurestoreJdbcConnectorDTO.getArguments()
        .replaceFirst(FeaturestoreConstants.ONLINE_FEATURE_STORE_CONNECTOR_PASSWORD_TEMPLATE, connectorPassword));
    }
  }

  // In the database we store the consul name for mysql, so that we can handle backups and taking AMIs.
  // When serving to the user, we resolve the name with ANY so that both internal and external users can
  // work with it.
  private FeaturestoreJdbcConnectorDTO replaceOnlineFsConnectorUrl(FeaturestoreJdbcConnectorDTO jdbcConnectorDTO)
      throws FeaturestoreException {

    String connectionString = "";
    try {
      connectionString = jdbcConnectorDTO.getConnectionString().replace(
          serviceDiscoveryController.constructServiceFQDN(ServiceDiscoveryController.HopsworksService.ONLINEFS_MYSQL),
          serviceDiscoveryController.getAnyAddressOfServiceWithDNS(
              ServiceDiscoveryController.HopsworksService.ONLINEFS_MYSQL).getAddress());
    } catch (ServiceDiscoveryException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.STORAGE_CONNECTOR_GET_ERROR, Level.SEVERE,
          "Error resolving MySQL DNS name", e.getMessage(), e);
    }
    jdbcConnectorDTO.setConnectionString(connectionString);
    return jdbcConnectorDTO;
  }
  
  /**
   *
   * @param user
   * @param featurestore
   * @param storageConnectorName
   * @return
   * @throws FeaturestoreException
   */
  public FeaturestoreStorageConnectorDTO getJdbcConnectorWithNameAndFeaturestore(Users user, Featurestore featurestore,
    String storageConnectorName) throws FeaturestoreException {
    FeaturestoreJdbcConnector featurestoreJdbcConnector = verifyJdbcConnectorName(storageConnectorName, featurestore);
    return getJdbcConnectorDTO(user, featurestore, featurestoreJdbcConnector);
  }
}
