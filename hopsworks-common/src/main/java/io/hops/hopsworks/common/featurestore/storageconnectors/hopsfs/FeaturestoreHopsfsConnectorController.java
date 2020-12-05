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

package io.hops.hopsworks.common.featurestore.storageconnectors.hopsfs;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.featurestore.FeaturestoreConstants;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.hopsfs.FeaturestoreHopsfsConnector;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Class controlling the interaction with the feature_store_hopsfs table and required business logic
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeaturestoreHopsfsConnectorController {
  @EJB
  private FeaturestoreHopsfsConnectorFacade featurestoreHopsfsConnectorFacade;
  @EJB
  private InodeController inodeController;
  @EJB
  private DatasetController datasetController;
  
  /**
   * Creates a HOPSFS storage connector for a feature store
   *
   * @param featurestore the featurestore
   * @param featurestoreHopsfsConnectorDTO the input data to use when creating the connector
   * @returns a DTO representing the created entity
   * @throws FeaturestoreException
   */
  public FeaturestoreHopsfsConnectorDTO createFeaturestoreHopsfsConnector(
      Featurestore featurestore, FeaturestoreHopsfsConnectorDTO featurestoreHopsfsConnectorDTO)
    throws FeaturestoreException {
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
   * Updates a HOPSFS storage connector for a feature store
   *
   * @param featurestore the featurestore
   * @param featurestoreHopsfsConnectorDTO the input data to use when updating the connector
   * @param storageConnectorName name of the storage connector to update
   * @returns a DTO representing the updated entity
   * @throws FeaturestoreException FeaturestoreException
   */
  public FeaturestoreHopsfsConnectorDTO updateFeaturestoreHopsfsConnector(Featurestore featurestore,
    FeaturestoreHopsfsConnectorDTO featurestoreHopsfsConnectorDTO, String storageConnectorName)
    throws FeaturestoreException {
    FeaturestoreHopsfsConnector featurestoreHopsfsConnector = verifyHopsfStorageConnectorName(featurestore,
      storageConnectorName);

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
   * Creates a default HOPSFS storage backend for storing training datasets
   *
   * @param featurestore the featurestore
   * @param hopsfsDataset the HOPSFS dataset
   * @throws FeaturestoreException
   */
  public void createHopsFsBackendForFeaturestoreConnector(Featurestore featurestore, Dataset hopsfsDataset)
    throws FeaturestoreException {
    String name = hopsfsDataset.getName();
    String description = "HOPSFS backend for storing Training Datasets of the Hopsworks Feature Store";
    FeaturestoreHopsfsConnectorDTO featurestoreHopsfsConnectorDTO = new FeaturestoreHopsfsConnectorDTO();
    featurestoreHopsfsConnectorDTO.setName(name);
    featurestoreHopsfsConnectorDTO.setDescription(description);
    featurestoreHopsfsConnectorDTO.setDatasetName(hopsfsDataset.getName());
    createFeaturestoreHopsfsConnector(featurestore, featurestoreHopsfsConnectorDTO);
  }
  
  /**
   * Removes a HOPSFS storage backend with a particular Id
   *
   * @param featurestoreHopsfsId the id
   * @returns DTO of the deleted entity
   */
  public void removeFeaturestoreHopsfsConnector(Integer featurestoreHopsfsId){
    FeaturestoreHopsfsConnector featurestoreHopsfsConnector =
        featurestoreHopsfsConnectorFacade.find(featurestoreHopsfsId);
    if (featurestoreHopsfsConnector != null) {
      featurestoreHopsfsConnectorFacade.remove(featurestoreHopsfsConnector);
    }
  }
  
  /**
   * Removes a HOPSFS storage backend with a particular by name
   * @param featurestoreHopsfsName
   * @param featurestore
   */
  public void removeFeaturestoreHopsfsConnector(String featurestoreHopsfsName, Featurestore featurestore){
    Optional<FeaturestoreHopsfsConnector> featurestoreHopsfsConnector =
      featurestoreHopsfsConnectorFacade.findByNameAndFeaturestore(featurestoreHopsfsName, featurestore);
    if (featurestoreHopsfsConnector.isPresent()) {
      featurestoreHopsfsConnectorFacade.remove(featurestoreHopsfsConnector.get());
    }
  }
  
  private FeaturestoreHopsfsConnector verifyHopsfStorageConnectorName(Featurestore featurestore,
    String storageConnectorName) throws FeaturestoreException {
    return featurestoreHopsfsConnectorFacade.findByNameAndFeaturestore(storageConnectorName, featurestore)
      .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.HOPSFS_CONNECTOR_NOT_FOUND,
        Level.FINE, "HopsFs Connector not found HopsFsConnector name: " + storageConnectorName));
  }

  /**
   * Verify user input name
   *
   * @param name the user input to verify
   * @param featurestore the featurestore to query
   * @param edit boolean flag whether the validation if for updating an existing connector or creating a new one
   * @throws FeaturestoreException
   */
  private void verifyHopsfsConnectorName(String name, Featurestore featurestore, Boolean edit)
    throws FeaturestoreException {
    if (Strings.isNullOrEmpty(name)) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_NAME,
              Level.FINE, "Illegal storage connector name, the storage connector name cannot be empty");
    }

    if(name.length() >
      FeaturestoreConstants.STORAGE_CONNECTOR_NAME_MAX_LENGTH) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_NAME, Level.FINE,
          "Illegal storage connector name, the name should be less than " +
            FeaturestoreConstants.STORAGE_CONNECTOR_NAME_MAX_LENGTH + " characters.");
    }

    if(!edit){
      if(featurestore.getHopsfsConnections().stream()
          .anyMatch(hopsfsCon -> hopsfsCon.getName().equalsIgnoreCase(name))) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_NAME, Level.FINE,
            "Illegal storage connector name, the storage connector name should be unique, there already exists a " +
              "HOPSFS connector with the same name ");
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
   * @throws FeaturestoreException
   */
  private void verifyHopsfsConnectorDescription(String description) throws FeaturestoreException {
    if(description.length() >
      FeaturestoreConstants.STORAGE_CONNECTOR_DESCRIPTION_MAX_LENGTH) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_DESCRIPTION, Level.FINE,
              "Illegal storage connector description, the description should be less than: "
              + FeaturestoreConstants.STORAGE_CONNECTOR_DESCRIPTION_MAX_LENGTH);
    }
  }

  /**
   * Verify user input dataset name
   *
   * @param datasetName the user input to verify
   * @param featurestore the featurestore to query
   * @throws FeaturestoreException
   */
  private void verifyHopsfsConnectorDatasetName(String datasetName, Featurestore featurestore)
    throws FeaturestoreException {
    Dataset dataset = datasetController.getByProjectAndDsName(featurestore.getProject(), null, datasetName);
    if (dataset == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_HOPSFS_CONNECTOR_DATASET, Level.FINE,
        "Illegal Hopsfs connector dataset, the dataset could not be found");
    }
  }
  
  /**
   * Validates user input for creating a new HOPSFS connector in a featurestore
   *
   * @param featurestore the featurestore
   * @param featurestoreHopsfsConnectorDTO the input data to use when creating the connector
   * @throws FeaturestoreException
   */
  private void verifyUserInput(Featurestore featurestore, FeaturestoreHopsfsConnectorDTO featurestoreHopsfsConnectorDTO)
    throws FeaturestoreException {
    if (featurestoreHopsfsConnectorDTO == null) {
      throw new IllegalArgumentException("Input data is null");
    }
    verifyFeaturestoreInput(featurestore);
    verifyHopsfsConnectorName(featurestoreHopsfsConnectorDTO.getName(), featurestore, false);
    verifyHopsfsConnectorDescription(featurestoreHopsfsConnectorDTO.getDescription());
    verifyHopsfsConnectorDatasetName(featurestoreHopsfsConnectorDTO.getDatasetName(), featurestore);
  }

  /**
   * Gets all HOPSFS connectors for a particular featurestore and project
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
   *
   * @param featurestore
   * @param storageConnectorName
   * @return
   * @throws FeaturestoreException
   */
  public FeaturestoreStorageConnectorDTO getHopsFsConnectorWithNameAndFeaturestore(Featurestore featurestore,
    String storageConnectorName) throws FeaturestoreException {
    FeaturestoreHopsfsConnector featurestoreHopsfsConnector =
      verifyHopsfStorageConnectorName(featurestore, storageConnectorName);
    return convertHopsfsConnectorToDTO(featurestoreHopsfsConnector);
  }

  /**
   * Get the default storage connector for the feature store. The default storage connector is the HopsFS one that
   * points to the TRAINING_DATASET dataset.
   * @param featurestore
   * @return
   * @throws FeaturestoreException
   */
  public FeaturestoreHopsfsConnector getDefaultStorageConnector(Featurestore featurestore)
      throws FeaturestoreException {
    String connectorName = featurestore.getProject().getName() + "_" +
        Settings.ServiceDataset.TRAININGDATASETS.getName();
    return featurestoreHopsfsConnectorFacade.findByNameAndFeaturestore(connectorName, featurestore)
        .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.HOPSFS_CONNECTOR_NOT_FOUND,
            Level.FINE, "Could not find default storage connector: " + connectorName));
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
    featurestoreHopsfsConnectorDTO.setHopsfsPath(inodeController.getPath(
        featurestoreHopsfsConnector.getHopsfsDataset().getInode()));
    return featurestoreHopsfsConnectorDTO;
  }
  
}
