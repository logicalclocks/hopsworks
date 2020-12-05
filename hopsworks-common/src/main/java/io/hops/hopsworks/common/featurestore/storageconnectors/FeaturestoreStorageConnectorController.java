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

import io.hops.hopsworks.common.constants.auth.AllowedRoles;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.featurestore.FeaturestoreConstants;
import io.hops.hopsworks.common.featurestore.storageconnectors.hopsfs.FeaturestoreHopsfsConnectorController;
import io.hops.hopsworks.common.featurestore.storageconnectors.hopsfs.FeaturestoreHopsfsConnectorDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.jdbc.FeaturestoreJdbcConnectorController;
import io.hops.hopsworks.common.featurestore.storageconnectors.jdbc.FeaturestoreJdbcConnectorDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.redshift.FeaturestoreRedshiftConnectorController;
import io.hops.hopsworks.common.featurestore.storageconnectors.redshift.FeaturestoreRedshiftConnectorDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.s3.FeaturestoreS3ConnectorController;
import io.hops.hopsworks.common.featurestore.storageconnectors.s3.FeaturestoreS3ConnectorDTO;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Controller class for operations on storage controller in the Hopsworks Feature Store
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeaturestoreStorageConnectorController {
  @EJB
  private FeaturestoreHopsfsConnectorController featurestoreHopsfsConnectorController;
  @EJB
  private FeaturestoreJdbcConnectorController featurestoreJdbcConnectorController;
  @EJB
  private FeaturestoreRedshiftConnectorController featurestoreRedshiftConnectorController;
  @EJB
  private FeaturestoreS3ConnectorController featurestoreS3ConnectorController;
  @EJB
  private ProjectTeamFacade projectTeamFacade;

  /**
   * Returns a list with DTOs of all storage connectors for a featurestore
   *
   * @param user the user making the request
   * @param featurestore the featurestore to query
   * @param user the user making the request
   * @return List of JSON/XML DTOs of the storage connectors
   */
  public List<FeaturestoreStorageConnectorDTO> getAllStorageConnectorsForFeaturestore(Users user,
                                                                                      Featurestore featurestore)
      throws FeaturestoreException {
    List<FeaturestoreStorageConnectorDTO> featurestoreStorageConnectorDTOS = new ArrayList<>();
    featurestoreStorageConnectorDTOS.addAll(
      featurestoreJdbcConnectorController.getJdbcConnectorsForFeaturestore(user, featurestore));
    featurestoreStorageConnectorDTOS.addAll(
      featurestoreRedshiftConnectorController.getConnectorsForFeaturestore(user, featurestore));
    featurestoreStorageConnectorDTOS.addAll(
      featurestoreS3ConnectorController.getS3ConnectorsForFeaturestore(user, featurestore));
    featurestoreStorageConnectorDTOS.addAll(featurestoreHopsfsConnectorController.getHopsfsConnectors(featurestore));
    return featurestoreStorageConnectorDTOS;
  }

  /**
   * Returns a list with DTOs of all storage connectors for a featurestore with a specific type
   *
   * @param user the user making the request
   * @param featurestore the featurestore to query
   * @param featurestoreStorageConnectorType the type of the storage connector
   * @return List of JSON/XML DTOs of the storage connectors
   */
  public List<FeaturestoreStorageConnectorDTO> getAllStorageConnectorsForFeaturestoreWithType(Users user,
    Featurestore featurestore, FeaturestoreStorageConnectorType featurestoreStorageConnectorType)
    throws FeaturestoreException {
    switch(featurestoreStorageConnectorType) {
      case S3:
        return featurestoreS3ConnectorController.getS3ConnectorsForFeaturestore(user, featurestore);
      case JDBC:
        return featurestoreJdbcConnectorController.getJdbcConnectorsForFeaturestore(user, featurestore);
      case REDSHIFT:
        return featurestoreRedshiftConnectorController.getConnectorsForFeaturestore(user, featurestore);
      case HOPSFS:
        return featurestoreHopsfsConnectorController.getHopsfsConnectors(featurestore);
      default:
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_TYPE, Level.FINE,
          "Unrecognized storage connector type " + featurestoreStorageConnectorType +
            ", Recognized storage connector types are: " + FeaturestoreStorageConnectorType.HOPSFS + ", " +
            FeaturestoreStorageConnectorType.REDSHIFT + ", " + FeaturestoreStorageConnectorType.S3 + ", and " +
            FeaturestoreStorageConnectorType.JDBC);
    }
  }
  
  /**
   *
   * @param user
   * @param featurestore
   * @param featurestoreStorageConnectorType
   * @param storageConnectorName
   * @return
   * @throws FeaturestoreException
   */
  public FeaturestoreStorageConnectorDTO getStorageConnectorForFeaturestoreWithTypeAndName(Users user,
    Featurestore featurestore, FeaturestoreStorageConnectorType featurestoreStorageConnectorType,
    String storageConnectorName) throws FeaturestoreException {
    switch(featurestoreStorageConnectorType) {
      case S3:
        return featurestoreS3ConnectorController.getS3ConnectorWithNameAndFeaturestore(user, featurestore,
          storageConnectorName);
      case JDBC:
        return featurestoreJdbcConnectorController.getJdbcConnectorWithNameAndFeaturestore(user, featurestore,
          storageConnectorName);
      case REDSHIFT:
        return featurestoreRedshiftConnectorController.getConnectorsWithNameAndFeaturestore(user, featurestore,
          storageConnectorName);
      case HOPSFS:
        return featurestoreHopsfsConnectorController.getHopsFsConnectorWithNameAndFeaturestore(featurestore,
          storageConnectorName);
      default:
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_TYPE, Level.FINE,
          "Unrecognized storage connector type " + featurestoreStorageConnectorType +
            ", Recognized storage connector types are: " + FeaturestoreStorageConnectorType.HOPSFS + ", " +
            FeaturestoreStorageConnectorType.REDSHIFT + ", " + FeaturestoreStorageConnectorType.S3 + ", and " +
            FeaturestoreStorageConnectorType.JDBC);
    }
  }

  /**
   * Creates a new Storage Connector of a specific type in a feature store
   *
   * @param user the user making the request
   * @param featurestore the featurestore to create the new connector
   * @param featurestoreStorageConnectorType the type of the storage connector
   * @param featurestoreStorageConnectorDTO the data to use when creating the storage connector
   * @return A JSON/XML DTOs representation of the created storage connector
   * @throws FeaturestoreException
   */
  public FeaturestoreStorageConnectorDTO createStorageConnectorWithType(Users user, Featurestore featurestore,
    FeaturestoreStorageConnectorType featurestoreStorageConnectorType,
    FeaturestoreStorageConnectorDTO featurestoreStorageConnectorDTO) throws FeaturestoreException, UserException,
    ProjectException {
    validateUser(user, featurestore);
    switch(featurestoreStorageConnectorType) {
      case S3:
        return featurestoreS3ConnectorController.createFeaturestoreS3Connector(user, featurestore,
            (FeaturestoreS3ConnectorDTO) featurestoreStorageConnectorDTO);
      case JDBC:
        return featurestoreJdbcConnectorController.createFeaturestoreJdbcConnector(featurestore,
            (FeaturestoreJdbcConnectorDTO) featurestoreStorageConnectorDTO);
      case REDSHIFT:
        return featurestoreRedshiftConnectorController.createFeaturestoreRedshiftConnector(user, featurestore,
          (FeaturestoreRedshiftConnectorDTO) featurestoreStorageConnectorDTO);
      case HOPSFS:
        return featurestoreHopsfsConnectorController.createFeaturestoreHopsfsConnector(featurestore,
            (FeaturestoreHopsfsConnectorDTO) featurestoreStorageConnectorDTO);
      default:
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_TYPE, Level.FINE,
          "Unrecognized storage connector type " + featurestoreStorageConnectorType +
            ", Recognized storage connector types are: " + FeaturestoreStorageConnectorType.HOPSFS + ", " +
            FeaturestoreStorageConnectorType.REDSHIFT + ", " + FeaturestoreStorageConnectorType.S3 + ", and " +
            FeaturestoreStorageConnectorType.JDBC);
    }
  }

  /**
   * Updates an existing Storage Connector of a specific type in a feature store
   *
   * @param user the user making the request
   * @param featurestore the featurestore where the connector exists
   * @param featurestoreStorageConnectorType the type of the storage connector
   * @param featurestoreStorageConnectorDTO the data to use when updating the storage connector
   * @param storageConnectorName name of the connector
   * @return A JSON/XML DTOs representation of the updated storage connector
   */
  public FeaturestoreStorageConnectorDTO updateStorageConnectorWithType(Users user, Featurestore featurestore,
    FeaturestoreStorageConnectorType featurestoreStorageConnectorType,
    FeaturestoreStorageConnectorDTO featurestoreStorageConnectorDTO, String storageConnectorName)
    throws FeaturestoreException, UserException, ProjectException {
    validateUser(user, featurestore);
    switch(featurestoreStorageConnectorType) {
      case S3:
        return featurestoreS3ConnectorController.updateFeaturestoreS3Connector(user, featurestore,
            (FeaturestoreS3ConnectorDTO) featurestoreStorageConnectorDTO, storageConnectorName);
      case JDBC:
        return featurestoreJdbcConnectorController.updateFeaturestoreJdbcConnector(featurestore,
            (FeaturestoreJdbcConnectorDTO) featurestoreStorageConnectorDTO, storageConnectorName);
      case REDSHIFT:
        return featurestoreRedshiftConnectorController.updateFeaturestoreRedshiftConnector(user, featurestore,
          (FeaturestoreRedshiftConnectorDTO) featurestoreStorageConnectorDTO, storageConnectorName);
      case HOPSFS:
        return featurestoreHopsfsConnectorController.updateFeaturestoreHopsfsConnector(featurestore,
            (FeaturestoreHopsfsConnectorDTO) featurestoreStorageConnectorDTO, storageConnectorName);
      default:
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_TYPE, Level.FINE,
          "Unrecognized storage connector type " + featurestoreStorageConnectorType +
            ", Recognized storage connector types are: " + FeaturestoreStorageConnectorType.HOPSFS + ", " +
            FeaturestoreStorageConnectorType.REDSHIFT + ", " + FeaturestoreStorageConnectorType.S3 + ", and " +
            FeaturestoreStorageConnectorType.JDBC);
    }
  }

  /**
   * Deletes a storage connector with a specific type and id in a feature store
   *
   * @param user the user making the request
   * @param featurestoreStorageConnectorType the type of the storage connector
   * @param storageConnectorId id of the storage connector
   * @param featurestore
   * @return JSON/XML DTOs of the deleted storage connector
   */
  public void deleteStorageConnectorWithTypeAndId(Users user,
    FeaturestoreStorageConnectorType featurestoreStorageConnectorType, Integer storageConnectorId,
    Featurestore featurestore) throws FeaturestoreException, UserException, ProjectException {
    validateUser(user, featurestore);
    switch (featurestoreStorageConnectorType) {
      case S3:
        featurestoreS3ConnectorController.removeFeaturestoreS3Connector(user, storageConnectorId);
        break;
      case JDBC:
        featurestoreJdbcConnectorController.removeFeaturestoreJdbcConnector(storageConnectorId);
        break;
      case REDSHIFT:
        featurestoreRedshiftConnectorController.removeFeaturestoreRedshiftConnector(user, storageConnectorId);
        break;
      case HOPSFS:
        featurestoreHopsfsConnectorController.removeFeaturestoreHopsfsConnector(storageConnectorId);
        break;
      default:
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_TYPE, Level.FINE,
          "Unrecognized storage connector type " + featurestoreStorageConnectorType +
            ", Recognized storage connector types are: " + FeaturestoreStorageConnectorType.HOPSFS + ", " +
            FeaturestoreStorageConnectorType.REDSHIFT + ", " + FeaturestoreStorageConnectorType.S3 + ", and " +
            FeaturestoreStorageConnectorType.JDBC);
    }
  }
  
  /**
   * Gets the JDBC connector of the online featurestore for a particular user and project. This connector is different
   * from other connectors in that it includes a password reference to the secretsmanager that needs to be resolved.
   *
   * @param user         the user making the request
   * @param dbUsername   the database username
   * @param featurestore the featurestore metadata
   * @return a JDBC DTO connector for the online featurestore.
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public FeaturestoreJdbcConnectorDTO getOnlineFeaturestoreConnector(Users user, String dbUsername,
    Featurestore featurestore) throws FeaturestoreException {
    String onlineFeaturestoreConnectorName = dbUsername + FeaturestoreConstants.ONLINE_FEATURE_STORE_CONNECTOR_SUFFIX;
    return (FeaturestoreJdbcConnectorDTO) this.getAllStorageConnectorsForFeaturestoreWithType(user, featurestore,
            FeaturestoreStorageConnectorType.JDBC).stream().filter(dto -> dto.getName()
            .equalsIgnoreCase(onlineFeaturestoreConnectorName)).findFirst().orElseThrow(() ->
            new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ONLINE_FEATURESTORE_JDBC_CONNECTOR_NOT_FOUND,
                    Level.SEVERE, "Cannot get online featurestore JDBC connector"));
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
  
  public void deleteStorageConnectorWithTypeAndName(Users user, FeaturestoreStorageConnectorType connectorType,
    String connectorName, Featurestore featurestore) throws FeaturestoreException, UserException, ProjectException {
    validateUser(user, featurestore);
    switch (connectorType) {
      case S3:
        featurestoreS3ConnectorController.removeFeaturestoreS3Connector(user, featurestore, connectorName);
        break;
      case JDBC:
        featurestoreJdbcConnectorController.removeFeaturestoreJdbcConnector(connectorName, featurestore);
        break;
      case REDSHIFT:
        featurestoreRedshiftConnectorController.removeFeaturestoreRedshiftConnector(user, connectorName, featurestore);
        break;
      case HOPSFS:
        featurestoreHopsfsConnectorController.removeFeaturestoreHopsfsConnector(connectorName, featurestore);
        break;
      default:
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_TYPE, Level.FINE,
          "Unrecognized storage connector type " + connectorType +
            ", Recognized storage connector types are: " + FeaturestoreStorageConnectorType.HOPSFS + ", " +
            FeaturestoreStorageConnectorType.REDSHIFT + ", " + FeaturestoreStorageConnectorType.S3 + ", and " +
            FeaturestoreStorageConnectorType.JDBC);
    }
  }
}
