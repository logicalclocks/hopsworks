/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.dao.featurestore.trainingdataset;

import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.featurestore.Featurestore;
import io.hops.hopsworks.common.dao.featurestore.FeaturestoreFacade;
import io.hops.hopsworks.common.dao.featurestore.feature.FeatureDTO;
import io.hops.hopsworks.common.dao.featurestore.stats.FeaturestoreStatisticController;
import io.hops.hopsworks.common.dao.featurestore.stats.cluster_analysis.ClusterAnalysisDTO;
import io.hops.hopsworks.common.dao.featurestore.stats.desc_stats.DescriptiveStatsDTO;
import io.hops.hopsworks.common.dao.featurestore.stats.feature_correlation.FeatureCorrelationMatrixDTO;
import io.hops.hopsworks.common.dao.featurestore.stats.feature_distributions.FeatureDistributionsDTO;
import io.hops.hopsworks.common.dao.featurestore.feature.FeaturestoreFeatureController;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Class controlling the interaction with the training_dataset table and required business logic
 */
@Stateless
public class TrainingDatasetController {
  @EJB
  private TrainingDatasetFacade trainingDatasetFacade;
  @EJB
  private HdfsUsersFacade hdfsUsersFacade;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private FeaturestoreFacade featurestoreFacade;
  @EJB
  private InodeFacade inodeFacade;
  @EJB
  private FeaturestoreStatisticController featurestoreStatisticController;
  @EJB
  private FeaturestoreFeatureController featurestoreFeatureController;

  /**
   * Gets all trainingDatasets for a particular featurestore and project
   *
   * @param featurestore featurestore to query trainingDatasets for
   * @return list of XML/JSON DTOs of the trainingDatasets
   */
  public List<TrainingDatasetDTO> getTrainingDatasetsForFeaturestore(Featurestore featurestore) {
    List<TrainingDataset> trainingDatasets = trainingDatasetFacade.findByFeaturestore(featurestore);
    return trainingDatasets.stream().map(td -> convertTrainingDatasetToDTO(td)).collect(Collectors.toList());
  }

  /**
   * Converts a trainingDataset entity to a TrainingDataset DTO
   *
   * @param trainingDataset trainingDataset entity
   * @return JSON/XML DTO of the trainingDataset
   */
  private TrainingDatasetDTO convertTrainingDatasetToDTO(TrainingDataset trainingDataset) {
    TrainingDatasetDTO trainingDatasetDTO = new TrainingDatasetDTO(trainingDataset);
    int versionLength = trainingDataset.getVersion().toString().length();
    String trainingDatasetNameWithVersion = trainingDataset.getInode().getInodePK().getName();
    //Remove the _version suffix
    String trainingDatasetName = trainingDatasetNameWithVersion.substring
        (0, trainingDatasetNameWithVersion.length() - (1 + versionLength));
    trainingDatasetDTO.setName(trainingDatasetName);
    String featurestoreName = featurestoreFacade.getHiveDbName(trainingDataset.getFeaturestore().getHiveDbId());
    trainingDatasetDTO.setFeaturestoreName(featurestoreName);
    trainingDatasetDTO.setHdfsStorePath(inodeFacade.getPath(trainingDataset.getInode()));
    trainingDatasetDTO.setLocation(trainingDatasetDTO.getHdfsStorePath());
    return trainingDatasetDTO;
  }

