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

package io.hops.hopsworks.common.featurestore.trainingdatasets.external;

import io.hops.hopsworks.common.dao.featurestore.storageconnector.s3.FeaturestoreS3Connector;
import io.hops.hopsworks.common.dao.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.common.dao.featurestore.trainingdataset.external.ExternalTrainingDataset;
import io.hops.hopsworks.common.featurestore.FeaturestoreConstants;
import io.hops.hopsworks.common.featurestore.storageconnectors.s3.FeaturestoreS3ConnectorFacade;
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
   * @return the created training dataset
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public ExternalTrainingDataset createExternalTrainingDataset(ExternalTrainingDatasetDTO externalTrainingDatasetDTO)
      throws FeaturestoreException {
    // Verify external training datasset specifc input
    FeaturestoreS3Connector featurestoreS3Connector =
      verifyExternalTrainingDatasetS3ConnectorId(externalTrainingDatasetDTO.getS3ConnectorId());
    
    ExternalTrainingDataset externalTrainingDataset = new ExternalTrainingDataset();
    externalTrainingDataset.setName(externalTrainingDatasetDTO.getName());
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
   * Verifies a s3ConnectorId for an external training dataset
   *
   * @param s3ConnectorId the connector id to verify
   * @return if the verification passed, returns the connector
   * @throws FeaturestoreException
   */
  private FeaturestoreS3Connector verifyExternalTrainingDatasetS3ConnectorId(Integer s3ConnectorId)
    throws FeaturestoreException {
    if(s3ConnectorId == null){
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.S3_CONNECTOR_ID_NOT_PROVIDED.getMessage());
    }
    FeaturestoreS3Connector featurestoreS3Connector =
      featurestoreS3ConnectorFacade.find(s3ConnectorId);
    if(featurestoreS3Connector == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.S3_CONNECTOR_NOT_FOUND, Level.FINE,
        "S3 connector with id: " + s3ConnectorId + " was not found");
    }
    return featurestoreS3Connector;
  }
  
  /**
   * Converts a External Training Dataset entity into a DTO representation
   *
   * @param trainingDataset the entity to convert
   * @return the converted DTO representation
   */
  public ExternalTrainingDatasetDTO convertExternalTrainingDatasetToDTO(TrainingDataset trainingDataset) {
    ExternalTrainingDatasetDTO externalTrainingDatasetDTO = new ExternalTrainingDatasetDTO(trainingDataset);
    externalTrainingDatasetDTO.setName(trainingDataset.getExternalTrainingDataset().getName());
    externalTrainingDatasetDTO.setLocation(
      "s3a://" + trainingDataset.getExternalTrainingDataset().getFeaturestoreS3Connector().getBucket() + "/" +
        FeaturestoreConstants.S3_BUCKET_TRAINING_DATASETS_FOLDER + "/" +
        trainingDataset.getExternalTrainingDataset().getName() + "_" + trainingDataset.getVersion());
    return externalTrainingDatasetDTO;
  }
  
  /**
   * Updates metadata of an external training dataset in the feature store
   *
   * @param externalTrainingDataset the external training dataset to update
   * @param externalTrainingDatasetDTO the metadata DTO
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void updateExternalTrainingDatasetMetadata(ExternalTrainingDataset externalTrainingDataset,
    ExternalTrainingDatasetDTO externalTrainingDatasetDTO) throws FeaturestoreException {
    // Verify User Input specific for external training datasets
    FeaturestoreS3Connector featurestoreS3Connector =
      verifyExternalTrainingDatasetS3ConnectorId(externalTrainingDatasetDTO.getS3ConnectorId());
    
    externalTrainingDataset.setFeaturestoreS3Connector(featurestoreS3Connector);
    externalTrainingDataset.setName(externalTrainingDatasetDTO.getName());
    
    externalTrainingDatasetFacade.updateExternalTrainingDatasetMetadata(externalTrainingDataset);
  }

}
