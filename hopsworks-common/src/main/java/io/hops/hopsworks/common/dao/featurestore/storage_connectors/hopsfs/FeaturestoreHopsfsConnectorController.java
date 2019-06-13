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

package io.hops.hopsworks.common.dao.featurestore.storage_connectors.hopsfs;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.featurestore.Featurestore;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.regex.Pattern;

/**
 * Class controlling the interaction with the feature_store_hopsfs table and required business logic
 */
@Stateless
public class FeaturestoreHopsfsConnectorController {
  @EJB
  private FeaturestoreHopsfsConnectorFacade featurestoreHopsfsConnectorFacade;
  
  /**
   * Creates a HopsFS storage backend for a feature store
   *
   * @param featurestore the featurestore
   * @param hopsfsDataset the HopsFs dataset
   * @param description a description
   * @param name a name
   * @returns a DTO representing the created entity
   */
  public FeaturestoreHopsfsConnectorDTO createFeaturestoreHopsfsConnector(Featurestore featurestore,
    Dataset hopsfsDataset, String description, String name){
    FeaturestoreHopsfsConnector featurestoreHopsfsConnector = new FeaturestoreHopsfsConnector();
    featurestoreHopsfsConnector.setName(name);
    featurestoreHopsfsConnector.setDescription(description);
    featurestoreHopsfsConnector.setHopsfsDataset(hopsfsDataset);
    featurestoreHopsfsConnector.setFeaturestore(featurestore);
    featurestoreHopsfsConnectorFacade.persist(featurestoreHopsfsConnector);
    return new FeaturestoreHopsfsConnectorDTO(featurestoreHopsfsConnector);
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
    createFeaturestoreHopsfsConnector(featurestore, hopsfsDataset, description, name);
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
      new FeaturestoreHopsfsConnectorDTO(featurestoreHopsfsConnector);
    featurestoreHopsfsConnectorFacade.remove(featurestoreHopsfsConnector);
    return featurestoreHopsfsConnectorDTO;
  }
  
  /**
   * Validates user input for creating a new HopsFS connector in a featurestore
   *
   * @param featurestore the featurestore
   * @param dataset the Hopsfs dataset to connect to
   * @param description description of the connector
   * @param name the name of the connector
   */
  public void verifyUserInput(Featurestore featurestore, Dataset dataset, String description, String name) {
    if (featurestore == null) {
      throw new IllegalArgumentException("Featurestore was not found");
    }
    
    if (Strings.isNullOrEmpty(name)) {
      throw new IllegalArgumentException(
        RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_NAME.getMessage() + ", the storage connector name " +
          "cannot be empty");
    }
  
    Pattern namePattern = Pattern.compile(Settings.HOPS_FEATURESTORE_REGEX);
  
    if(name.length() > Settings.HOPS_STORAGE_CONNECTOR_NAME_MAX_LENGTH || !namePattern.matcher(name).matches()) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_NAME.getMessage()
        + ", the name should be less than " +
        Settings.HOPS_STORAGE_CONNECTOR_NAME_MAX_LENGTH + " characters and match " +
        "the regular expression: " +  Settings.HOPS_FEATURESTORE_REGEX);
    }
  
    if(featurestore.getHopsfsConnections().stream()
      .anyMatch(hopsfsCon -> hopsfsCon.getName().equalsIgnoreCase(name))) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_NAME.getMessage() +
        ", the storage connector name should be unique, there already exists a HopsFS connector with the same name ");
    }
  
    if(description.length() > Settings.HOPS_STORAGE_CONNECTOR_DESCRIPTION_MAX_LENGTH) {
      throw new IllegalArgumentException(
        RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_DESCRIPTION.getMessage() +
        ", the description should be less than: " + Settings.HOPS_STORAGE_CONNECTOR_DESCRIPTION_MAX_LENGTH);
    }
    
    if(dataset == null){
      throw new IllegalArgumentException(
        RESTCodes.FeaturestoreErrorCode.ILLEGAL_HOPSFS_CONNECTOR_DATASET.getMessage() +
        ", the dataset could not be found");
    }
    
  }

}
