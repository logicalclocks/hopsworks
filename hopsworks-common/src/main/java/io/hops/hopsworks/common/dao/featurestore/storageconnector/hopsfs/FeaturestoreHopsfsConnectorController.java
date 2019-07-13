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

package io.hops.hopsworks.common.dao.featurestore.storageconnector.hopsfs;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.featurestore.Featurestore;
import io.hops.hopsworks.common.dao.featurestore.settings.FeaturestoreClientSettingsDTO;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Class controlling the interaction with the feature_store_hopsfs table and required business logic
 */
@Stateless
public class FeaturestoreHopsfsConnectorController {
  @EJB
  private FeaturestoreHopsfsConnectorFacade featurestoreHopsfsConnectorFacade;
  @EJB
  private InodeFacade inodeFacade;
  @EJB
  private DatasetController datasetController;
  
  /**
   * Creates a HopsFS storage connector for a feature store
   *
   * @param featurestore the featurestore
   * @param featurestoreHopsfsConnectorDTO the input data to use when creating the connector
   * @returns a DTO representing the created entity
   */
  public FeaturestoreHopsfsConnectorDTO createFeaturestoreHopsfsConnector(
      Featurestore featurestore, FeaturestoreHopsfsConnectorDTO featurestoreHopsfsConnectorDTO) {
    verifyUserInput(featurestore, featurestoreHopsfsConnectorDTO);
    Dataset dataset = datasetController.getByProjectAndDsName(featurestore.getProject(),
        null, featurestoreHopsfsConnectorDTO.getDatasetName());
    FeaturestoreHopsfsConnector featurestoreHopsfsConnector = new FeaturestoreHopsfsConnector();
    featurestoreHopsfsConnector.setName(featurestoreHopsfsConnectorDTO.getName());
    featurestoreHopsfsConnector.setDescription(featurestoreHopsfsConnectorDTO.getDescription());
    featurestoreHopsfsConnector.setHopsfsDataset(dataset);
    featurestoreHopsfsConnector.setFeaturestore(featurestore);
    featurestoreHopsfsConnectorFacade.persist(featurestoreHopsfsConnector);
    return convertHopsfsConnectorToDTO(featurestoreHopsfsConnector);
  }

  /**
   * Updates a HopsFS storage connector for a feature store
   *
   * @param featurestore the featurestore
   * @param featurestoreHopsfsConnectorDTO the input data to use when updating the connector
   * @param storageConnectorId id of the storage connector to update
   * @returns a DTO representing the updated entity
   * @throws FeaturestoreException FeaturestoreException
   */
  public FeaturestoreHopsfsConnectorDTO updateFeaturestoreHopsfsConnector(
      Featurestore featurestore, FeaturestoreHopsfsConnectorDTO featurestoreHopsfsConnectorDTO,
      Integer storageConnectorId) throws FeaturestoreException {
    FeaturestoreHopsfsConnector featurestoreHopsfsConnector = verifyHopsfStorageConnectorId(storageConnectorId,
        featurestore);
    if(!Strings.isNullOrEmpty(featurestoreHopsfsConnectorDTO.getDatasetName())){
      verifyHopsfsConnectorDatasetName(featurestoreHopsfsConnectorDTO.getDatasetName(), featurestore);
      Dataset dataset = datasetController.getByProjectAndDsName(featurestore.getProject(),
          null, featurestoreHopsfsConnectorDTO.getDatasetName());
      featurestoreHopsfsConnector.setHopsfsDataset(dataset);
    }
    if(!Strings.isNullOrEmpty(featurestoreHopsfsConnectorDTO.getName())){
      verifyHopsfsConnectorName(featurestoreHopsfsConnectorDTO.getName(), featurestore, true);
      featurestoreHopsfsConnector.setName(featurestoreHopsfsConnectorDTO.getName());
    }
    if(!Strings.isNullOrEmpty(featurestoreHopsfsConnectorDTO.getDescription())){
      verifyHopsfsConnectorDescription(featurestoreHopsfsConnectorDTO.getDescription());
      featurestoreHopsfsConnector.setDescription(featurestoreHopsfsConnectorDTO.getDescription());
    }
    if(featurestore != null) {
      featurestoreHopsfsConnector.setFeaturestore(featurestore);
    }
    FeaturestoreHopsfsConnector updatedFeaturestoreHopsfsConnector =
        featurestoreHopsfsConnectorFacade.updateHopsfsConnector(featurestoreHopsfsConnector);
    return convertHopsfsConnectorToDTO(updatedFeaturestoreHopsfsConnector);
  }
  
