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
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.hopsfs.FeaturestoreHopsfsConnector;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.Transactional;
import java.util.logging.Level;

/**
 * Class controlling the interaction with the feature_store_hopsfs table and required business logic
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeaturestoreHopsfsConnectorController {
  @EJB
  private InodeController inodeController;
  @EJB
  private DatasetController datasetController;

  /**
   * Creates a HOPSFS storage connector for a feature store
   *
   * @param featurestoreHopsfsConnectorDTO the input data to use when creating the connector
   * @returns a DTO representing the created entity
   * @throws FeaturestoreException
   */
  public FeaturestoreHopsfsConnector createFeaturestoreHopsfsConnector(
      Featurestore featurestore, FeaturestoreHopsfsConnectorDTO featurestoreHopsfsConnectorDTO)
      throws FeaturestoreException {
    Dataset dataset =
        verifyHopsfsConnectorDatasetName(featurestoreHopsfsConnectorDTO.getDatasetName(), featurestore);

    FeaturestoreHopsfsConnector featurestoreHopsfsConnector = new FeaturestoreHopsfsConnector();
    featurestoreHopsfsConnector.setHopsfsDataset(dataset);

    return featurestoreHopsfsConnector;
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  @Transactional(rollbackOn = FeaturestoreException.class)
  public FeaturestoreHopsfsConnector updateFeaturestoreHopsfsConnector(Featurestore featurestore,
      FeaturestoreHopsfsConnectorDTO featurestoreHopsfsConnectorDTO,
      FeaturestoreHopsfsConnector featurestoreHopsfsConnector)
      throws FeaturestoreException {

    if(!Strings.isNullOrEmpty(featurestoreHopsfsConnectorDTO.getDatasetName())){
      Dataset dataset =
          verifyHopsfsConnectorDatasetName(featurestoreHopsfsConnectorDTO.getDatasetName(), featurestore);
      featurestoreHopsfsConnector.setHopsfsDataset(dataset);
    }

    return featurestoreHopsfsConnector;
  }

  private Dataset verifyHopsfsConnectorDatasetName(String datasetName, Featurestore featurestore)
      throws FeaturestoreException {
    Dataset dataset = datasetController.getByProjectAndDsName(featurestore.getProject(), null, datasetName);

    if (dataset == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_HOPSFS_CONNECTOR_DATASET, Level.FINE,
          datasetName + " could not be found in project " + featurestore.getProject().getName());
    }

    return dataset;
  }

  public FeaturestoreHopsfsConnectorDTO getHopsfsConnectorDTO(FeaturestoreConnector featurestoreConnector) {
    FeaturestoreHopsfsConnectorDTO featurestoreHopsfsConnectorDTO = new
        FeaturestoreHopsfsConnectorDTO(featurestoreConnector);
    featurestoreHopsfsConnectorDTO.setHopsfsPath(
        inodeController.getPath(featurestoreConnector.getHopsfsConnector().getHopsfsDataset().getInode()));
    return featurestoreHopsfsConnectorDTO;
  }
}
