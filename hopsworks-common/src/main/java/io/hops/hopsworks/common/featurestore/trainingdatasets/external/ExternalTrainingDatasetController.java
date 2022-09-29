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

import com.google.common.base.Strings;
import io.hops.hopsworks.common.featurestore.FeaturestoreConstants;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorController;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetDTO;
import io.hops.hopsworks.common.cloud.TemporaryCredentialsHelper;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.exceptions.CloudException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.adls.FeaturestoreADLSConnector;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.external.ExternalTrainingDataset;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
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
  private FeaturestoreStorageConnectorController storageConnectorController;
  @EJB
  private ExternalTrainingDatasetFacade externalTrainingDatasetFacade;
  @EJB
  private TemporaryCredentialsHelper temporaryCredentialsHelper;

  private static final String ABFSS_SCHEME = "abfss://";
  private static final String ABFSS_URI_SUFFIX = ".dfs.core.windows.net";

  private static final String ADL_SCHEME = "adl://";
  private static final String ADL_URI_SUFFIX = ".azuredatalakestore.net";
  
  public ExternalTrainingDataset create(FeaturestoreConnector connector, String path, Inode inode) {
    ExternalTrainingDataset externalTrainingDataset = new ExternalTrainingDataset();
    externalTrainingDataset.setFeaturestoreConnector(connector);
    externalTrainingDataset.setPath(path);
    externalTrainingDataset.setInode(inode);
    return externalTrainingDatasetFacade.createExternalTrainingDataset(externalTrainingDataset);
  }

  public TrainingDatasetDTO convertExternalTrainingDatasetToDTO(Users user, Project project,
                                                                TrainingDatasetDTO trainingDatasetDTO,
                                                                TrainingDataset trainingDataset)
      throws FeaturestoreException, CloudException {
    ExternalTrainingDataset externalTrainingDataset = trainingDataset.getExternalTrainingDataset();
    FeaturestoreStorageConnectorDTO storageConnectorDTO = storageConnectorController
      .convertToConnectorDTO(user, project, externalTrainingDataset.getFeaturestoreConnector());
    temporaryCredentialsHelper.setTemporaryCredentials(true, user, project, -1, storageConnectorDTO);
    trainingDatasetDTO.setStorageConnector(storageConnectorDTO);
    trainingDatasetDTO.setLocation(buildDatasetPath(trainingDataset));

    return trainingDatasetDTO;
  }

  /**
   * This method return the path for external training datasets stored on external sources.
   * @param trainingDataset
   * @return
   */
  private String buildDatasetPath(TrainingDataset trainingDataset) throws FeaturestoreException {
    switch (trainingDataset.getExternalTrainingDataset().getFeaturestoreConnector().getConnectorType()) {
      case S3:
        return buildDatasetPathS3(trainingDataset);
      case ADLS:
        return buildDatasetPathADL(trainingDataset);
      case GCS:
        return buildDatasetPathGCS(trainingDataset);
      default:
        // This shouldn't happen here
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_TYPE,
            Level.SEVERE, "External training dataset type not supported");
    }
  }

  private String buildDatasetPathS3(TrainingDataset trainingDataset) {
    String bucketFolder = FeaturestoreConstants.S3_BUCKET_TRAINING_DATASETS_FOLDER;
    if (!Strings.isNullOrEmpty(trainingDataset.getExternalTrainingDataset().getPath())) {
      bucketFolder = trainingDataset.getExternalTrainingDataset().getPath();
    }

    return "s3://" + Paths.get(trainingDataset.getExternalTrainingDataset()
            .getFeaturestoreConnector().getS3Connector().getBucket(), bucketFolder,
        trainingDataset.getName() + "_" + trainingDataset.getVersion()).toString();
  }

  private String buildDatasetPathADL(TrainingDataset trainingDataset) {
    FeaturestoreADLSConnector adlsConnector = trainingDataset.getExternalTrainingDataset()
        .getFeaturestoreConnector().getAdlsConnector();
    String directory = Strings.isNullOrEmpty(trainingDataset.getExternalTrainingDataset().getPath()) ? "/" :
        trainingDataset.getExternalTrainingDataset().getPath();
    String scheme = adlsConnector.getGeneration() == 1 ? ADL_SCHEME : ABFSS_SCHEME;
    String hostname = adlsConnector.getGeneration() == 1 ?
        adlsConnector.getAccountName() + ADL_URI_SUFFIX :
        adlsConnector.getContainerName() + "@" + adlsConnector.getAccountName() + ABFSS_URI_SUFFIX;

    return scheme + hostname + directory + trainingDataset.getName() + "_" + trainingDataset.getVersion();
  }
  
  private String buildDatasetPathGCS(TrainingDataset trainingDataset) {
    String bucketFolder = FeaturestoreConstants.TRAINING_DATASETS_FOLDER;
    if (!Strings.isNullOrEmpty(trainingDataset.getExternalTrainingDataset().getPath())) {
      bucketFolder = trainingDataset.getExternalTrainingDataset().getPath();
    }
    
    return "gs://" + Paths.get(trainingDataset.getExternalTrainingDataset()
        .getFeaturestoreConnector().getGcsConnector().getBucket(), bucketFolder,
      trainingDataset.getName() + "_" + trainingDataset.getVersion()).toString();
  }
}
