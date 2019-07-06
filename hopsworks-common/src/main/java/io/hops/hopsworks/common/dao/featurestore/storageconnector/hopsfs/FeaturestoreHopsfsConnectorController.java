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
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.featurestore.Featurestore;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;
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
  private DatasetFacade datasetFacade;
  
  /**
   * Creates a HopsFS storage backend for a feature store
   *
   * @param featurestore the featurestore
   * @param featurestoreHopsfsConnectorDTO the input data to use when creating the connector
   * @returns a DTO representing the created entity
   */
  public FeaturestoreHopsfsConnectorDTO createFeaturestoreHopsfsConnector(
      Featurestore featurestore, FeaturestoreHopsfsConnectorDTO featurestoreHopsfsConnectorDTO) {
    verifyUserInput(featurestore, featurestoreHopsfsConnectorDTO);
    Dataset dataset = datasetFacade.findByNameAndProjectId(featurestore.getProject(),
        featurestoreHopsfsConnectorDTO.getDatasetName());
    FeaturestoreHopsfsConnector featurestoreHopsfsConnector = new FeaturestoreHopsfsConnector();
    featurestoreHopsfsConnector.setName(featurestoreHopsfsConnectorDTO.getName());
    featurestoreHopsfsConnector.setDescription(featurestoreHopsfsConnectorDTO.getDescription());
    featurestoreHopsfsConnector.setHopsfsDataset(dataset);
    featurestoreHopsfsConnector.setFeaturestore(featurestore);
    featurestoreHopsfsConnectorFacade.persist(featurestoreHopsfsConnector);
    return convertHopsfsConnectorToDTO(featurestoreHopsfsConnector);
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
   * Validates user input for creating a new HopsFS connector in a featurestore
   *
   * @param featurestore the featurestore
   * @param featurestoreHopsfsConnectorDTO the input data to use when creating the connector
   */
  public void verifyUserInput(Featurestore featurestore,
                              FeaturestoreHopsfsConnectorDTO featurestoreHopsfsConnectorDTO) {
    if (featurestore == null) {
      throw new IllegalArgumentException("Featurestore was not found");
    }

    if (featurestoreHopsfsConnectorDTO == null) {
      throw new IllegalArgumentException("Input data is null");
    }
    
    if (Strings.isNullOrEmpty(featurestoreHopsfsConnectorDTO.getName())) {
      throw new IllegalArgumentException(
        RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_NAME.getMessage() + ", the storage connector name " +
          "cannot be empty");
    }
  
    Pattern namePattern = Pattern.compile(Settings.HOPS_FEATURESTORE_REGEX);
  
    if(featurestoreHopsfsConnectorDTO.getName().length() > Settings.HOPS_STORAGE_CONNECTOR_NAME_MAX_LENGTH ||
        !namePattern.matcher(featurestoreHopsfsConnectorDTO.getName()).matches()) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_NAME.getMessage()
        + ", the name should be less than " +
        Settings.HOPS_STORAGE_CONNECTOR_NAME_MAX_LENGTH + " characters and match " +
        "the regular expression: " +  Settings.HOPS_FEATURESTORE_REGEX);
    }
  
    if(featurestore.getHopsfsConnections().stream()
      .anyMatch(hopsfsCon -> hopsfsCon.getName().equalsIgnoreCase(featurestoreHopsfsConnectorDTO.getName()))) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_NAME.getMessage() +
        ", the storage connector name should be unique, there already exists a HopsFS connector with the same name ");
    }
  
    if(featurestoreHopsfsConnectorDTO.getDescription().length() >
        Settings.HOPS_STORAGE_CONNECTOR_DESCRIPTION_MAX_LENGTH) {
      throw new IllegalArgumentException(
        RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_DESCRIPTION.getMessage() +
        ", the description should be less than: " + Settings.HOPS_STORAGE_CONNECTOR_DESCRIPTION_MAX_LENGTH);
    }
    Dataset dataset = datasetFacade.findByNameAndProjectId(featurestore.getProject(),
        featurestoreHopsfsConnectorDTO.getDatasetName());
    if(dataset == null){
      throw new IllegalArgumentException(
        RESTCodes.FeaturestoreErrorCode.ILLEGAL_HOPSFS_CONNECTOR_DATASET.getMessage() +
        ", the dataset could not be found");
    }
    
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
    FeaturestoreHopsfsConnector featurestoreHopsfsConnector =
        featurestoreHopsfsConnectorFacade.findByIdAndFeaturestore(id, featurestore);
    if (featurestoreHopsfsConnector == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.HOPSFS_CONNECTOR_NOT_FOUND,
          Level.FINE, "HopsFsConnectorId: " + id);
    }
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
