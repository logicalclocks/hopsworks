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

package io.hops.hopsworks.common.dao.featurestore.storageconnector;

import io.hops.hopsworks.common.dao.featurestore.Featurestore;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.hopsfs.FeaturestoreHopsfsConnectorController;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.hopsfs.FeaturestoreHopsfsConnectorDTO;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.jdbc.FeaturestoreJdbcConnectorController;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.jdbc.FeaturestoreJdbcConnectorDTO;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.s3.FeaturestoreS3ConnectorController;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.s3.FeaturestoreS3ConnectorDTO;
import io.hops.hopsworks.exceptions.FeaturestoreException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.ArrayList;
import java.util.List;

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
      case HopsFS:
        return featurestoreHopsfsConnectorController.getHopsfsConnectors(featurestore);
      default:
        return new ArrayList<>();
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
      case HopsFS:
        return featurestoreHopsfsConnectorController.getHopsFsConnectorWithIdAndFeaturestore(featurestore,
            storageConnectorId);
      default:
        return null;
    }
  }

  /**
   * Creates a new Storage Connector of a specific type in a feature store
   *
   * @param featurestore the featurestore to create the new connector
   * @param featurestoreStorageConnectorType the type of the storage connector
   * @param featurestoreStorageConnectorDTO the data to use when creating the storage connector
   * @return A JSON/XML DTOs representation of the created storage connector
   */
  public FeaturestoreStorageConnectorDTO createStorageConnectorWithType(
      Featurestore featurestore, FeaturestoreStorageConnectorType featurestoreStorageConnectorType,
      FeaturestoreStorageConnectorDTO featurestoreStorageConnectorDTO) {
    switch(featurestoreStorageConnectorType) {
      case S3:
        return featurestoreS3ConnectorController.createFeaturestoreS3Connector(featurestore,
            (FeaturestoreS3ConnectorDTO) featurestoreStorageConnectorDTO);
      case JDBC:
        return featurestoreJdbcConnectorController.createFeaturestoreJdbcConnector(featurestore,
            (FeaturestoreJdbcConnectorDTO) featurestoreStorageConnectorDTO);
      case HopsFS:
        return featurestoreHopsfsConnectorController.createFeaturestoreHopsfsConnector(featurestore,
            (FeaturestoreHopsfsConnectorDTO) featurestoreStorageConnectorDTO);
      default:
        return null;
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
      FeaturestoreStorageConnectorType featurestoreStorageConnectorType, Integer storageConnectorId)
      throws FeaturestoreException {
    switch(featurestoreStorageConnectorType) {
      case S3:
        return featurestoreS3ConnectorController.removeFeaturestoreS3Connector(storageConnectorId);
      case JDBC:
        return featurestoreJdbcConnectorController.removeFeaturestoreJdbcConnector(storageConnectorId);
      case HopsFS:
        return featurestoreHopsfsConnectorController.removeFeaturestoreHopsfsConnector(storageConnectorId);
      default:
        return null;
    }
  }

}
