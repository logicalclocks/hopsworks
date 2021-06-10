/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.storageconnectors.snowflake;

import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.featurestore.OptionDTO;
import io.hops.hopsworks.common.security.secrets.SecretsController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.snowflake.FeaturestoreSnowflakeConnector;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.secrets.Secret;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.parquet.Strings;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.Transactional;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeaturestoreSnowflakeConnectorController {
  
  private static final Logger LOGGER = Logger.getLogger(FeaturestoreSnowflakeConnectorController.class.getName());
  
  @EJB
  private SecretsController secretsController;
  @EJB
  private UserFacade userFacade;
  
  public FeaturestoreSnowflakeConnectorDTO getConnector(Users user, FeaturestoreConnector featurestoreConnector) {
    FeaturestoreSnowflakeConnectorDTO snowflakeConnectorDTO =
        new FeaturestoreSnowflakeConnectorDTO(featurestoreConnector);
    snowflakeConnectorDTO.setSfOptions(toOptions(featurestoreConnector.getSnowflakeConnector().getArguments()));
    snowflakeConnectorDTO.setPassword(getSecret(user, featurestoreConnector.getSnowflakeConnector().getPwdSecret()));
    snowflakeConnectorDTO.setToken(getSecret(user, featurestoreConnector.getSnowflakeConnector().getTokenSecret()));
    return snowflakeConnectorDTO;
  }
  
  public FeaturestoreSnowflakeConnector createConnector(Users user, Featurestore featurestore,
      FeaturestoreSnowflakeConnectorDTO featurestoreSnowflakeConnectorDTO) throws FeaturestoreException, UserException,
      ProjectException {
    verifyConnectorDTO(featurestoreSnowflakeConnectorDTO);
    Secret secret = createSecret(user, featurestore, featurestoreSnowflakeConnectorDTO);
    FeaturestoreSnowflakeConnector snowflakeConnector = new FeaturestoreSnowflakeConnector();
    setConnector(snowflakeConnector, secret, featurestoreSnowflakeConnectorDTO);
    return snowflakeConnector;
  }
  
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  @Transactional(rollbackOn = FeaturestoreException.class)
  public FeaturestoreSnowflakeConnector updateConnector(Users user,
      FeaturestoreSnowflakeConnectorDTO featurestoreSnowflakeConnectorDTO,
      FeaturestoreSnowflakeConnector snowflakeConnector) throws FeaturestoreException, UserException,
      ProjectException {
    verifyConnectorDTO(featurestoreSnowflakeConnectorDTO);
    Secret secret = updateSecret(user, featurestoreSnowflakeConnectorDTO, snowflakeConnector);
    setConnector(snowflakeConnector, secret, featurestoreSnowflakeConnectorDTO);
    return snowflakeConnector;
  }
  
  private void setConnector(FeaturestoreSnowflakeConnector snowflakeConnector, Secret secret,
      FeaturestoreSnowflakeConnectorDTO featurestoreSnowflakeConnectorDTO) {
    snowflakeConnector.setUrl(getValueOrNull(featurestoreSnowflakeConnectorDTO.getUrl()));
    snowflakeConnector.setDatabaseUser(getValueOrNull(featurestoreSnowflakeConnectorDTO.getUser()));
    snowflakeConnector.setDatabaseName(getValueOrNull(featurestoreSnowflakeConnectorDTO.getDatabase()));
    snowflakeConnector.setDatabaseSchema(getValueOrNull(featurestoreSnowflakeConnectorDTO.getSchema()));
    snowflakeConnector.setTableName(getValueOrNull(featurestoreSnowflakeConnectorDTO.getTable()));
    snowflakeConnector.setRole(getValueOrNull(featurestoreSnowflakeConnectorDTO.getRole()));
    snowflakeConnector.setWarehouse(getValueOrNull(featurestoreSnowflakeConnectorDTO.getWarehouse()));
    snowflakeConnector.setArguments(fromOptions(featurestoreSnowflakeConnectorDTO.getSfOptions()));
    if (!Strings.isNullOrEmpty(featurestoreSnowflakeConnectorDTO.getPassword())) {
      snowflakeConnector.setPwdSecret(secret);
      snowflakeConnector.setTokenSecret(null);
    } else {
      snowflakeConnector.setTokenSecret(secret);
      snowflakeConnector.setPwdSecret(null);
    }
  }
  
  private boolean isNullOrWhitespace(String val) {
    return Strings.isNullOrEmpty(val) || Strings.isNullOrEmpty(val.trim());
  }

  private String getValueOrNull(String val) {
    return isNullOrWhitespace(val)? null : val.trim();
  }

  private List<OptionDTO> toOptions(String arguments) {
    if (Strings.isNullOrEmpty(arguments)) {
      return null;
    }
    return Arrays.stream(arguments.split(";"))
        .map(arg -> arg.split("="))
        .map(a -> new OptionDTO(a[0], a[1]))
        .collect(Collectors.toList());
  }
  
  private String fromOptions(List<OptionDTO> options) {
    if (options == null || options.isEmpty()) {
      return null;
    }
    StringBuilder arguments = new StringBuilder();
    for (OptionDTO option : options) {
      arguments.append(arguments.length() > 0? ";" : "")
          .append(option.getName())
          .append("=")
          .append(option.getValue());
    }
    return arguments.toString();
  }
  
  private Secret getSecret(FeaturestoreSnowflakeConnector snowflakeConnector) {
    if (snowflakeConnector.getPwdSecret() != null) {
      return snowflakeConnector.getPwdSecret();
    } else {
      return snowflakeConnector.getTokenSecret();
    }
  }
  
  private String getSecret(Users user, Secret secret) {
    if (secret != null) {
      try {
        Users owner = userFacade.find(secret.getId().getUid());
        return secretsController.getShared(user, owner, secret.getId().getName()).getPlaintext();
      } catch (UserException | ServiceException | ProjectException e) {
        //Just return empty
      }
    }
    return null;
  }
  
  private String createSecretName(Integer featurestoreId, String connectorName) {
    return "snowflake_" + connectorName.replaceAll(" ", "_").toLowerCase() + "_" + featurestoreId;
  }
  
  private Secret createSecret(Users user, Featurestore featurestore,
      FeaturestoreSnowflakeConnectorDTO featurestoreSnowflakeConnectorDTO) throws ProjectException, UserException {
    String secretName = createSecretName(featurestore.getId(), featurestoreSnowflakeConnectorDTO.getName());
    Secret secret;
    if (!Strings.isNullOrEmpty(featurestoreSnowflakeConnectorDTO.getPassword())) {
      secret = secretsController.createSecretForProject(user, secretName,
          featurestoreSnowflakeConnectorDTO.getPassword(), featurestore.getProject().getId());
    } else {
      secret = secretsController.createSecretForProject(user, secretName,
          featurestoreSnowflakeConnectorDTO.getToken(), featurestore.getProject().getId());
    }
    return secret;
  }
  
  private Secret updateSecret(Users user, FeaturestoreSnowflakeConnectorDTO featurestoreSnowflakeConnectorDTO,
      FeaturestoreSnowflakeConnector snowflakeConnector) throws UserException, ProjectException {
    String secret;
    Secret existingSecret = getSecret(snowflakeConnector);
    secretsController.checkCanAccessSecret(existingSecret, user);
    if (!Strings.isNullOrEmpty(featurestoreSnowflakeConnectorDTO.getPassword())) {
      secret = featurestoreSnowflakeConnectorDTO.getPassword();
    } else {
      secret = featurestoreSnowflakeConnectorDTO.getToken();
    }
    try {
      existingSecret.setSecret(secretsController.encryptSecret(secret));
    } catch (IOException | GeneralSecurityException e) {
      throw new UserException(RESTCodes.UserErrorCode.SECRET_ENCRYPTION_ERROR, Level.SEVERE,
          "Error encrypting secret", "Could not encrypt Secret " + existingSecret.getId().getName(), e);
    }
    return existingSecret;
  }
  
  private void verifyConnectorDTO(FeaturestoreSnowflakeConnectorDTO featurestoreSnowflakeConnectorDTO)
      throws FeaturestoreException {
    if (featurestoreSnowflakeConnectorDTO == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
          "Null input data");
    }
    if (isNullOrWhitespace(featurestoreSnowflakeConnectorDTO.getUrl())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
          "Url can not be empty");
    }
    if (isNullOrWhitespace(featurestoreSnowflakeConnectorDTO.getUser())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
          "User can not be empty");
    }
    if (isNullOrWhitespace(featurestoreSnowflakeConnectorDTO.getSchema())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
          "Schema can not be empty");
    }
    if (isNullOrWhitespace(featurestoreSnowflakeConnectorDTO.getDatabase())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
          "Database can not be empty");
    }
    if (Strings.isNullOrEmpty(featurestoreSnowflakeConnectorDTO.getPassword()) &&
        Strings.isNullOrEmpty(featurestoreSnowflakeConnectorDTO.getToken())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
          "Password or OAuth token must be set");
    }
    if (!Strings.isNullOrEmpty(featurestoreSnowflakeConnectorDTO.getPassword()) &&
        !Strings.isNullOrEmpty(featurestoreSnowflakeConnectorDTO.getToken())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
          "Only one authentication method is allowed");
    }
  }
}
