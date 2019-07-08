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

package io.hops.hopsworks.common.dao.featurestore.trainingdataset.external_trainingdataset;

import io.hops.hopsworks.common.dao.featurestore.settings.FeaturestoreClientSettingsDTO;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.s3.FeaturestoreS3Connector;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.s3.FeaturestoreS3ConnectorFacade;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.logging.Level;

/**
 * Class controlling the interaction with the external_training_dataset table and required business logic
 */
@Stateless
public class ExternalTrainingDatasetController {
  @EJB
  private ExternalTrainingDatasetFacade externalTrainingDatasetFacade;
  @EJB
  private FeaturestoreS3ConnectorFacade featurestoreS3ConnectorFacade;
  
  /**
   * Persists an external training dataset
   *
   * @param externalTrainingDatasetDTO the user input data to use when creating the external training dataset
   * @return
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public ExternalTrainingDataset createExternalTrainingDataset(ExternalTrainingDatasetDTO externalTrainingDatasetDTO)
      throws FeaturestoreException {
    //Verify user input
    verifyExternalTrainingDatasetInput(externalTrainingDatasetDTO);
    //Get S3 Connector
    FeaturestoreS3Connector featurestoreS3Connector = featurestoreS3ConnectorFacade.find(
      externalTrainingDatasetDTO.getS3ConnectorId());
    if(featurestoreS3Connector == null){
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.S3_CONNECTOR_NOT_FOUND,
        Level.FINE, "hopsfsConnector: " + externalTrainingDatasetDTO.getS3ConnectorId());
    }
    ExternalTrainingDataset externalTrainingDataset = new ExternalTrainingDataset();
    externalTrainingDataset.setFeaturestoreS3Connector(featurestoreS3Connector);
    externalTrainingDatasetFacade.persist(externalTrainingDataset);
    return externalTrainingDataset;
  }
  
  /**
   * Removes an external training dataset from the database
   *
   * @param externalTrainingDataset the entity to remove
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void removeExternalTrainingDataset(ExternalTrainingDataset externalTrainingDataset) {
    externalTrainingDatasetFacade.remove(externalTrainingDataset);
  }

  /**
   * Verify user input specific for creation of external training dataset
   *
   * @param externalTrainingDatasetDTO the input data to use when creating the external training dataset
   */
  public void verifyExternalTrainingDatasetInput(ExternalTrainingDatasetDTO externalTrainingDatasetDTO) {
    if(externalTrainingDatasetDTO.getName().length() >
      FeaturestoreClientSettingsDTO.EXTERNAL_TRAINING_DATASET_NAME_MAX_LENGTH) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_NAME.getMessage()
        + ", the name of an external training dataset should be less than "
        + FeaturestoreClientSettingsDTO.EXTERNAL_TRAINING_DATASET_NAME_MAX_LENGTH + " characters");
    }
  
    if(externalTrainingDatasetDTO.getS3ConnectorId() == null){
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.S3_CONNECTOR_ID_NOT_PROVIDED.getMessage());
    }
    FeaturestoreS3Connector featurestoreS3Connector =
      featurestoreS3ConnectorFacade.find(externalTrainingDatasetDTO.getS3ConnectorId());
    if(featurestoreS3Connector == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.S3_CONNECTOR_NOT_FOUND.getMessage()
        + "S3 connector with id: " + externalTrainingDatasetDTO.getS3ConnectorId() + " was not found");
    }
  }

}