  /**
   * Creates a default HopsFS storage backend for storing training datasets
   *
   * @param featurestore the featurestore
   * @param hopsfsDataset the HopsFS dataset
   */
  public void createHopsFsBackendForFeaturestoreConnector(Featurestore featurestore, Dataset hopsfsDataset) {
    String name = hopsfsDataset.getName();
    String description = "HopsFS backend for storing Training Datasets of the Hopsworks Feature Store";
    FeaturestoreHopsfsConnectorDTO featurestoreHopsfsConnectorDTO = new FeaturestoreHopsfsConnectorDTO();
    featurestoreHopsfsConnectorDTO.setName(name);
    featurestoreHopsfsConnectorDTO.setDescription(description);
    featurestoreHopsfsConnectorDTO.setDatasetName(hopsfsDataset.getName());
    createFeaturestoreHopsfsConnector(featurestore, featurestoreHopsfsConnectorDTO);
  }
  
  /**
   * Removes a HopsFS storage backend with a particular Id
   *
   * @param featurestoreHopsfsId the id
   * @returns DTO of the deleted entity
   */
  public FeaturestoreHopsfsConnectorDTO removeFeaturestoreHopsfsConnector(Integer featurestoreHopsfsId){
    FeaturestoreHopsfsConnector featurestoreHopsfsConnector =
      featurestoreHopsfsConnectorFacade.find(featurestoreHopsfsId);
    FeaturestoreHopsfsConnectorDTO featurestoreHopsfsConnectorDTO =
        convertHopsfsConnectorToDTO(featurestoreHopsfsConnector);
    featurestoreHopsfsConnectorFacade.remove(featurestoreHopsfsConnector);
    return featurestoreHopsfsConnectorDTO;
  }

