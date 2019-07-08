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

package io.hops.hopsworks.common.dao.featurestore.trainingdataset.hopsfs_trainingdataset;

import io.hops.hopsworks.common.dao.featurestore.settings.FeaturestoreClientSettingsDTO;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.hopsfs.FeaturestoreHopsfsConnector;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.hopsfs.FeaturestoreHopsfsConnectorFacade;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.logging.Level;

/**
 * Class controlling the interaction with the on_demand_feature_group table and required business logic
 */
@Stateless
public class HopsfsTrainingDatasetController {
  @EJB
  private HopsfsTrainingDatasetFacade hopsfsTrainingDatasetFacade;
  @EJB
  private FeaturestoreHopsfsConnectorFacade featurestoreHopsfsConnectorFacade;
  @EJB
  private InodeFacade inodeFacade;
  
  /**
   * Persists a hopsfs training dataset
   *
   * @param hopsfsTrainingDatasetDTO the user input data to use when creating the training dataset
   * @return
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public HopsfsTrainingDataset createHopsfsTrainingDataset(HopsfsTrainingDatasetDTO hopsfsTrainingDatasetDTO)
      throws FeaturestoreException {
    //Verify user input
    verifyHopsfsTrainingDatasetInput(hopsfsTrainingDatasetDTO);
    //Get Inode
    Inode inode = inodeFacade.findById(hopsfsTrainingDatasetDTO.getInodeId());
    //Get HopsFS Connector
    FeaturestoreHopsfsConnector featurestoreHopsfsConnector = featurestoreHopsfsConnectorFacade.find(
      hopsfsTrainingDatasetDTO.getHopsfsConnectorId());
    if(featurestoreHopsfsConnector == null){
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.HOPSFS_CONNECTOR_NOT_FOUND,
        Level.FINE, "hopsfsConnector: " + hopsfsTrainingDatasetDTO.getHopsfsConnectorId());
    }
    HopsfsTrainingDataset hopsfsTrainingDataset = new HopsfsTrainingDataset();
    hopsfsTrainingDataset.setInode(inode);
    hopsfsTrainingDataset.setFeaturestoreHopsfsConnector(featurestoreHopsfsConnector);
    hopsfsTrainingDatasetFacade.persist(hopsfsTrainingDataset);
    return hopsfsTrainingDataset;
  }
  
  /**
   * Removes a hopsfs training dataset from the database
   *
   * @param hopsfsTrainingDataset the entity to remove
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void removeHopsfsTrainingDataset(HopsfsTrainingDataset hopsfsTrainingDataset) {
    hopsfsTrainingDatasetFacade.remove(hopsfsTrainingDataset);
  }

  /**
   * Verify user input specific for creation of hopsfs training dataset
   *
   * @param hopsfsTrainingDatasetDTO the input data to use when creating the feature group
   */
  public void verifyHopsfsTrainingDatasetInput(HopsfsTrainingDatasetDTO hopsfsTrainingDatasetDTO) {
    if(hopsfsTrainingDatasetDTO.getName().length() >
      FeaturestoreClientSettingsDTO.HOPSFS_TRAINING_DATASET_NAME_MAX_LENGTH) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_NAME.getMessage()
        + ", the name of a hopsfs training dataset should be less than "
        + FeaturestoreClientSettingsDTO.HOPSFS_TRAINING_DATASET_NAME_MAX_LENGTH + " characters");
    }
    
    if(hopsfsTrainingDatasetDTO.getHopsfsConnectorId() == null){
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.HOPSFS_CONNECTOR_ID_NOT_PROVIDED.getMessage());
    }
    FeaturestoreHopsfsConnector featurestoreHopsfsConnector =
      featurestoreHopsfsConnectorFacade.find(hopsfsTrainingDatasetDTO.getHopsfsConnectorId());
    if(featurestoreHopsfsConnector == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.HOPSFS_CONNECTOR_NOT_FOUND.getMessage()
        + "HopsFS connector with id: " + hopsfsTrainingDatasetDTO.getHopsfsConnectorId() + " was not found");
    }
  }

}
