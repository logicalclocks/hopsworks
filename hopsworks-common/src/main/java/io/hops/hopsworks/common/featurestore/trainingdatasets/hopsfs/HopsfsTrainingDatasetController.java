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

package io.hops.hopsworks.common.featurestore.trainingdatasets.hopsfs;

import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.hopsfs.FeaturestoreHopsfsConnector;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.hopsfs.HopsfsTrainingDataset;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorType;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetDTO;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.featurestore.storageconnectors.hopsfs.FeaturestoreHopsfsConnectorFacade;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.logging.Level;

/**
 * Class controlling the interaction with the hopsfs_training_dataset table and required business logic
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class HopsfsTrainingDatasetController {
  @EJB
  private HopsfsTrainingDatasetFacade hopsfsTrainingDatasetFacade;
  @EJB
  private FeaturestoreHopsfsConnectorFacade featurestoreHopsfsConnectorFacade;
  @EJB
  private InodeController inodeController;

  /**
   * Create and persiste a HopsFS training dataset
   * @param connector
   * @param inode
   * @return
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  public HopsfsTrainingDataset createHopsfsTrainingDataset(FeaturestoreHopsfsConnector connector, Inode inode) {
    HopsfsTrainingDataset hopsfsTrainingDataset = new HopsfsTrainingDataset();
    hopsfsTrainingDataset.setInode(inode);
    hopsfsTrainingDataset.setFeaturestoreHopsfsConnector(connector);
    hopsfsTrainingDatasetFacade.persist(hopsfsTrainingDataset);
    return hopsfsTrainingDataset;
  }

  /**
   * Verify hopsfsconnectorid
   *
   * @param hopsfsConnectorId the id to verify
   * @throws FeaturestoreException
   */
  private FeaturestoreHopsfsConnector verifyHopsfsTrainingDatasetConnectorId(Integer hopsfsConnectorId)
    throws FeaturestoreException {
    if (hopsfsConnectorId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.HOPSFS_CONNECTOR_ID_NOT_PROVIDED.getMessage());
    }
    FeaturestoreHopsfsConnector featurestoreHopsfsConnector =
      featurestoreHopsfsConnectorFacade.find(hopsfsConnectorId);
    if (featurestoreHopsfsConnector == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.HOPSFS_CONNECTOR_NOT_FOUND, Level.FINE,
        "HOPSFS connector with id: " + hopsfsConnectorId + " was not found");
    }
    return featurestoreHopsfsConnector;
  }
  
  /**
   * Converts a Hopsfs Training Dataset entity into a DTO representation
   *
   * @param trainingDataset the entity to convert
   * @return the converted DTO representation
   */
  public TrainingDatasetDTO convertHopsfsTrainingDatasetToDTO(TrainingDatasetDTO trainingDatasetDTO,
                                                              TrainingDataset trainingDataset) {
    HopsfsTrainingDataset hopsfsTrainingDataset = trainingDataset.getHopsfsTrainingDataset();
    trainingDatasetDTO.setLocation(inodeController.getPath(hopsfsTrainingDataset.getInode()));
    trainingDatasetDTO.setInodeId(hopsfsTrainingDataset.getInode().getId());
    trainingDatasetDTO.setStorageConnectorId(hopsfsTrainingDataset.getFeaturestoreHopsfsConnector().getId());
    trainingDatasetDTO.setStorageConnectorName(hopsfsTrainingDataset.getFeaturestoreHopsfsConnector().getName());
    trainingDatasetDTO.setStorageConnectorType(FeaturestoreStorageConnectorType.HOPSFS);
    return trainingDatasetDTO;
  }
}
