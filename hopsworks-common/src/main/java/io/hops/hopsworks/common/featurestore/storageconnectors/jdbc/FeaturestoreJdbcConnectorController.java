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
import io.hops.hopsworks.common.dao.user.security.secrets.SecretsFacade;
import io.hops.hopsworks.common.featurestore.FeaturestoreConstants;
import io.hops.hopsworks.common.featurestore.OptionDTO;
import io.hops.hopsworks.common.featurestore.online.OnlineFeaturestoreController;
import io.hops.hopsworks.common.featurestore.storageconnectors.StorageConnectorUtil;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.common.security.secrets.SecretsController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.jdbc.FeaturestoreJdbcConnector;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.secrets.Secret;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.servicediscovery.HopsworksService;
import io.hops.hopsworks.servicediscovery.tags.HiveTags;
import io.hops.hopsworks.servicediscovery.tags.MysqlTags;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.Transactional;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class controlling the interaction with the feature_store_jdbc_connector table and required business logic
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeaturestoreJdbcConnectorController {
  
  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;
  @EJB
  private SecretsController secretsController;
  @EJB
  private OnlineFeaturestoreController onlineFeaturestoreController;
  @EJB
  private StorageConnectorUtil storageConnectorUtil;
  @EJB
  private SecretsFacade secretsFacade;

  private static final Logger LOGGER = Logger.getLogger(FeaturestoreJdbcConnectorController.class.getName());

  public FeaturestoreJdbcConnector createFeaturestoreJdbcConnector(
      Users user, Featurestore featurestore, FeaturestoreJdbcConnectorDTO featurestoreJdbcConnectorDTO)
      throws FeaturestoreException, ProjectException, UserException {
    return createOrUpdateJdbcConnector(user, featurestore, featurestoreJdbcConnectorDTO,
      new FeaturestoreJdbcConnector());
  }
  
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  @Transactional(rollbackOn = FeaturestoreException.class)
  public FeaturestoreJdbcConnector updateFeaturestoreJdbcConnector(
    Users user, Featurestore featurestore, FeaturestoreJdbcConnectorDTO featurestoreJdbcConnectorDTO,
      FeaturestoreJdbcConnector featurestoreJdbcConnector) throws FeaturestoreException, ProjectException,
    UserException {
    return createOrUpdateJdbcConnector(user, featurestore, featurestoreJdbcConnectorDTO, featurestoreJdbcConnector);
  }

  /**
   * Verifies user input JDBC connection string
   *
   * @param connectionString the user input to validate
   * @throws FeaturestoreException
   */
  private void verifyJdbcConnectorConnectionString(String connectionString) throws FeaturestoreException {
    if(Strings.isNullOrEmpty(connectionString) ||
        connectionString.length() > FeaturestoreConstants.JDBC_STORAGE_CONNECTOR_CONNECTIONSTRING_MAX_LENGTH) {
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
    if(!Strings.isNullOrEmpty(arguments)
        && arguments.length() > FeaturestoreConstants.STORAGE_CONNECTOR_ARGUMENTS_MAX_LENGTH) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_JDBC_CONNECTION_ARGUMENTS, Level.FINE,
              "JDBC connection arguments should not exceed: " +
                  FeaturestoreConstants.STORAGE_CONNECTOR_ARGUMENTS_MAX_LENGTH + " characters");
    }
  }

  public FeaturestoreJdbcConnectorDTO getJdbcConnectorDTO(Users user, Project project,
      FeaturestoreConnector featurestoreConnector)
    throws FeaturestoreException {
    FeaturestoreJdbcConnectorDTO featurestoreJdbcConnectorDTO = new FeaturestoreJdbcConnectorDTO(featurestoreConnector);
    Secret secret = featurestoreConnector.getJdbcConnector().getPasswordSecret();
    SecretPlaintext plaintSecret = null;
    if (secret != null) {
      try {
        plaintSecret = secretsController.get(user, secret.getId().getName());
      } catch (UserException e) {
        LOGGER.log(Level.SEVERE,
          String.format("Could not get plain secret for jdbc connector password for project: %s, user: %s",
            project.getName(), user.getEmail()), e);
      }
    }
    
    featurestoreJdbcConnectorDTO.setArguments(
      storageConnectorUtil.toOptions(featurestoreConnector.getJdbcConnector().getArguments()));
    // replace password template with plain text password
    if (featurestoreJdbcConnectorDTO.getName()
      .equals(onlineFeaturestoreController.onlineDbUsername(project, user)
        + FeaturestoreConstants.ONLINE_FEATURE_STORE_CONNECTOR_SUFFIX)) {
      setPasswordPlainTextForOnlineJdbcConnector(user, featurestoreJdbcConnectorDTO, project);
    } else if (plaintSecret != null) {
      storageConnectorUtil.replaceToPlainText(featurestoreJdbcConnectorDTO.getArguments(),
        plaintSecret.getPlaintext());
      featurestoreJdbcConnectorDTO.setConnectionString(
        storageConnectorUtil.replaceToPlainText(featurestoreJdbcConnectorDTO.getConnectionString(),
          plaintSecret.getPlaintext())
      );
    }
    replaceOnlineFsConnectorUrl(featurestoreJdbcConnectorDTO);
    replaceOfflineFsConnectorUrl(featurestoreJdbcConnectorDTO);
    featurestoreJdbcConnectorDTO.setDriverPath(featurestoreConnector.getJdbcConnector().getDriverPath());

    return featurestoreJdbcConnectorDTO;
  }

  /**
   * Gets the plain text password for the connector from the secret
   * @param user
   * @param project
   * @return
   */
  private String getConnectorPlainPasswordFromSecret(Users user, Project project) {
    String secretName = onlineFeaturestoreController.onlineDbUsername(project, user);
    try {
      SecretPlaintext plaintext = secretsController.get(user, secretName);
      return plaintext.getPlaintext();
    } catch (UserException e) {
      LOGGER.log(Level.SEVERE,
        String.format("Could not get the online jdbc connector password for project: %s, user: %s", project.getName(),
          user.getEmail()));
      return null;
    }
  }
  
  /**
   * Set the password in argument to plain text only if the connector is an online feature store connector
   * @param user
   * @param featurestoreJdbcConnectorDTO
   * @param project
   */
  private void setPasswordPlainTextForOnlineJdbcConnector(Users user,
                                                          FeaturestoreJdbcConnectorDTO featurestoreJdbcConnectorDTO,
                                                          Project project) {
    String connectorPassword = getConnectorPlainPasswordFromSecret(user, project);
    storageConnectorUtil.replaceToPlainText(featurestoreJdbcConnectorDTO.getArguments(), connectorPassword);
  }

  // In the database we store the consul name for mysql, so that we can handle backups and taking AMIs.
  // When serving to the user, we resolve the name with ANY so that both internal and external users can
  // work with it.
  private void replaceOnlineFsConnectorUrl(FeaturestoreJdbcConnectorDTO jdbcConnectorDTO)
      throws FeaturestoreException {
    String connectionString = "";
    try {
      connectionString = jdbcConnectorDTO.getConnectionString().replace(
          serviceDiscoveryController.constructServiceFQDN(
              HopsworksService.MYSQL.getNameWithTag(MysqlTags.onlinefs)),
          serviceDiscoveryController.getAnyAddressOfServiceWithDNS(
              HopsworksService.MYSQL.getNameWithTag(MysqlTags.onlinefs)).getAddress());
    } catch (ServiceDiscoveryException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.STORAGE_CONNECTOR_GET_ERROR, Level.SEVERE,
          "Error resolving MySQL DNS name", e.getMessage(), e);
    }
    jdbcConnectorDTO.setConnectionString(connectionString);
  }

  // In the database we store the consul name for Hive, so that we can handle backups and taking AMIs.
  // When serving to the user, we resolve the name with ANY so that both internal and external users can
  // work with it.
  private void replaceOfflineFsConnectorUrl(FeaturestoreJdbcConnectorDTO jdbcConnectorDTO)
      throws FeaturestoreException {
    String connectionString = "";
    try {
      connectionString = jdbcConnectorDTO.getConnectionString().replace(
          serviceDiscoveryController.constructServiceFQDN(
              HopsworksService.HIVE.getNameWithTag(HiveTags.hiveserver2_tls)),
          serviceDiscoveryController.getAnyAddressOfServiceWithDNS(
              HopsworksService.HIVE.getNameWithTag(HiveTags.hiveserver2_tls)).getAddress());
    } catch (ServiceDiscoveryException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.STORAGE_CONNECTOR_GET_ERROR, Level.SEVERE,
          "Error resolving Hive DNS name", e.getMessage(), e);
    }
    jdbcConnectorDTO.setConnectionString(connectionString);
  }
  
  /**
   * Update a JDBC connector
   *
   * @param user Users
   * @param featurestore  Featurestore
   * @param featurestoreJdbcConnectorDTO  FeaturestoreJdbcConnectorDTO
   * @param featurestoreJdbcConnector existing FeaturestoreJdbcConnector
   * @return FeaturestoreJdbcConnector
   * @throws FeaturestoreException
   * @throws ProjectException
   * @throws UserException
   */
  private FeaturestoreJdbcConnector createOrUpdateJdbcConnector(Users user, Featurestore featurestore,
    FeaturestoreJdbcConnectorDTO featurestoreJdbcConnectorDTO, FeaturestoreJdbcConnector featurestoreJdbcConnector)
    throws FeaturestoreException, ProjectException, UserException {
    List<OptionDTO> arguments = validationDTO(featurestoreJdbcConnectorDTO);
    String password = getPassword(featurestoreJdbcConnectorDTO.getConnectionString(),
      featurestoreJdbcConnectorDTO.getArguments());
    setPasswordReplacedFields(featurestoreJdbcConnectorDTO, featurestoreJdbcConnector, password, arguments);
    // create and set secret
    createOrUpdateSecret(user, featurestore, featurestoreJdbcConnectorDTO,
      featurestoreJdbcConnector, password, featurestoreJdbcConnector.getPasswordSecret());
    
    if (!Strings.isNullOrEmpty(featurestoreJdbcConnectorDTO.getDriverPath())) {
      featurestoreJdbcConnector.setDriverPath(featurestoreJdbcConnectorDTO.getDriverPath());
    }
    return featurestoreJdbcConnector;
  }
  
  /**
   * Validate the JDBC connector DTO
   * @param featurestoreJdbcConnectorDTO
   * @return List<OptionDTO>
   * @throws FeaturestoreException
   */
  private List<OptionDTO> validationDTO(FeaturestoreJdbcConnectorDTO featurestoreJdbcConnectorDTO)
    throws FeaturestoreException {
    verifyJdbcConnectorConnectionString(featurestoreJdbcConnectorDTO.getConnectionString());
    List<OptionDTO> arguments = featurestoreJdbcConnectorDTO.getArguments();
    String argumentsString = storageConnectorUtil.fromOptions(arguments);
    verifyJdbcConnectorArguments(argumentsString);
    return arguments;
  }
  
  /** Set the connection Url and arguments with replaced password template
   * @param featurestoreJdbcConnectorDTO
   * @param featurestoreJdbcConnector
   * @param password
   * @param arguments
   */
  private void setPasswordReplacedFields(FeaturestoreJdbcConnectorDTO featurestoreJdbcConnectorDTO,
    FeaturestoreJdbcConnector featurestoreJdbcConnector, String password, List<OptionDTO> arguments) {
    // replace password in connection string to template <SECRET_PASSWORD> and set it
    featurestoreJdbcConnector
      .setConnectionString(storageConnectorUtil.replaceToPasswordTemplate(
        featurestoreJdbcConnectorDTO.getConnectionString(), password
        )
    );
    //  replace password in arguments to template <SECRET_PASSWORD> and set it
    featurestoreJdbcConnector.setArguments(storageConnectorUtil.replaceToPasswordTemplate(arguments));
  }
  
  /**
   * Create or update a secret for the JDBC connector
   * If currentSecret is null, create a new secret. If currentSecret is not null, update it.
   * @param user  Users
   * @param featurestore Featurestore
   * @param featurestoreJdbcConnectorDTO FeaturestoreJdbcConnectorDTO
   * @param featurestoreJdbcConnector FeaturestoreJdbcConnector
   * @param password String password
   * @param currentSecret Secret existing currentSecret
   * @throws ProjectException
   * @throws UserException
   */
  private void createOrUpdateSecret(Users user, Featurestore featurestore,
    FeaturestoreJdbcConnectorDTO featurestoreJdbcConnectorDTO, FeaturestoreJdbcConnector featurestoreJdbcConnector,
    String password, Secret currentSecret) throws ProjectException, UserException {
    
    if (!Strings.isNullOrEmpty(password)) {
      setSecret(user, featurestore, featurestoreJdbcConnectorDTO,
        featurestoreJdbcConnector, password, currentSecret);
    } else {
      featurestoreJdbcConnector.setPasswordSecret(null);
      // if secret exists, delete it
      if (currentSecret != null) {
        secretsFacade.deleteSecret(currentSecret.getId());
      }
    }
  }
  
  /**
   * Set a new secret or update existing on JDBC connector
   * @param user
   * @param featurestore
   * @param featurestoreJdbcConnectorDTO
   * @param featurestoreJdbcConnector
   * @param password
   * @param currentSecret
   * @throws ProjectException
   * @throws UserException
   */
  private void setSecret(Users user, Featurestore featurestore,
    FeaturestoreJdbcConnectorDTO featurestoreJdbcConnectorDTO, FeaturestoreJdbcConnector featurestoreJdbcConnector,
    String password, Secret currentSecret) throws ProjectException, UserException {
    String secretName = storageConnectorUtil.createSecretName(featurestore.getId(),
      featurestoreJdbcConnectorDTO.getName(), featurestoreJdbcConnectorDTO.getStorageConnectorType());
    Secret secret = storageConnectorUtil.updateProjectSecret(user, currentSecret, secretName, featurestore,
      password);
    featurestoreJdbcConnector.setPasswordSecret(secret);
  }
  
  /**
   * Extract the password from the connectionUrl or arguments
   *
   * @param connectionUrl
   * @param arguments
   * @return String password
   */
  private String getPassword(String connectionUrl, List<OptionDTO> arguments) {
    String password = null;
    if (connectionUrl.contains(FeaturestoreConstants.ONLINE_FEATURE_STORE_JDBC_PASSWORD_ARG)) {
      password = storageConnectorUtil.fetchPasswordFromJdbcUrl(connectionUrl);
    } else if (arguments != null) {
      // get password from arguments
      password = arguments.stream().filter(argument ->
          argument.getName().equals(FeaturestoreConstants.ONLINE_FEATURE_STORE_JDBC_PASSWORD_ARG))
        .findFirst().map(OptionDTO::getValue).orElse(null);
    }
    return password;
  }
}
