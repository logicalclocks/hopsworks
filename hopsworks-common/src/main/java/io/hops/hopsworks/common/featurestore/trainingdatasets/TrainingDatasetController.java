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
import io.hops.hopsworks.common.dao.jobs.description.JobFacade;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.featurestore.FeaturestoreConstants;
import io.hops.hopsworks.common.featurestore.FeaturestoreFacade;
import io.hops.hopsworks.common.featurestore.feature.FeaturestoreFeatureController;
import io.hops.hopsworks.common.featurestore.jobs.FeaturestoreJobDTO;
import io.hops.hopsworks.common.featurestore.jobs.FeaturestoreJobFacade;
import io.hops.hopsworks.common.featurestore.statistics.FeaturestoreStatisticController;
import io.hops.hopsworks.common.featurestore.storageconnectors.hopsfs.FeaturestoreHopsfsConnectorController;
import io.hops.hopsworks.common.featurestore.storageconnectors.hopsfs.FeaturestoreHopsfsConnectorFacade;
import io.hops.hopsworks.common.featurestore.storageconnectors.s3.FeaturestoreS3ConnectorFacade;
import io.hops.hopsworks.common.featurestore.trainingdatasets.external.ExternalTrainingDatasetController;
import io.hops.hopsworks.common.featurestore.trainingdatasets.external.ExternalTrainingDatasetFacade;
import io.hops.hopsworks.common.featurestore.trainingdatasets.hopsfs.HopsfsTrainingDatasetController;
import io.hops.hopsworks.common.featurestore.trainingdatasets.hopsfs.HopsfsTrainingDatasetFacade;
import io.hops.hopsworks.common.featurestore.utils.FeaturestoreInputValidation;
import io.hops.hopsworks.common.featurestore.utils.FeaturestoreUtils;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.provenance.core.HopsFSProvenanceController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.jobs.FeaturestoreJob;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.hopsfs.FeaturestoreHopsfsConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.s3.FeaturestoreS3Connector;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetType;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.external.ExternalTrainingDataset;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.hopsfs.HopsfsTrainingDataset;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Class controlling the interaction with the training_dataset table and required business logic
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TrainingDatasetController {
  @EJB
  private TrainingDatasetFacade trainingDatasetFacade;
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
  private HopsfsTrainingDatasetFacade hopsfsTrainingDatasetFacade;
  @EJB
  private ExternalTrainingDatasetController externalTrainingDatasetController;
  @EJB
  private ExternalTrainingDatasetFacade externalTrainingDatasetFacade;
  @EJB
  private FeaturestoreJobFacade featurestoreJobFacade;
  @EJB
  private FeaturestoreInputValidation featurestoreInputValidation;
  @EJB
  private FeaturestoreHopsfsConnectorFacade hopsfsConnectorFacade;
  @EJB
  private FeaturestoreS3ConnectorFacade S3ConnectorFacade;
  @EJB
  private InodeController inodeController;
  @EJB
  private HopsFSProvenanceController fsProvenanceController;
  @EJB
  private FeaturestoreHopsfsConnectorController hopsfsConnectorController;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private FeaturestoreUtils featurestoreUtils;

  /**
   * Gets all trainingDatasets for a particular featurestore and project
   *
   * @param featurestore featurestore to query trainingDatasets for
   * @return list of XML/JSON DTOs of the trainingDatasets
   */
  public List<TrainingDatasetDTO> getTrainingDatasetsForFeaturestore(Featurestore featurestore) {
    List<TrainingDataset> trainingDatasets = trainingDatasetFacade.findByFeaturestore(featurestore);
    return trainingDatasets.stream().map(this::convertTrainingDatasetToDTO).collect(Collectors.toList());
  }

  /**
   * Converts a trainingDataset entity to a TrainingDataset DTO
   *
   * @param trainingDataset trainingDataset entity
   * @return JSON/XML DTO of the trainingDataset
   * @throws FeaturestoreException
   */
  private TrainingDatasetDTO convertTrainingDatasetToDTO(TrainingDataset trainingDataset) {
    TrainingDatasetDTO trainingDatasetDTO = new TrainingDatasetDTO(trainingDataset);

    String featurestoreName = featurestoreFacade.getHiveDbName(trainingDataset.getFeaturestore().getHiveDbId());
    trainingDatasetDTO.setFeaturestoreName(featurestoreName);

    switch (trainingDataset.getTrainingDatasetType()) {
      case HOPSFS_TRAINING_DATASET:
        return hopsfsTrainingDatasetController.convertHopsfsTrainingDatasetToDTO(trainingDatasetDTO, trainingDataset);
      case EXTERNAL_TRAINING_DATASET:
        return externalTrainingDatasetController.convertExternalTrainingDatasetToDTO(trainingDatasetDTO,
            trainingDataset);
      default:
        throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_TRAINING_DATASET_TYPE.getMessage() +
          ", Recognized training dataset types are: " + TrainingDatasetType.HOPSFS_TRAINING_DATASET + ", and: " +
          TrainingDatasetType.EXTERNAL_TRAINING_DATASET + ". The provided training dataset type was not recognized: "
          + trainingDataset.getTrainingDatasetType());
    }
  }

  public TrainingDatasetDTO createTrainingDataset(Users user, Project project, Featurestore featurestore,
                                                  TrainingDatasetDTO trainingDatasetDTO)
      throws FeaturestoreException, ProvenanceException, IOException {

    // Check that training dataset doesn't already exists
    if (trainingDatasetFacade.findByNameVersionAndFeaturestore
        (trainingDatasetDTO.getName(), trainingDatasetDTO.getVersion(), featurestore)
        .isPresent()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_ALREADY_EXISTS, Level.FINE,
          "Training Dataset: " + trainingDatasetDTO.getName() + ", version: " + trainingDatasetDTO.getVersion());
    }

    Inode inode = null;
    FeaturestoreHopsfsConnector hopsfsConnector = null;
    FeaturestoreS3Connector s3Connector = null;

    if(trainingDatasetDTO.getTrainingDatasetType() == TrainingDatasetType.HOPSFS_TRAINING_DATASET) {
      if (trainingDatasetDTO.getStorageConnectorId() == null) {
        hopsfsConnector = hopsfsConnectorController.getDefaultStorageConnector(featurestore);
      } else {
        hopsfsConnector = hopsfsConnectorFacade.
            findByIdAndFeaturestore(trainingDatasetDTO.getStorageConnectorId(), featurestore)
            .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.HOPSFS_CONNECTOR_NOT_FOUND,
                Level.FINE, "HOPSFS Connector: " + trainingDatasetDTO.getStorageConnectorId()));
      }

      Dataset trainingDatasetsFolder = hopsfsConnector.getHopsfsDataset();

      // TODO(Fabio) account for path
      String trainingDatasetPath = getTrainingDatasetPath(
          inodeController.getPath(trainingDatasetsFolder.getInode()),
          trainingDatasetDTO.getName(), trainingDatasetDTO.getVersion());

      DistributedFileSystemOps udfso = null;
      String username = hdfsUsersBean.getHdfsUserName(project, user);
      try {
        udfso = dfs.getDfsOps(username);
        udfso.mkdir(trainingDatasetPath);
      } finally {
        if (udfso != null) {
          udfso.close();
        }
      }

      inode = inodeController.getInodeAtPath(trainingDatasetPath);
      fsProvenanceController.trainingDatasetAttachXAttr(user, project, trainingDatasetPath,
          trainingDatasetDTO.getFeatures());
    } else {
      s3Connector = S3ConnectorFacade.findByIdAndFeaturestore(trainingDatasetDTO.getStorageConnectorId(), featurestore)
          .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.S3_CONNECTOR_NOT_FOUND,
              Level.FINE, "S3 connector: " + trainingDatasetDTO.getStorageConnectorId()));
    }

    return createTrainingDatasetMetadata(user, featurestore, trainingDatasetDTO, hopsfsConnector,  inode, s3Connector);
  }


  /**
   * Creates the metadata structure in DB for the training dataset
   *
   * @param user                     the user creating the dataset
   * @param featurestore             the featurestore linked to the training dataset
   * @param trainingDatasetDTO       user input data
   * @param inode                    for hopsfs training dataset the inode where the training
   * @return JSON/XML DTO of the trainingDataset
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  private TrainingDatasetDTO createTrainingDatasetMetadata(Users user, Featurestore featurestore,
                                                           TrainingDatasetDTO trainingDatasetDTO,
                                                           FeaturestoreHopsfsConnector hopsfsConnector,
                                                           Inode inode,
                                                           FeaturestoreS3Connector S3Connector)
      throws FeaturestoreException {
    // Verify general entity related informatio
    featurestoreInputValidation.verifyUserInput(trainingDatasetDTO);
    // Verify input specific for training dataset
    verifyTrainingDatasetInput(trainingDatasetDTO, featurestore);
    // Statistics
    verifyStatisticsInput(trainingDatasetDTO);
    
    //Create specific dataset type
    HopsfsTrainingDataset hopsfsTrainingDataset = null;
    ExternalTrainingDataset externalTrainingDataset = null;
    switch (trainingDatasetDTO.getTrainingDatasetType()) {
      case HOPSFS_TRAINING_DATASET:
        hopsfsTrainingDataset =
            hopsfsTrainingDatasetFacade.createHopsfsTrainingDataset(hopsfsConnector, inode);
        break;
      case EXTERNAL_TRAINING_DATASET:
        externalTrainingDataset = externalTrainingDatasetFacade.createExternalTrainingDataset(S3Connector,
            trainingDatasetDTO.getLocation());
        break;
      default:
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_TRAINING_DATASET_TYPE, Level.FINE,
          ", Recognized training dataset types are: " + TrainingDatasetType.HOPSFS_TRAINING_DATASET + ", and: " +
          TrainingDatasetType.EXTERNAL_TRAINING_DATASET + ". The provided training dataset type was not recognized: "
          + trainingDatasetDTO.getTrainingDatasetType());
    }
    
    //Store trainingDataset metadata in Hopsworks
    TrainingDataset trainingDataset = new TrainingDataset();
    trainingDataset.setName(trainingDatasetDTO.getName());
    trainingDataset.setHopsfsTrainingDataset(hopsfsTrainingDataset);
    trainingDataset.setExternalTrainingDataset(externalTrainingDataset);
    trainingDataset.setDataFormat(trainingDatasetDTO.getDataFormat());
    trainingDataset.setDescription(trainingDatasetDTO.getDescription());
    trainingDataset.setFeaturestore(featurestore);
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
    featurestoreJobFacade.insertJobs(trainingDataset, jobs);
    
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
      return jobDTOs.stream()
          .filter(jobDTO -> jobDTO != null && !Strings.isNullOrEmpty(jobDTO.getJobName()))
          .map(FeaturestoreJobDTO::getJobName)
          .distinct()
          .map(jobName -> jobFacade.findByProjectAndName(project, jobName))
          .collect(Collectors.toList());
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
  public TrainingDatasetDTO getTrainingDatasetWithIdAndFeaturestore(Featurestore featurestore, Integer id)
      throws FeaturestoreException {
    TrainingDataset trainingDataset = trainingDatasetFacade.findByIdAndFeaturestore(id, featurestore)
        .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_NOT_FOUND,
            Level.FINE, "trainingDatasetId: " + id));

    return convertTrainingDatasetToDTO(trainingDataset);
  }

  /**
   * Retrieves a list of trainingDataset with a particular name from a particular feature store
   *
   * @param name name of the trainingDataset
   * @param featurestore the featurestore that the trainingDataset belongs to
   * @return XML/JSON DTO of the trainingDataset
   * @throws FeaturestoreException
   */
  public List<TrainingDatasetDTO> getTrainingDatasetWithNameAndFeaturestore(Featurestore featurestore, String name)
      throws FeaturestoreException {
    List<TrainingDataset> trainingDatasetList= trainingDatasetFacade.findByNameAndFeaturestore(name, featurestore);
    if (trainingDatasetList == null || trainingDatasetList.isEmpty()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_NOT_FOUND,
          Level.FINE, "training dataset name : " + name);
    }
    return trainingDatasetList.stream().map(this::convertTrainingDatasetToDTO).collect(Collectors.toList());
  }

  /**
   * Retrieves a trainingDataset with a particular name and version from a particular feature store
   *
   * @param name name of the trainingDataset
   * @param featurestore the featurestore that the trainingDataset belongs to
   * @return XML/JSON DTO of the trainingDataset
   * @throws FeaturestoreException
   */
  public TrainingDatasetDTO getTrainingDatasetWithNameVersionAndFeaturestore(Featurestore featurestore,
        String name, Integer version) throws FeaturestoreException {

    Optional<TrainingDataset> trainingDataset =
        trainingDatasetFacade.findByNameVersionAndFeaturestore(name, version, featurestore);
    return convertTrainingDatasetToDTO(trainingDataset
        .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_NOT_FOUND,
            Level.FINE, "training dataset name : " + name)));
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
    TrainingDataset trainingDataset = trainingDatasetFacade.findByIdAndFeaturestore(id, featurestore)
        .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_NOT_FOUND,
            Level.FINE, "trainingDatasetId: " + id));

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

  public String delete(Users user, Project project, Featurestore featurestore, Integer trainingDatasetId)
      throws FeaturestoreException, IOException {

    TrainingDataset trainingDataset =
        trainingDatasetFacade.findByIdAndFeaturestore(trainingDatasetId, featurestore)
        .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_NOT_FOUND,
            Level.FINE, "training dataset id:" + trainingDatasetId));

    featurestoreUtils.verifyUserRole(trainingDataset, featurestore, user, project);

    if(trainingDataset.getTrainingDatasetType() == TrainingDatasetType.HOPSFS_TRAINING_DATASET) {
      String dsPath = inodeController.getPath(trainingDataset.getHopsfsTrainingDataset().getInode());
      String username = hdfsUsersBean.getHdfsUserName(project, user);
      DistributedFileSystemOps udfso = dfs.getDfsOps(username);
      try {
        // TODO(Fabio): if Data owner *In project* do operation as superuser
        udfso.rm(dsPath, true);
      } finally {
        if (udfso != null) {
          udfso.close();
        }
      }
    }
    deleteMetadata(trainingDataset);
    return trainingDataset.getName();
  }

  /**
   * Deletes a trainingDataset with a particular id from a particular featurestore
   *
   * @param trainingDataset the traininig dataset for which to delete the metadata
   */
  private void deleteMetadata(TrainingDataset trainingDataset) throws FeaturestoreException {
    // Here we are relying on the FK to delete the training dataset. Not great.
    switch(trainingDataset.getTrainingDatasetType()) {
      case HOPSFS_TRAINING_DATASET:
        hopsfsTrainingDatasetFacade.remove(trainingDataset.getHopsfsTrainingDataset());
        break;
      case EXTERNAL_TRAINING_DATASET:
        externalTrainingDatasetFacade.remove(trainingDataset.getExternalTrainingDataset());
        break;
      default:
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_TRAINING_DATASET_TYPE, Level.FINE,
          ", Recognized training dataset types are: " + TrainingDatasetType.HOPSFS_TRAINING_DATASET + ", and: " +
          TrainingDatasetType.EXTERNAL_TRAINING_DATASET + ". The provided training dataset type was not recognized: "
          + trainingDataset.getTrainingDatasetType());
    }
  }


  /**
   * Updates a training dataset with new metadata
   *
   * @param featurestore             the featurestore that the trainingDataset is linked to
   * @param trainingDatasetDTO       the user input data for updating the training dataset
   * @return a JSON/XML DTO of the updated training dataset
   * @throws FeaturestoreException
   */
  public TrainingDatasetDTO updateTrainingDatasetMetadata(
      Featurestore featurestore, TrainingDatasetDTO trainingDatasetDTO) throws FeaturestoreException {
    TrainingDataset trainingDataset = verifyTrainingDatasetId(trainingDatasetDTO.getId(), featurestore);
  
    // Verify general entity related information
    featurestoreInputValidation.verifyUserInput(trainingDatasetDTO);

    //Get jobs
    List<Jobs> jobs = getJobs(trainingDatasetDTO.getJobs(), featurestore.getProject());
    //Store jobs
    featurestoreJobFacade.insertJobs(trainingDataset, jobs);
    List<FeaturestoreJob> updatedJobsList = featurestoreJobFacade.getByTrainingDataset(trainingDataset);

    // Update metadata
    trainingDataset.setDescription(trainingDatasetDTO.getDescription());
    TrainingDataset updatedTrainingDataset = trainingDatasetFacade.updateTrainingDatasetMetadata(trainingDataset);
    trainingDataset.setJobs(updatedJobsList);

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
  public TrainingDatasetDTO updateTrainingDatasetStats(
    Featurestore featurestore, TrainingDatasetDTO trainingDatasetDTO) throws FeaturestoreException {
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
   * @throws FeaturestoreException if the training dataset was not found
   */
  private TrainingDataset verifyTrainingDatasetId(Integer trainingDatasetId, Featurestore featurestore)
      throws FeaturestoreException {
    return trainingDatasetFacade.findByIdAndFeaturestore(trainingDatasetId, featurestore)
        .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_NOT_FOUND,
            Level.FINE, "training dataset id: " + trainingDatasetId));
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
}
