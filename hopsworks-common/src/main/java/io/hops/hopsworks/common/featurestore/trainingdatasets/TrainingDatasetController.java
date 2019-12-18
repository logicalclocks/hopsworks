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

package io.hops.hopsworks.common.featurestore.trainingdatasets;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.featurestore.Featurestore;
import io.hops.hopsworks.common.dao.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.common.dao.featurestore.trainingdataset.TrainingDatasetType;
import io.hops.hopsworks.common.dao.featurestore.trainingdataset.external.ExternalTrainingDataset;
import io.hops.hopsworks.common.dao.featurestore.trainingdataset.hopsfs.HopsfsTrainingDataset;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.jobs.description.JobFacade;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.featurestore.FeaturestoreConstants;
import io.hops.hopsworks.common.featurestore.FeaturestoreFacade;
import io.hops.hopsworks.common.featurestore.feature.FeaturestoreFeatureController;
import io.hops.hopsworks.common.featurestore.jobs.FeaturestoreJobController;
import io.hops.hopsworks.common.featurestore.jobs.FeaturestoreJobDTO;
import io.hops.hopsworks.common.featurestore.statistics.FeaturestoreStatisticController;
import io.hops.hopsworks.common.featurestore.trainingdatasets.external.ExternalTrainingDatasetController;
import io.hops.hopsworks.common.featurestore.trainingdatasets.external.ExternalTrainingDatasetDTO;
import io.hops.hopsworks.common.featurestore.trainingdatasets.hopsfs.HopsfsTrainingDatasetController;
import io.hops.hopsworks.common.featurestore.trainingdatasets.hopsfs.HopsfsTrainingDatasetDTO;
import io.hops.hopsworks.common.featurestore.utils.FeaturestoreInputValidation;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
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
  private FeaturestoreStatisticController featurestoreStatisticController;
  @EJB
  private FeaturestoreFeatureController featurestoreFeatureController;
  @EJB
  private JobFacade jobFacade;
  @EJB
  private DatasetController datasetController;
  @EJB
  private HopsfsTrainingDatasetController hopsfsTrainingDatasetController;
  @EJB
  private ExternalTrainingDatasetController externalTrainingDatasetController;
  @EJB
  private FeaturestoreJobController featurestoreJobController;
  @EJB
  private FeaturestoreInputValidation featurestoreInputValidation;

  /**
   * Gets all trainingDatasets for a particular featurestore and project
   *
   * @param featurestore featurestore to query trainingDatasets for
   * @return list of XML/JSON DTOs of the trainingDatasets
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public List<TrainingDatasetDTO> getTrainingDatasetsForFeaturestore(Featurestore featurestore) {
    List<TrainingDataset> trainingDatasets = trainingDatasetFacade.findByFeaturestore(featurestore);
    return trainingDatasets.stream().map(td -> convertTrainingDatasetToDTO(td)).collect(Collectors.toList());
  }

  /**
   * Converts a trainingDataset entity to a TrainingDataset DTO
   *
   * @param trainingDataset trainingDataset entity
   * @return JSON/XML DTO of the trainingDataset
   * @throws FeaturestoreException
   */
  private TrainingDatasetDTO convertTrainingDatasetToDTO(TrainingDataset trainingDataset) {
    String featurestoreName = featurestoreFacade.getHiveDbName(trainingDataset.getFeaturestore().getHiveDbId());
    switch (trainingDataset.getTrainingDatasetType()) {
      case HOPSFS_TRAINING_DATASET:
        HopsfsTrainingDatasetDTO hopsfsTrainingDatasetDTO =
          hopsfsTrainingDatasetController.convertHopsfsTrainingDatasetToDTO(trainingDataset);
        hopsfsTrainingDatasetDTO.setFeaturestoreName(featurestoreName);
        return hopsfsTrainingDatasetDTO;
      case EXTERNAL_TRAINING_DATASET:
        ExternalTrainingDatasetDTO externalTrainingDatasetDTO =
          externalTrainingDatasetController.convertExternalTrainingDatasetToDTO(trainingDataset);
        externalTrainingDatasetDTO.setFeaturestoreName(featurestoreName);
        return externalTrainingDatasetDTO;
      default:
        throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_TRAINING_DATASET_TYPE.getMessage() +
          ", Recognized training dataset types are: " + TrainingDatasetType.HOPSFS_TRAINING_DATASET + ", and: " +
          TrainingDatasetType.EXTERNAL_TRAINING_DATASET + ". The provided training dataset type was not recognized: "
          + trainingDataset.getTrainingDatasetType());
    }
  }

  /**
   * Creates a new 'managed' training dataset with extended metadata stored in Hopsworks
   *
   * @param user                     the user creating the dataset
   * @param featurestore             the featurestore linked to the training dataset
   * @param trainingDatasetDTO       user input data
   * @return JSON/XML DTO of the trainingDataset
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public TrainingDatasetDTO createTrainingDataset(Users user, Featurestore featurestore,
    TrainingDatasetDTO trainingDatasetDTO) throws FeaturestoreException {
    // Verify general entity related information
    featurestoreInputValidation.verifyUserInput(trainingDatasetDTO);
    // Verify input specific for training dataset
    verifyTrainingDatasetInput(trainingDatasetDTO, featurestore);
    // Statistics
    verifyStatisticsInput(trainingDatasetDTO);
    
    //Get username
    String hdfsUsername = hdfsUsersController.getHdfsUserName(featurestore.getProject(), user);
    HdfsUsers hdfsUser = hdfsUsersFacade.findByName(hdfsUsername);
  
    //Create specific dataset type
    HopsfsTrainingDataset hopsfsTrainingDataset = null;
    ExternalTrainingDataset externalTrainingDataset = null;
    switch (trainingDatasetDTO.getTrainingDatasetType()) {
      case HOPSFS_TRAINING_DATASET:
        hopsfsTrainingDataset =
          hopsfsTrainingDatasetController.createHopsfsTrainingDataset((HopsfsTrainingDatasetDTO) trainingDatasetDTO);
        break;
      case EXTERNAL_TRAINING_DATASET:
        removeExternalTrainingDatasetIfExists(featurestore, trainingDatasetDTO.getName(),
          trainingDatasetDTO.getVersion());
        externalTrainingDataset = externalTrainingDatasetController.createExternalTrainingDataset(
          (ExternalTrainingDatasetDTO) trainingDatasetDTO);
        break;
      default:
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_TRAINING_DATASET_TYPE, Level.FINE,
          ", Recognized training dataset types are: " + TrainingDatasetType.HOPSFS_TRAINING_DATASET + ", and: " +
          TrainingDatasetType.EXTERNAL_TRAINING_DATASET + ". The provided training dataset type was not recognized: "
          + trainingDatasetDTO.getTrainingDatasetType());
    }
    
    //Store trainingDataset metadata in Hopsworks
    TrainingDataset trainingDataset = new TrainingDataset();
    trainingDataset.setHopsfsTrainingDataset(hopsfsTrainingDataset);
    trainingDataset.setExternalTrainingDataset(externalTrainingDataset);
    trainingDataset.setDataFormat(trainingDatasetDTO.getDataFormat());
    trainingDataset.setDescription(trainingDatasetDTO.getDescription());
    trainingDataset.setFeaturestore(featurestore);
    trainingDataset.setHdfsUserId(hdfsUser.getId());
    trainingDataset.setCreated(new Date());
    trainingDataset.setCreator(user);
    trainingDataset.setVersion(trainingDatasetDTO.getVersion());
    trainingDataset.setTrainingDatasetType(trainingDatasetDTO.getTrainingDatasetType());
    trainingDatasetFacade.persist(trainingDataset);
  
    // Store statistics
    featurestoreStatisticController.updateFeaturestoreStatistics(null, trainingDataset,
        trainingDatasetDTO.getFeatureCorrelationMatrix(), trainingDatasetDTO.getDescriptiveStatistics(),
      trainingDatasetDTO.getFeaturesHistogram(), trainingDatasetDTO.getClusterAnalysis());
    
    // Store features
    featurestoreFeatureController.updateTrainingDatasetFeatures(trainingDataset, trainingDatasetDTO.getFeatures());
  
    //Get jobs
    List<Jobs> jobs = getJobs(trainingDatasetDTO.getJobs(), featurestore.getProject());
    
    //Store jobs
    featurestoreJobController.insertJobs(trainingDataset, jobs);
    
    return convertTrainingDatasetToDTO(trainingDataset);
  }
  
  /**
   * Lookup jobs by list of jobNames
   *
   * @param jobDTOs the DTOs with the job names
   * @param project the project that owns the jobs
   * @return a list of job entities
   */
  private List<Jobs> getJobs(List<FeaturestoreJobDTO> jobDTOs, Project project) {
    if(jobDTOs != null){
      return jobDTOs.stream().filter(jobDTO -> jobDTO != null && !Strings.isNullOrEmpty(jobDTO.getJobName()))
           .map(jobDTO -> jobDTO.getJobName()).distinct().map(jobName ->
          jobFacade.findByProjectAndName(project, jobName)).collect(Collectors.toList());
    } else {
      return new ArrayList<>();
    }
  }

  /**
   * Retrieves a trainingDataset with a particular id from a particular featurestore
   *
   * @param id           if of the trainingDataset
   * @param featurestore the featurestore that the trainingDataset belongs to
   * @return XML/JSON DTO of the trainingDataset
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
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
   * @throws FeaturestoreException
   */
  public Inode getInodeWithTrainingDatasetIdAndFeaturestore(Featurestore featurestore, Integer id)
      throws FeaturestoreException {
    TrainingDataset trainingDataset = trainingDatasetFacade.findByIdAndFeaturestore(id, featurestore);
    if (trainingDataset == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_NOT_FOUND,
          Level.FINE, "trainingDatasetId: " + id);
    }
    if(trainingDataset.getTrainingDatasetType() != TrainingDatasetType.HOPSFS_TRAINING_DATASET){
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.CAN_ONLY_GET_INODE_FOR_HOPSFS_TRAINING_DATASETS, Level.FINE,
        "Training Dataset Type: " + trainingDataset.getTrainingDatasetType());
    } else {
      return trainingDataset.getHopsfsTrainingDataset().getInode();
    }
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
  @TransactionAttribute(TransactionAttributeType.NEVER)
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
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public TrainingDatasetDTO deleteTrainingDatasetWithIdAndFeaturestore(
      Featurestore featurestore, Integer id) throws FeaturestoreException {
    TrainingDataset trainingDataset = trainingDatasetFacade.findByIdAndFeaturestore(id, featurestore);
    TrainingDatasetDTO trainingDatasetDTO = convertTrainingDatasetToDTO(trainingDataset);
    switch(trainingDataset.getTrainingDatasetType()) {
      case HOPSFS_TRAINING_DATASET:
        hopsfsTrainingDatasetController.removeHopsfsTrainingDataset(trainingDataset.getHopsfsTrainingDataset());
        break;
      case EXTERNAL_TRAINING_DATASET:
        externalTrainingDatasetController.removeExternalTrainingDataset(trainingDataset.getExternalTrainingDataset());
        break;
      default:
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_TRAINING_DATASET_TYPE, Level.FINE,
          ", Recognized training dataset types are: " + TrainingDatasetType.HOPSFS_TRAINING_DATASET + ", and: " +
          TrainingDatasetType.EXTERNAL_TRAINING_DATASET + ". The provided training dataset type was not recognized: "
          + trainingDataset.getTrainingDatasetType());
    }
    return trainingDatasetDTO;
  }


  /**
   * Updates a training dataset with new metadata
   *
   * @param featurestore             the featurestore that the trainingDataset is linked to
   * @param trainingDatasetDTO       the user input data for updating the training dataset
   * @return a JSON/XML DTO of the updated training dataset
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public TrainingDatasetDTO updateTrainingDatasetMetadata(
      Featurestore featurestore, TrainingDatasetDTO trainingDatasetDTO) throws FeaturestoreException {
    TrainingDataset trainingDataset = verifyTrainingDatasetId(trainingDatasetDTO.getId(), featurestore);
  
    // Verify general entity related information
    featurestoreInputValidation.verifyUserInput(trainingDatasetDTO);
  
    // Verify training dataset specific information
    verifyTrainingDatasetDataFormat(trainingDatasetDTO.getDataFormat());
  
    //Get jobs
    List<Jobs> jobs = getJobs(trainingDatasetDTO.getJobs(), featurestore.getProject());
    //Store jobs
    featurestoreJobController.insertJobs(trainingDataset, jobs);
    
    // Update metadata
    trainingDataset.setDataFormat(trainingDataset.getDataFormat());
    trainingDataset.setDescription(trainingDatasetDTO.getDescription());
    TrainingDataset updatedTrainingDataset = trainingDatasetFacade.updateTrainingDatasetMetadata(trainingDataset);
    // Store features
    featurestoreFeatureController.updateTrainingDatasetFeatures(trainingDataset, trainingDatasetDTO.getFeatures());
    switch (updatedTrainingDataset.getTrainingDatasetType()) {
      case HOPSFS_TRAINING_DATASET:
        hopsfsTrainingDatasetController.updateHopsfsTrainingDatasetMetadata(trainingDataset.getHopsfsTrainingDataset(),
          (HopsfsTrainingDatasetDTO) trainingDatasetDTO);
        break;
      case EXTERNAL_TRAINING_DATASET:
        externalTrainingDatasetController.updateExternalTrainingDatasetMetadata(
          trainingDataset.getExternalTrainingDataset(), (ExternalTrainingDatasetDTO) trainingDatasetDTO);
        break;
      default:
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_TRAINING_DATASET_TYPE, Level.FINE,
          ", Recognized training dataset types are: " + TrainingDatasetType.HOPSFS_TRAINING_DATASET + ", and: " +
            TrainingDatasetType.EXTERNAL_TRAINING_DATASET + ". The provided training dataset type was not recognized: "
            + trainingDatasetDTO.getTrainingDatasetType());
        
    }
    return convertTrainingDatasetToDTO(updatedTrainingDataset);
  }
  
  /**
   * Updates a training dataset with new metadata
   *
   * @param featurestore             the featurestore that the trainingDataset is linked to
   * @param trainingDatasetDTO       the user input data for updating the training dataset
   *
   * @return a JSON/XML DTO of the updated training dataset
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public TrainingDatasetDTO updateTrainingDatasetStats(
    Featurestore featurestore, TrainingDatasetDTO trainingDatasetDTO) {
    TrainingDataset trainingDataset = verifyTrainingDatasetId(trainingDatasetDTO.getId(), featurestore);
    verifyStatisticsInput(trainingDatasetDTO);
    featurestoreStatisticController.updateFeaturestoreStatistics(null, trainingDataset,
      trainingDatasetDTO.getFeatureCorrelationMatrix(), trainingDatasetDTO.getDescriptiveStatistics(),
      trainingDatasetDTO.getFeaturesHistogram(), trainingDatasetDTO.getClusterAnalysis());
    return convertTrainingDatasetToDTO(trainingDataset);
  }
  
  /**
   * Helper function that gets the Dataset where all the training dataset in the featurestore resides within the project
   *
   * @param project the project to get the dataset for
   * @return the training dataset for the project
   */
  public Dataset getTrainingDatasetFolder(Project project){
    return datasetController.getByProjectAndDsName(project,
        null, getTrainingDatasetFolderName(project));
  }
  
  /**
   * Returns the training dataset folder name of a project (projectname_Training_Datasets)
   *
   * @param project the project to get the folder name for
   * @return the name of the folder
   */
  public String getTrainingDatasetFolderName(Project project){
    return project.getName() + "_" + Settings.ServiceDataset.TRAININGDATASETS.getName();
  }
  
  /**
   * Helper function that gets the training dataset path from a folder and training dataset name.
   * (path_to_folder/trainingdatasetName_version)
   *
   * @param trainingDatasetsFolderPath the path to the dataset folder
   * @param trainingDatasetName the name of the training dataset
   * @param version the version of the training dataset
   * @return the path to the training dataset as a child-file of the training dataset folder
   */
  public String getTrainingDatasetPath(String trainingDatasetsFolderPath, String trainingDatasetName, Integer version){
    return trainingDatasetsFolderPath + "/" + trainingDatasetName + "_" + version;
  }
  
  /**
   * Verifies statistics user input for a feature group
   *
   * @param trainingDatasetDTO DTO containing the feature group statistics
   */
  private void verifyStatisticsInput(TrainingDatasetDTO trainingDatasetDTO) {
    if (trainingDatasetDTO.getFeatureCorrelationMatrix() != null &&
      trainingDatasetDTO.getFeatureCorrelationMatrix().getFeatureCorrelations().size() >
        FeaturestoreConstants.FEATURESTORE_STATISTICS_MAX_CORRELATIONS) {
      throw new IllegalArgumentException(
        RESTCodes.FeaturestoreErrorCode.CORRELATION_MATRIX_EXCEED_MAX_SIZE.getMessage());
    }
  }
  
  /**
   * Verifies the id of a training dataset
   *
   * @param trainingDatasetId the id of the training dataset
   * @param featurestore the featurestore to query
   * @return the training dataset with the Id if it passed the validation
   */
  private TrainingDataset verifyTrainingDatasetId(Integer trainingDatasetId, Featurestore featurestore) {
    TrainingDataset trainingDataset = trainingDatasetFacade.findByIdAndFeaturestore(trainingDatasetId, featurestore);
    if (trainingDataset == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_NOT_FOUND +
        ", training dataset id: " + trainingDatasetId);
    }
    return trainingDataset;
  }
  
  /**
   * Verify training dataset type
   *
   * @param trainingDatasetType the training dataset type to verify
   * @throws FeaturestoreException
   */
  private void verifyTrainingDatasetType(TrainingDatasetType trainingDatasetType) throws FeaturestoreException {
    if (trainingDatasetType != TrainingDatasetType.HOPSFS_TRAINING_DATASET &&
      trainingDatasetType != TrainingDatasetType.EXTERNAL_TRAINING_DATASET) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_TRAINING_DATASET_TYPE, Level.FINE,
        ", Recognized Training Dataset types are: " + TrainingDatasetType.HOPSFS_TRAINING_DATASET + ", and: " +
        TrainingDatasetType.EXTERNAL_TRAINING_DATASET+ ". The provided training dataset type was not recognized: "
        + trainingDatasetType);
    }
  }
  
  /**
   * Verify user input training dataset version
   *
   * @param version the version to verify
   * @throws FeaturestoreException
   */
  private void verifyTrainingDatasetVersion(Integer version) throws FeaturestoreException {
    if (version == null) {
      throw new IllegalArgumentException(
        RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_VERSION_NOT_PROVIDED.getMessage());
    }
    if(version <= 0) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.ILLEGAL_TRAINING_DATASET_VERSION, Level.FINE,
        " version cannot be negative or zero");
    }
  }
  
  /**
   * Verfiy user input data format
   *
   * @param dataFormat the data format to verify
   * @throws FeaturestoreException
   */
  private void verifyTrainingDatasetDataFormat(String dataFormat) throws FeaturestoreException {
    if (!FeaturestoreConstants.TRAINING_DATASET_DATA_FORMATS.contains(dataFormat)) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.ILLEGAL_TRAINING_DATASET_DATA_FORMAT, Level.FINE, ", the recognized " +
          "training dataset formats are: " +
          StringUtils.join(FeaturestoreConstants.TRAINING_DATASET_DATA_FORMATS) + ". The provided data " +
          "format:" + dataFormat + " was not recognized.");
    }
  }
  
  /**
   * Verify training dataset specific input
   *
   * @param trainingDatasetDTO the provided user input
   * @param featurestore    the feature store to perform the operation against
   * @throws FeaturestoreException
   */
  private void verifyTrainingDatasetInput(TrainingDatasetDTO trainingDatasetDTO, Featurestore featurestore)
    throws FeaturestoreException {
    verifyTrainingDatasetType(trainingDatasetDTO.getTrainingDatasetType());
    verifyTrainingDatasetVersion(trainingDatasetDTO.getVersion());
    verifyTrainingDatasetDataFormat(trainingDatasetDTO.getDataFormat());
  }
  
  /**
   * Checks whether an external training dataset with the provided name and version already exists, and if so, deletes
   * it (overwrite semantics for creating new training datasets, for hopsfs training datasets this is enforced
   * by foreign key on the inode with ON DELETE CASCADE, but not for external training datasets)
   *
   * @param featurestore the featurestore to query
   * @param externalTrainingDatasetName the name of the external training dataset
   * @param externalTrainingDatasetVersion the version of the external training dataset
   */
  private void removeExternalTrainingDatasetIfExists(Featurestore featurestore, String externalTrainingDatasetName,
    Integer externalTrainingDatasetVersion) {
    List<TrainingDataset> trainingDatasets = trainingDatasetFacade.findByFeaturestore(featurestore);
    trainingDatasets.stream().filter(td -> {
      return (
        td.getTrainingDatasetType() == TrainingDatasetType.EXTERNAL_TRAINING_DATASET &&
          td.getExternalTrainingDataset().getName().equals(externalTrainingDatasetName) &&
          td.getVersion().equals(externalTrainingDatasetVersion)
        );
      }
    ).collect(Collectors.toList());
    trainingDatasets.forEach(td ->
      externalTrainingDatasetController.removeExternalTrainingDataset(td.getExternalTrainingDataset()));
  }

}