  /**
   * Creates a new 'managed' training dataset with extended metadata stored in Hopsworks
   *
   * @param project                  the project that owns the training dataset
   * @param user                     the user creating the dataset
   * @param featurestore             the featurestore linked to the training dataset
   * @param job                      the job used to compute the training dataset
   * @param version                  the version of the training dataset
   * @param dataFormat               the format of the training dataset
   * @param inode                    the inode where the training dataset is stored
   * @param trainingDatasetFolder    the Hopsworks dataset-folder where the training dataset is stored
   * @param description              a description of the training dataset
   * @param featureCorrelationMatrix feature correlation data
   * @param descriptiveStatistics    descriptive statistics data
   * @param featuresHistogram        feature distributions data
   * @param clusterAnalysis          clusterAnalysis data
   * @param features                 schema of the training dataset
   * @return JSON/XML DTO of the trainingDataset
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public TrainingDatasetDTO createTrainingDataset(
      Project project, Users user, Featurestore featurestore, Jobs job, Integer version,
      String dataFormat, Inode inode, Dataset trainingDatasetFolder, String description,
      FeatureCorrelationMatrixDTO featureCorrelationMatrix, DescriptiveStatsDTO descriptiveStatistics,
      FeatureDistributionsDTO featuresHistogram,
      List<FeatureDTO> features, ClusterAnalysisDTO clusterAnalysis) {

    String hdfsUsername = hdfsUsersController.getHdfsUserName(project, user);
    HdfsUsers hdfsUser = hdfsUsersFacade.findByName(hdfsUsername);
    //Store trainingDataset metadata in Hopsworks
    TrainingDataset trainingDataset = new TrainingDataset();
    trainingDataset.setInode(inode);
    trainingDataset.setDataFormat(dataFormat);
    trainingDataset.setTrainingDatasetFolder(trainingDatasetFolder);
    trainingDataset.setDescription(description);
    trainingDataset.setFeaturestore(featurestore);
    trainingDataset.setHdfsUserId(hdfsUser.getId());
    trainingDataset.setJob(job);
    trainingDataset.setCreated(new Date());
    trainingDataset.setCreator(user);
    trainingDataset.setVersion(version);
    trainingDatasetFacade.persist(trainingDataset);
    featurestoreStatisticController.updateFeaturestoreStatistics(null, trainingDataset,
        featureCorrelationMatrix, descriptiveStatistics, featuresHistogram, clusterAnalysis);
    featurestoreFeatureController.updateTrainingDatasetFeatures(trainingDataset, features);
    return convertTrainingDatasetToDTO(trainingDataset);
  }

  /**
   * Retrieves a trainingDataset with a particular id from a particular featurestore
   *
   * @param id           if of the trainingDataset
   * @param featurestore the featurestore that the trainingDataset belongs to
   * @return XML/JSON DTO of the trainingDataset
   */
  public TrainingDatasetDTO getTrainingDatasetWithIdAndFeaturestore(Featurestore featurestore, Integer id)
      throws FeaturestoreException {
    TrainingDataset trainingDataset = trainingDatasetFacade.findByIdAndFeaturestore(id, featurestore);
    if (trainingDataset == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_NOT_FOUND,
          Level.FINE, "trainingDatasetId: " + id);
    }
    return convertTrainingDatasetToDTO(trainingDataset);
  }

  /**
   * Retrieves the inode of a trainingDataset with a particular id from a particular featurestore
   *
   * @param id           if of the trainingDataset
   * @param featurestore the featurestore that the trainingDataset belongs to
   * @return inode of the training dataset
   */
  public Inode getInodeWithTrainingDatasetIdAndFeaturestore(Featurestore featurestore, Integer id)
      throws FeaturestoreException {
    TrainingDataset trainingDataset = trainingDatasetFacade.findByIdAndFeaturestore(id, featurestore);
    if (trainingDataset == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_NOT_FOUND,
          Level.FINE, "trainingDatasetId: " + id);
    }
    return trainingDataset.getInode();
  }

  /**
   * Gets a trainingDataset in a specific project and featurestore with the given name and version
   *
   * @param project             the project of the featurestore
   * @param featurestore        the featurestore where the trainingDataset resides
   * @param trainingDatasetName the name of the trainingDataset
   * @param version             version of the trainingDataset
   * @return the trainindataset with the specific name in the specific featurestore & project
   * @throws FeaturestoreException
   */
  public TrainingDatasetDTO getTrainingDatasetByFeaturestoreAndName(
      Project project, Featurestore featurestore, String trainingDatasetName, int version)
      throws FeaturestoreException {
    List<TrainingDataset> trainingDatasets = trainingDatasetFacade.findByFeaturestore(featurestore);
    List<TrainingDatasetDTO> trainingDatasetDTOS =
        trainingDatasets.stream().map(td -> convertTrainingDatasetToDTO(td)).collect(Collectors.toList());
    List<TrainingDatasetDTO> trainingDatasetsDTOWithName =
        trainingDatasetDTOS.stream().filter(td -> td.getName().equals(trainingDatasetName) &&
            td.getVersion().intValue() == version)
            .collect(Collectors.toList());
    if (trainingDatasetsDTOWithName.size() != 1) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_NOT_FOUND,
          Level.FINE, "featurestoreId: " + featurestore.getId() + " , project: " + project.getName() +
          " trainingDatasetName: " + trainingDatasetName);
    }
    //TrainingDataset name corresponds to Hive table inside the featurestore so uniqueness is enforced by Hive
    return trainingDatasetsDTOWithName.get(0);
  }

  /**
   * Deletes a trainingDataset with a particular id from a particular featurestore
   *
   * @param id           if od the trainingDataset
   * @param featurestore the featurestore that the trainingDataset belongs to
   * @return JSON/XML DTO of the deleted trainingDataset
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public TrainingDatasetDTO deleteTrainingDatasetWithIdAndFeaturestore(
      Featurestore featurestore, Integer id) {
    TrainingDataset trainingDataset = trainingDatasetFacade.findByIdAndFeaturestore(id, featurestore);
    TrainingDatasetDTO trainingDatasetDTO = convertTrainingDatasetToDTO(trainingDataset);
    trainingDatasetFacade.remove(trainingDataset);
    return trainingDatasetDTO;
  }


  /**
   * Updates a training dataset with new metadata
   *
   * @param featurestore             the featurestore that the trainingDataset is linked to
   * @param id                       the id of hte trainingDataset to update
   * @param job                      the new job of the trainingDataset
   * @param dataFormat               the new dataFormat of the trainingDataset
   * @param description              the new description of the trainingDataset
   * @param featureCorrelationMatrix feature correlation matrix data
   * @param descriptiveStatistics    descriptive statistics data
   * @param featuresHistogram        feature distributions data
   * @param clusterAnalysis          cluster analysis data
   * @param features                 training dataset schema
   * @param updateMetadata           boolean flag whether to update featuregroup metadata
   * @param updateStats              boolean flag whether to update featuregroup stats
   * @return a JSON/XML DTO of the updated training dataset
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public TrainingDatasetDTO updateTrainingDataset(
      Featurestore featurestore, Integer id, Jobs job,
      String dataFormat, String description, FeatureCorrelationMatrixDTO featureCorrelationMatrix,
      DescriptiveStatsDTO descriptiveStatistics, FeatureDistributionsDTO featuresHistogram, List<FeatureDTO> features,
      boolean updateMetadata, boolean updateStats, ClusterAnalysisDTO clusterAnalysis) throws FeaturestoreException {
    TrainingDataset trainingDataset = trainingDatasetFacade.findByIdAndFeaturestore(id, featurestore);
    if (trainingDataset == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_NOT_FOUND,
          Level.FINE, "training dataset id: " + id);
    }
    TrainingDataset updatedTrainingDataset = trainingDataset;
    if (updateMetadata) {
      updatedTrainingDataset = trainingDatasetFacade.updateTrainingDataset(
          trainingDataset, job, dataFormat, description);
      featurestoreFeatureController.updateTrainingDatasetFeatures(updatedTrainingDataset, features);
    }
    if (updateStats) {
      featurestoreStatisticController.updateFeaturestoreStatistics(null, trainingDataset, featureCorrelationMatrix,
          descriptiveStatistics, featuresHistogram, clusterAnalysis);
    }
    return convertTrainingDatasetToDTO(updatedTrainingDataset);
  }

}
