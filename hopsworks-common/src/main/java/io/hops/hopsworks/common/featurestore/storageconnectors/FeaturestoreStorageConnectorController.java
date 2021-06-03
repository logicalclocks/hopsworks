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

import com.google.common.base.Strings;
import io.hops.hopsworks.common.constants.auth.AllowedRoles;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.featurestore.FeaturestoreConstants;
import io.hops.hopsworks.common.featurestore.online.OnlineFeaturestoreController;
import io.hops.hopsworks.common.featurestore.storageconnectors.adls.FeaturestoreADLSConnectorController;
import io.hops.hopsworks.common.featurestore.storageconnectors.adls.FeaturestoreADLSConnectorDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.hopsfs.FeaturestoreHopsfsConnectorController;
import io.hops.hopsworks.common.featurestore.storageconnectors.hopsfs.FeaturestoreHopsfsConnectorDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.jdbc.FeaturestoreJdbcConnectorController;
import io.hops.hopsworks.common.featurestore.storageconnectors.jdbc.FeaturestoreJdbcConnectorDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.redshift.FeaturestoreRedshiftConnectorController;
import io.hops.hopsworks.common.featurestore.storageconnectors.redshift.FeaturestoreRedshiftConnectorDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.s3.FeaturestoreS3ConnectorController;
import io.hops.hopsworks.common.featurestore.storageconnectors.s3.FeaturestoreS3ConnectorDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.snowflake.FeaturestoreSnowflakeConnectorController;
import io.hops.hopsworks.common.featurestore.storageconnectors.snowflake.FeaturestoreSnowflakeConnectorDTO;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnectorType;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.activity.ActivityFlag;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Controller class for operations on storage controller in the Hopsworks Feature Store
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeaturestoreStorageConnectorController {
  @EJB
  private FeaturestoreHopsfsConnectorController hopsfsConnectorController;
  @EJB
  private FeaturestoreJdbcConnectorController jdbcConnectorController;
  @EJB
  private FeaturestoreRedshiftConnectorController redshiftConnectorController;
  @EJB
  private FeaturestoreS3ConnectorController s3ConnectorController;
  @EJB
  private ProjectTeamFacade projectTeamFacade;
  @EJB
  private FeaturestoreConnectorFacade featurestoreConnectorFacade;
  @EJB
  private OnlineFeaturestoreController onlineFeaturestoreController;
  @EJB
  private FeaturestoreADLSConnectorController adlsConnectorController;
  @EJB
  private FeaturestoreSnowflakeConnectorController snowflakeConnectorController;
  @EJB
  private ActivityFacade activityFacade;

  /**
   * Returns a list with DTOs of all storage connectors for a featurestore
   *
   * @param user the user making the request
   * @param featurestore the featurestore to query
   * @param user the user making the request
   * @return List of JSON/XML DTOs of the storage connectors
   */
  public List<FeaturestoreStorageConnectorDTO> getConnectorsForFeaturestore(Users user, Project project,
                                                                            Featurestore featurestore)
      throws FeaturestoreException {
    List<FeaturestoreConnector> featurestoreConnectors = featurestoreConnectorFacade.findByFeaturestore(featurestore);
    List<FeaturestoreStorageConnectorDTO> featurestoreStorageConnectorDTOS = new ArrayList<>();

    for (FeaturestoreConnector featurestoreConnector : featurestoreConnectors) {
      featurestoreStorageConnectorDTOS.add(convertToConnectorDTO(user, project, featurestoreConnector));
    }

    return featurestoreStorageConnectorDTOS;
  }

  public FeaturestoreStorageConnectorDTO getConnectorWithName(Users user, Project project,
                                                              Featurestore featurestore,
                                                              String connectorName)
      throws FeaturestoreException {
    FeaturestoreConnector featurestoreConnector =
        featurestoreConnectorFacade.findByFeaturestoreName(featurestore, connectorName)
        .orElseThrow(() ->
            new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.CONNECTOR_NOT_FOUND, Level.FINE,
                "Cannot find storage connector with name: " + connectorName));

    return convertToConnectorDTO(user, project, featurestoreConnector);
  }

  public FeaturestoreStorageConnectorDTO convertToConnectorDTO(Users user, Project project,
                                                               FeaturestoreConnector featurestoreConnector)
      throws FeaturestoreException {
    switch (featurestoreConnector.getConnectorType()) {
      case S3:
        return s3ConnectorController.getS3ConnectorDTO(user, featurestoreConnector);
      case JDBC:
        return jdbcConnectorController.getJdbcConnectorDTO(user, project, featurestoreConnector);
      case HOPSFS:
        return hopsfsConnectorController.getHopsfsConnectorDTO(featurestoreConnector);
      case REDSHIFT:
        return redshiftConnectorController.getRedshiftConnectorDTO(user, featurestoreConnector);
      case ADLS:
        return adlsConnectorController.getADLConnectorDTO(user, featurestoreConnector);
      case SNOWFLAKE:
        return snowflakeConnectorController.getConnector(user, featurestoreConnector);
      default:
        // We should not reach this point
        throw new IllegalArgumentException("Feature Store connector type not recognized");
    }
  }

  /**
   * Creates a new Storage Connector of a specific type in a feature store
   *
   * @param user the user making the request
   * @param featurestore the featurestore to create the new connector
   * @param featurestoreStorageConnectorDTO the data to use when creating the storage connector
   * @return A JSON/XML DTOs representation of the created storage connector
   * @throws FeaturestoreException
   */
  public FeaturestoreStorageConnectorDTO createStorageConnector(Users user, Project project, Featurestore featurestore,
         FeaturestoreStorageConnectorDTO featurestoreStorageConnectorDTO)
      throws FeaturestoreException, UserException, ProjectException {
    validateUser(user, featurestore);

    if (featurestoreConnectorFacade.findByFeaturestoreName(featurestore, featurestoreStorageConnectorDTO.getName())
        .isPresent()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_NAME, Level.FINE,
          "Storage connector with the same name already exists. Name=" + featurestoreStorageConnectorDTO.getName());
    }

    FeaturestoreConnector featurestoreConnector = new FeaturestoreConnector();
    verifyName(featurestoreStorageConnectorDTO);
    featurestoreConnector.setName(featurestoreStorageConnectorDTO.getName());

    verifyDescription(featurestoreStorageConnectorDTO);
    featurestoreConnector.setDescription(featurestoreStorageConnectorDTO.getDescription());
    featurestoreConnector.setFeaturestore(featurestore);

    switch (featurestoreStorageConnectorDTO.getStorageConnectorType()) {
      case HOPSFS:
        featurestoreConnector.setConnectorType(FeaturestoreConnectorType.HOPSFS);
        featurestoreConnector.setHopsfsConnector(hopsfsConnectorController.createFeaturestoreHopsfsConnector(
            featurestore, (FeaturestoreHopsfsConnectorDTO) featurestoreStorageConnectorDTO));
        break;
      case S3:
        featurestoreConnector.setConnectorType(FeaturestoreConnectorType.S3);
        featurestoreConnector.setS3Connector(s3ConnectorController.createFeaturestoreS3Connector(
            user, featurestore, (FeaturestoreS3ConnectorDTO) featurestoreStorageConnectorDTO));
        break;
      case JDBC:
        featurestoreConnector.setConnectorType(FeaturestoreConnectorType.JDBC);
        featurestoreConnector.setJdbcConnector(jdbcConnectorController.createFeaturestoreJdbcConnector(
            (FeaturestoreJdbcConnectorDTO) featurestoreStorageConnectorDTO));
        break;
      case REDSHIFT:
        featurestoreConnector.setConnectorType(FeaturestoreConnectorType.REDSHIFT);
        featurestoreConnector.setRedshiftConnector(redshiftConnectorController.createFeaturestoreRedshiftConnector(
            user, featurestore, (FeaturestoreRedshiftConnectorDTO) featurestoreStorageConnectorDTO));
        break;
      case ADLS:
        featurestoreConnector.setConnectorType(FeaturestoreConnectorType.ADLS);
        featurestoreConnector.setAdlsConnector(adlsConnectorController.createADLConnector(
            user, project, featurestore, (FeaturestoreADLSConnectorDTO) featurestoreStorageConnectorDTO));
        break;
      case SNOWFLAKE:
        featurestoreConnector.setConnectorType(FeaturestoreConnectorType.SNOWFLAKE);
        featurestoreConnector.setSnowflakeConnector(snowflakeConnectorController.createConnector(user, featurestore,
          (FeaturestoreSnowflakeConnectorDTO) featurestoreStorageConnectorDTO));
        break;
      default:
        // We should not reach this point
        throw new IllegalArgumentException("Feature Store connector type not recognized");
    }

    // Update object to populate id (auto-increment) information
    featurestoreConnector = featurestoreConnectorFacade.update(featurestoreConnector);

    activityFacade.persistActivity(ActivityFacade.ADDED_FEATURESTORE_STORAGE_CONNECTOR +
        featurestoreConnector.getName(), project, user, ActivityFlag.SERVICE);

    return convertToConnectorDTO(user, project, featurestoreConnector);
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  @Transactional(rollbackOn = FeaturestoreException.class)
  public void updateStorageConnector(Users user, Project project, Featurestore featurestore,
      FeaturestoreStorageConnectorDTO featurestoreStorageConnectorDTO, String connectorName)
      throws FeaturestoreException, UserException, ProjectException {
    validateUser(user, featurestore);

    if (!connectorName.equalsIgnoreCase(featurestoreStorageConnectorDTO.getName())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG,
          Level.FINE, "Can not update connector name.");
    }

    FeaturestoreConnector featurestoreConnector =
        featurestoreConnectorFacade.findByFeaturestoreName(featurestore, connectorName)
        .orElseThrow(() ->
            new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.CONNECTOR_NOT_FOUND, Level.FINE,
                "Cannot find storage connector with name: " + connectorName));

    verifyDescription(featurestoreStorageConnectorDTO);
    featurestoreConnector.setDescription(featurestoreStorageConnectorDTO.getDescription());

    switch (featurestoreConnector.getConnectorType()) {
      case HOPSFS:
        featurestoreConnector.setHopsfsConnector(hopsfsConnectorController.updateFeaturestoreHopsfsConnector(
            featurestore, (FeaturestoreHopsfsConnectorDTO) featurestoreStorageConnectorDTO,
            featurestoreConnector.getHopsfsConnector()));
        break;
      case S3:
        featurestoreConnector.setS3Connector(s3ConnectorController.updateFeaturestoreS3Connector(
            user, featurestore, (FeaturestoreS3ConnectorDTO) featurestoreStorageConnectorDTO,
            featurestoreConnector.getS3Connector()));
        break;
      case JDBC:
        featurestoreConnector.setJdbcConnector(jdbcConnectorController.updateFeaturestoreJdbcConnector(
            (FeaturestoreJdbcConnectorDTO) featurestoreStorageConnectorDTO, featurestoreConnector.getJdbcConnector()));
        break;
      case REDSHIFT:
        featurestoreConnector.setRedshiftConnector(redshiftConnectorController.updateFeaturestoreRedshiftConnector(
            user, featurestore, (FeaturestoreRedshiftConnectorDTO) featurestoreStorageConnectorDTO,
            featurestoreConnector.getRedshiftConnector()));
        break;
      case ADLS:
        featurestoreConnector.setAdlsConnector(adlsConnectorController.updateAdlConnector(user,
            (FeaturestoreADLSConnectorDTO) featurestoreStorageConnectorDTO, featurestoreConnector.getAdlsConnector()));
        break;
      case SNOWFLAKE:
        featurestoreConnector.setSnowflakeConnector(snowflakeConnectorController.updateConnector(user,
          (FeaturestoreSnowflakeConnectorDTO) featurestoreStorageConnectorDTO,
          featurestoreConnector.getSnowflakeConnector()));
        break;
      default:
        // We should not reach this point
        throw new IllegalArgumentException("Feature Store connector type not recognized");
    }

    featurestoreConnector = featurestoreConnectorFacade.update(featurestoreConnector);

    activityFacade.persistActivity(
        ActivityFacade.UPDATED_FEATURESTORE_STORAGE_CONNECTOR + featurestoreConnector.getName(),
        project, user, ActivityFlag.SERVICE);
  }

  // The transaction here is required otherwise when calling the remove the entity is not going to be managed anymore
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  public void deleteConnectorWithName(Users user, Project project, String connectorName, Featurestore featurestore)
      throws UserException{
    validateUser(user, featurestore);
    Optional<FeaturestoreConnector> featurestoreConnectorOptional =
        featurestoreConnectorFacade.findByFeaturestoreName(featurestore, connectorName);
    if (!featurestoreConnectorOptional.isPresent()) {
      return;
    }
    FeaturestoreConnector featurestoreConnector = featurestoreConnectorOptional.get();
    featurestoreConnectorFacade.remove(featurestoreConnector);
    activityFacade.persistActivity(
        ActivityFacade.REMOVED_FEATURESTORE_STORAGE_CONNECTOR + featurestoreConnector.getName(),
        project, user, ActivityFlag.SERVICE);
  }
  
  public FeaturestoreStorageConnectorDTO getOnlineFeaturestoreConnector(Users user, Project project,
                                                                        Featurestore featurestore)
      throws FeaturestoreException {
    String dbUsername = onlineFeaturestoreController.onlineDbUsername(project, user);
    Optional<FeaturestoreConnector> featurestoreConnector = featurestoreConnectorFacade
        .findByFeaturestoreName(featurestore,
            dbUsername + FeaturestoreConstants.ONLINE_FEATURE_STORE_CONNECTOR_SUFFIX);

    if (featurestoreConnector.isPresent()) {
      return convertToConnectorDTO(user, project, featurestoreConnector.get());
    } else {
      return null;
    }
  }
  
  /**
   * Checks if the user is a data owner of the feature store project to add, edit, and delete a connector
   * @param user the user making the request
   * @param featurestore
   * @throws UserException
   */
  private void validateUser(Users user, Featurestore featurestore) throws UserException {
    String userRole = projectTeamFacade.findCurrentRole(featurestore.getProject(), user);
    if (userRole == null || !userRole.equalsIgnoreCase(AllowedRoles.DATA_OWNER)) {
      throw new UserException(RESTCodes.UserErrorCode.ACCESS_CONTROL, Level.FINE,
          "Action not allowed. User " + user.getUsername() + " is" + " not member of project ");
    }
  }

  public void verifyName(FeaturestoreStorageConnectorDTO connectorDTO) throws FeaturestoreException {
    if (Strings.isNullOrEmpty(connectorDTO.getName())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
          RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATURE_NAME + ", the storage connector name cannot be empty");
    }
    if (connectorDTO.getName().length() > FeaturestoreConstants.STORAGE_CONNECTOR_NAME_MAX_LENGTH) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
          RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATURE_NAME + ", the name should be less than " +
              FeaturestoreConstants.STORAGE_CONNECTOR_NAME_MAX_LENGTH + " characters, the provided name was: " +
              connectorDTO.getName());
    }
  }

  public void verifyDescription(FeaturestoreStorageConnectorDTO connectorDTO) throws FeaturestoreException {
    if (connectorDTO.getDescription() != null &&
        connectorDTO.getDescription().length() > FeaturestoreConstants.STORAGE_CONNECTOR_DESCRIPTION_MAX_LENGTH) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
          RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATURE_DESCRIPTION + ", the description should be less than: " +
              FeaturestoreConstants.STORAGE_CONNECTOR_DESCRIPTION_MAX_LENGTH);
    }
  }
}