  /**
   * Verifies that the id exists in the database
   *
   * @param storageConnectorId the id to verfiy
   * @return the storage connector with the given id
   * @throws FeaturestoreException
   */
  private FeaturestoreHopsfsConnector verifyHopsfStorageConnectorId(
      Integer storageConnectorId, Featurestore featurestore) throws FeaturestoreException {
    FeaturestoreHopsfsConnector featurestoreHopsfsConnector =
        featurestoreHopsfsConnectorFacade.findByIdAndFeaturestore(storageConnectorId, featurestore);
    if (featurestoreHopsfsConnector == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.HOPSFS_CONNECTOR_NOT_FOUND,
          Level.FINE, "HopsFsConnectorId: " + storageConnectorId);
    }
    return featurestoreHopsfsConnector;
  }

  /**
   * Verify user input name
   *
   * @param name the user input to verify
   * @param featurestore the featurestore to query
   * @param edit boolean flag whether the validation if for updating an existing connector or creating a new one
   */
  private void verifyHopsfsConnectorName(String name, Featurestore featurestore, Boolean edit){
    if (Strings.isNullOrEmpty(name)) {
      throw new IllegalArgumentException(
          RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_NAME.getMessage() +
              ", the storage connector name cannot be empty");
    }

    if(name.length() >
        FeaturestoreClientSettingsDTO.STORAGE_CONNECTOR_NAME_MAX_LENGTH) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_NAME.getMessage()
          + ", the name should be less than " +
          FeaturestoreClientSettingsDTO.STORAGE_CONNECTOR_NAME_MAX_LENGTH + " characters.");
    }

    if(!edit){
      if(featurestore.getHopsfsConnections().stream()
          .anyMatch(hopsfsCon -> hopsfsCon.getName().equalsIgnoreCase(name))) {
        throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_NAME.getMessage()
            + ", the storage connector name should be unique, there already exists a HopsFS connector " +
            "with the same name ");
      }
    }
  }

  /**
   * Verify user featurestore
   *
   * @param featurestore the user input to verify
   */
  private void verifyFeaturestoreInput(Featurestore featurestore){
    if (featurestore == null) {
      throw new IllegalArgumentException("Featurestore was not found");
    }
  }

  /**
   * Verify user input description
   *
   * @param description the user input to verify
   */
  private void verifyHopsfsConnectorDescription(String description) {
    if(description.length() >
        FeaturestoreClientSettingsDTO.STORAGE_CONNECTOR_DESCRIPTION_MAX_LENGTH) {
      throw new IllegalArgumentException(
          RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_DESCRIPTION.getMessage() +
              ", the description should be less than: "
              + FeaturestoreClientSettingsDTO.STORAGE_CONNECTOR_DESCRIPTION_MAX_LENGTH);
    }
  }

  /**
   * Verify user input dataset name
   *
   * @param datasetName the user input to verify
   * @param featurestore the featurestore to query
   */
  private void verifyHopsfsConnectorDatasetName(String datasetName, Featurestore featurestore){
    Dataset dataset = datasetController.getByProjectAndDsName(featurestore.getProject(),
        null, datasetName);
    if(dataset == null){
      throw new IllegalArgumentException(
          RESTCodes.FeaturestoreErrorCode.ILLEGAL_HOPSFS_CONNECTOR_DATASET.getMessage() +
              ", the dataset could not be found");
    }
  }
  
  /**
   * Validates user input for creating a new HopsFS connector in a featurestore
   *
   * @param featurestore the featurestore
   * @param featurestoreHopsfsConnectorDTO the input data to use when creating the connector
   */
  private void verifyUserInput(Featurestore featurestore,
                              FeaturestoreHopsfsConnectorDTO featurestoreHopsfsConnectorDTO) {
    if (featurestoreHopsfsConnectorDTO == null) {
      throw new IllegalArgumentException("Input data is null");
    }
    verifyFeaturestoreInput(featurestore);
    verifyHopsfsConnectorName(featurestoreHopsfsConnectorDTO.getName(), featurestore, false);
    verifyHopsfsConnectorDescription(featurestoreHopsfsConnectorDTO.getDescription());
    verifyHopsfsConnectorDatasetName(featurestoreHopsfsConnectorDTO.getDatasetName(), featurestore);
  }

  /**
   * Gets all HopsFS connectors for a particular featurestore and project
   *
   * @param featurestore featurestore to query for hopsfs connectors
   * @return list of XML/JSON DTOs of the hopsfs connectors
   */
  public List<FeaturestoreStorageConnectorDTO> getHopsfsConnectors(Featurestore featurestore) {
    List<FeaturestoreHopsfsConnector> hopsfsConnectors =
        featurestoreHopsfsConnectorFacade.findByFeaturestore(featurestore);
    return hopsfsConnectors.stream().map(hopsfsConnector -> (FeaturestoreStorageConnectorDTO)
        convertHopsfsConnectorToDTO(hopsfsConnector))
        .collect(Collectors.toList());
  }

  /**
   * Retrieves a Hopsfs Connector with a particular id from a particular featurestore
   *
   * @param id           id of the hopsfs connector
   * @param featurestore the featurestore that the connector belongs to
   * @return XML/JSON DTO of the hopsfs Connector
   */
  public FeaturestoreHopsfsConnectorDTO getHopsFsConnectorWithIdAndFeaturestore(Featurestore featurestore, Integer id)
      throws FeaturestoreException {
    FeaturestoreHopsfsConnector featurestoreHopsfsConnector = verifyHopsfStorageConnectorId(id,
        featurestore);
    return convertHopsfsConnectorToDTO(featurestoreHopsfsConnector);
  }

  /**
   * Convert a FeaturestoreHopsfsConnector entity to a DTO
   *
   * @param featurestoreHopsfsConnector the entity to convert to DTO
   * @return a DTO representation of the entity
   */
  private FeaturestoreHopsfsConnectorDTO convertHopsfsConnectorToDTO(
      FeaturestoreHopsfsConnector featurestoreHopsfsConnector) {
    FeaturestoreHopsfsConnectorDTO featurestoreHopsfsConnectorDTO = new
        FeaturestoreHopsfsConnectorDTO(featurestoreHopsfsConnector);
    featurestoreHopsfsConnectorDTO.setHopsfsPath(inodeFacade.getPath(
        featurestoreHopsfsConnector.getHopsfsDataset().getInode()));
    return featurestoreHopsfsConnectorDTO;
  }

}
