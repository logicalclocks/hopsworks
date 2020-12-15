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
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.common.security.secrets.SecretsController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.jdbc.FeaturestoreJdbcConnector;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.Transactional;
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

  private static final Logger LOGGER = Logger.getLogger(FeaturestoreJdbcConnectorController.class.getName());

  public FeaturestoreJdbcConnector createFeaturestoreJdbcConnector(
      FeaturestoreJdbcConnectorDTO featurestoreJdbcConnectorDTO)
      throws FeaturestoreException {
    verifyJdbcConnectorConnectionString(featurestoreJdbcConnectorDTO.getConnectionString());
    verifyJdbcConnectorArguments(featurestoreJdbcConnectorDTO.getArguments());

    FeaturestoreJdbcConnector featurestoreJdbcConnector = new FeaturestoreJdbcConnector();
    featurestoreJdbcConnector.setArguments(featurestoreJdbcConnectorDTO.getArguments());
    featurestoreJdbcConnector.setConnectionString(featurestoreJdbcConnectorDTO.getConnectionString());
    return featurestoreJdbcConnector;
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  @Transactional(rollbackOn = FeaturestoreException.class)
  public FeaturestoreJdbcConnector updateFeaturestoreJdbcConnector(
      FeaturestoreJdbcConnectorDTO featurestoreJdbcConnectorDTO,
      FeaturestoreJdbcConnector featurestoreJdbcConnector) throws FeaturestoreException {

    if(!Strings.isNullOrEmpty(featurestoreJdbcConnectorDTO.getConnectionString())) {
      verifyJdbcConnectorConnectionString(featurestoreJdbcConnectorDTO.getConnectionString());
      featurestoreJdbcConnector.setConnectionString(featurestoreJdbcConnectorDTO.getConnectionString());
    }

    if(!Strings.isNullOrEmpty(featurestoreJdbcConnectorDTO.getArguments())) {
      verifyJdbcConnectorArguments(featurestoreJdbcConnectorDTO.getArguments());
      featurestoreJdbcConnector.setArguments(featurestoreJdbcConnectorDTO.getArguments());
    }

    return featurestoreJdbcConnector;
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
        && arguments.length() > FeaturestoreConstants.JDBC_STORAGE_CONNECTOR_ARGUMENTS_MAX_LENGTH) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_JDBC_CONNECTION_ARGUMENTS, Level.FINE,
              "JDBC connection arguments should not exceed: " +
                  FeaturestoreConstants.JDBC_STORAGE_CONNECTOR_ARGUMENTS_MAX_LENGTH + " characters");
    }
  }

  public FeaturestoreJdbcConnectorDTO getJdbcConnectorDTO(Users user, Project project,
                                                          FeaturestoreConnector featurestoreConnector)
      throws FeaturestoreException {
    FeaturestoreJdbcConnectorDTO featurestoreJdbcConnectorDTO =
      new FeaturestoreJdbcConnectorDTO(featurestoreConnector);
    if(featurestoreJdbcConnectorDTO.getName()
        .equals(onlineFeaturestoreController.onlineDbUsername(project, user)
            + FeaturestoreConstants.ONLINE_FEATURE_STORE_CONNECTOR_SUFFIX)) {
      setPasswordPlainTextForOnlineJdbcConnector(user, featurestoreJdbcConnectorDTO, project.getName());
    }
    return replaceOnlineFsConnectorUrl(featurestoreJdbcConnectorDTO);
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
}
