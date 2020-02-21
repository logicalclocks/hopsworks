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

import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.s3.FeaturestoreS3Connector;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.external.ExternalTrainingDataset;
import com.google.common.base.Strings;
import io.hops.hopsworks.common.featurestore.FeaturestoreConstants;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorType;
import io.hops.hopsworks.common.featurestore.storageconnectors.s3.FeaturestoreS3ConnectorFacade;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetDTO;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.nio.file.Paths;
import java.util.logging.Level;

/**
 * Class controlling the interaction with the external_training_dataset table and required business logic
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ExternalTrainingDatasetController {
  @EJB
  private ExternalTrainingDatasetFacade externalTrainingDatasetFacade;
  @EJB
  private FeaturestoreS3ConnectorFacade featurestoreS3ConnectorFacade;


  /**
   * Create and persist an external training dataset
   * @param connector
   * @param path
   * @return
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  public ExternalTrainingDataset createExternalTrainingDataset(FeaturestoreS3Connector connector, String path) {
    ExternalTrainingDataset externalTrainingDataset = new ExternalTrainingDataset();
    externalTrainingDataset.setFeaturestoreS3Connector(connector);
    externalTrainingDataset.setPath(path);
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
   * @param trainingDatasetDTO the DTO to populate
   * @param trainingDataset the entity to convert
   * @return the converted DTO representation
   */
  public TrainingDatasetDTO convertExternalTrainingDatasetToDTO(TrainingDatasetDTO trainingDatasetDTO,
                                                                TrainingDataset trainingDataset) {
    ExternalTrainingDataset externalTrainingDataset = trainingDataset.getExternalTrainingDataset();

    trainingDatasetDTO.setStorageConnectorId(externalTrainingDataset.getFeaturestoreS3Connector().getId());
    trainingDatasetDTO.setStorageConnectorName(externalTrainingDataset.getFeaturestoreS3Connector().getName());
    trainingDatasetDTO.setStorageConnectorType(FeaturestoreStorageConnectorType.S3);
    trainingDatasetDTO.setLocation(buildDatasetPath(trainingDataset));

    return trainingDatasetDTO;
  }

  /**
   * This method return the s3 path for external training datasets stored on S3.
   * @param trainingDataset
   * @return
   */
  private String buildDatasetPath(TrainingDataset trainingDataset) {
    String bucketFolder = FeaturestoreConstants.S3_BUCKET_TRAINING_DATASETS_FOLDER;
    if (!Strings.isNullOrEmpty(trainingDataset.getExternalTrainingDataset().getPath())) {
      bucketFolder = trainingDataset.getExternalTrainingDataset().getPath();
    }

    return "s3://" + Paths.get(trainingDataset.getExternalTrainingDataset().getFeaturestoreS3Connector().getBucket(),
        bucketFolder,
        trainingDataset.getName() + "_" + trainingDataset.getVersion()).toString();
  }
}
