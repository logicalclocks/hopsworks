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
import com.logicalclocks.shaded.com.google.common.collect.Streams;
import io.hops.hopsworks.common.dao.jobs.description.JobFacade;
import io.hops.hopsworks.common.featurestore.FeaturestoreConstants;
import io.hops.hopsworks.common.featurestore.FeaturestoreFacade;
import io.hops.hopsworks.common.featurestore.feature.TrainingDatasetFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.jobs.FeaturestoreJobDTO;
import io.hops.hopsworks.common.featurestore.jobs.FeaturestoreJobFacade;
import io.hops.hopsworks.common.featurestore.statistics.StatisticsController;
import io.hops.hopsworks.common.featurestore.online.OnlineFeaturestoreController;
import io.hops.hopsworks.common.featurestore.query.ConstructorController;
import io.hops.hopsworks.common.featurestore.query.Feature;
import io.hops.hopsworks.common.featurestore.query.Join;
import io.hops.hopsworks.common.featurestore.query.Query;
import io.hops.hopsworks.common.featurestore.query.QueryDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.hopsfs.FeaturestoreHopsfsConnectorController;
import io.hops.hopsworks.common.featurestore.storageconnectors.hopsfs.FeaturestoreHopsfsConnectorFacade;
import io.hops.hopsworks.common.featurestore.storageconnectors.s3.FeaturestoreS3ConnectorFacade;
import io.hops.hopsworks.common.featurestore.trainingdatasets.external.ExternalTrainingDatasetController;
import io.hops.hopsworks.common.featurestore.trainingdatasets.external.ExternalTrainingDatasetFacade;
import io.hops.hopsworks.common.featurestore.trainingdatasets.hopsfs.HopsfsTrainingDatasetController;
import io.hops.hopsworks.common.featurestore.trainingdatasets.hopsfs.HopsfsTrainingDatasetFacade;
import io.hops.hopsworks.common.featurestore.trainingdatasets.split.TrainingDatasetSplitDTO;
import io.hops.hopsworks.common.featurestore.utils.FeaturestoreInputValidation;
import io.hops.hopsworks.common.featurestore.utils.FeaturestoreUtils;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.provenance.core.HopsFSProvenanceController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.hopsfs.FeaturestoreHopsfsConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.s3.FeaturestoreS3Connector;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetFeature;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetJoin;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetJoinCondition;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetType;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.external.ExternalTrainingDataset;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.hopsfs.HopsfsTrainingDataset;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.split.TrainingDatasetSplit;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.calcite.sql.JoinType;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;
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
  private JobFacade jobFacade;
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
  @EJB
  private StatisticsController statisticsController;
  @EJB
  private ConstructorController constructorController;
  @EJB
  private OnlineFeaturestoreController onlineFeaturestoreController;

  /**
   * Gets all trainingDatasets for a particular featurestore and project
   *
   * @param featurestore featurestore to query trainingDatasets for
   * @return list of XML/JSON DTOs of the trainingDatasets
   */
  public List<TrainingDatasetDTO> getTrainingDatasetsForFeaturestore(Featurestore featurestore)
      throws ServiceException {
    List<TrainingDatasetDTO> trainingDatasets = new ArrayList<>();
    for (TrainingDataset td : trainingDatasetFacade.findByFeaturestore(featurestore)) {
      trainingDatasets.add(convertTrainingDatasetToDTO(td));
    }

    return trainingDatasets;
  }

  /**
   * Converts a trainingDataset entity to a TrainingDataset DTO
   *
   * @param trainingDataset trainingDataset entity
   * @return JSON/XML DTO of the trainingDataset
   * @throws ServiceException
   */
  private TrainingDatasetDTO convertTrainingDatasetToDTO(TrainingDataset trainingDataset)
      throws ServiceException {
    TrainingDatasetDTO trainingDatasetDTO = new TrainingDatasetDTO(trainingDataset);

    String featurestoreName = featurestoreFacade.getHiveDbName(trainingDataset.getFeaturestore().getHiveDbId());
    trainingDatasetDTO.setFeaturestoreName(featurestoreName);

    // Set features
    List<TrainingDatasetFeature> tdFeatures = getFeaturesSorted(trainingDataset);
    Map<Integer, String> fsLookupTable = getFsLookupTableFeatures(tdFeatures);
    trainingDatasetDTO.setFeatures(tdFeatures
        .stream()
        .map(f -> new TrainingDatasetFeatureDTO(f.getName(), f.getType(),
            f.getFeatureGroup() != null ?
                // Don't call the full constructor as it sends also the statistics
                new FeaturegroupDTO(f.getFeatureGroup().getFeaturestore().getId(),
                    fsLookupTable.get(f.getFeatureGroup().getFeaturestore().getId()),
                    f.getFeatureGroup().getId(),
                    f.getFeatureGroup().getName(), f.getFeatureGroup().getVersion())
                : null,
            f.getIndex()))
        .collect(Collectors.toList()));

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
      throws FeaturestoreException, ProvenanceException, IOException, ServiceException {

    // if version not provided, get latest and increment
    if (trainingDatasetDTO.getVersion() == null) {
      // returns ordered list by desc version
      List<TrainingDataset> tdPrevious = trainingDatasetFacade.findByNameAndFeaturestoreOrderedDescVersion(
        trainingDatasetDTO.getName(), featurestore);
      if (tdPrevious != null && !tdPrevious.isEmpty()) {
        trainingDatasetDTO.setVersion(tdPrevious.get(0).getVersion() + 1);
      } else {
        trainingDatasetDTO.setVersion(1);
      }
    }

    // Check that training dataset doesn't already exists
    if (trainingDatasetFacade.findByNameVersionAndFeaturestore
        (trainingDatasetDTO.getName(), trainingDatasetDTO.getVersion(), featurestore)
        .isPresent()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_ALREADY_EXISTS, Level.FINE,
          "Training Dataset: " + trainingDatasetDTO.getName() + ", version: " + trainingDatasetDTO.getVersion());
    }
  
    // Verify input
    verifyTrainingDatasetInput(trainingDatasetDTO);
  
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

        inode = inodeController.getInodeAtPath(trainingDatasetPath);
        TrainingDatasetDTO completeTrainingDatasetDTO = createTrainingDatasetMetadata(user, featurestore,
          trainingDatasetDTO, hopsfsConnector, inode, s3Connector);
        fsProvenanceController.trainingDatasetAttachXAttr(trainingDatasetPath, completeTrainingDatasetDTO, udfso);
        return completeTrainingDatasetDTO;
      } finally {
        if (udfso != null) {
          dfs.closeDfsClient(udfso);
        }
      }
    } else {
      s3Connector = S3ConnectorFacade.findByIdAndFeaturestore(trainingDatasetDTO.getStorageConnectorId(), featurestore)
          .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.S3_CONNECTOR_NOT_FOUND,
              Level.FINE, "S3 connector: " + trainingDatasetDTO.getStorageConnectorId()));
      return createTrainingDatasetMetadata(user, featurestore, trainingDatasetDTO, hopsfsConnector,  inode,
        s3Connector);
    }
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
      throws FeaturestoreException, ServiceException {
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
    trainingDataset.setSeed(trainingDatasetDTO.getSeed());
    trainingDataset.setSplits(trainingDatasetDTO.getSplits().stream()
      .map(tdDTO -> new TrainingDatasetSplit(trainingDataset, tdDTO.getName(), tdDTO.getPercentage())).collect(
        Collectors.toList()));

    // set features/query
    setTrainingDatasetFeatures(trainingDatasetDTO, trainingDataset);

    trainingDatasetFacade.persist(trainingDataset);
  
    //Get jobs
    List<Jobs> jobs = getJobs(trainingDatasetDTO.getJobs(), featurestore.getProject());

    //Store jobs
    featurestoreJobFacade.insertJobs(trainingDataset, jobs);

    //Get final entity from the database
    return getTrainingDatasetWithNameVersionAndFeaturestore(featurestore, trainingDataset.getName(),
        trainingDataset.getVersion());
  }

  private void setTrainingDatasetFeatures(TrainingDatasetDTO trainingDatasetDTO, TrainingDataset trainingDataset)
      throws FeaturestoreException {
    if (trainingDatasetDTO.getQueryDTO() != null) {
      // The user has created a training dataset from a query object. Stored it so that it can be reused later
      setTrainingDatasetQuery(trainingDatasetDTO.getQueryDTO(), trainingDataset);
    } else if (trainingDatasetDTO.getFeatures() != null && !trainingDatasetDTO.getFeatures().isEmpty()) {
      // The user has created a training dataset from a dataframe. We can't make any assumption on the content of it
      // just store the schema (feature name + type)
      trainingDataset.setFeatures(getTrainingDatasetFeaturesDTO(trainingDatasetDTO.getFeatures(), trainingDataset));
    } else {
      // The user didn't specify a query nor a valid set of features. throw an exception
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_NO_SCHEMA,
          Level.FINE, "The training dataset doesn't have any feature");
    }
  }

  private void setTrainingDatasetQuery(QueryDTO queryDTO, TrainingDataset trainingDataset)
      throws FeaturestoreException {
    // Convert the queryDTO to the internal representation
    Query query = constructorController.convertQueryDTO(queryDTO, 0);
    trainingDataset.setQuery(true);
    // Convert the joins from the query object into training dataset joins
    List<TrainingDatasetJoin> tdJoins = collectJoins(query, trainingDataset);
    trainingDataset.setJoins(tdJoins);

    trainingDataset.setFeatures(collectFeatures(query, trainingDataset, 0, tdJoins, 0));
  }

  // Here we need to pass the list of training dataset joins so that we can rebuild the aliases.
  // and handle correctly the case in which a feature group is joined with itself.
  private List<TrainingDatasetFeature> collectFeatures(Query query, TrainingDataset trainingDataset,
                                                       int featureIndex, List<TrainingDatasetJoin> tdJoins,
                                                       int joinIndex) {
    List<TrainingDatasetFeature> features = new ArrayList<>();
    for (Feature f : query.getFeatures()) {
      features.add(new TrainingDatasetFeature(trainingDataset, tdJoins.get(joinIndex), query.getFeaturegroup(),
          f.getName(), f.getType(), featureIndex++));
    }

    if (query.getJoins() != null) {
      for (Join join : query.getJoins()) {
        joinIndex++;
        features.addAll(collectFeatures(join.getRightQuery(), trainingDataset, featureIndex, tdJoins, joinIndex));
      }
    }
    return features;
  }

  private List<TrainingDatasetJoin> collectJoins(Query query, TrainingDataset trainingDataset) {
    List<TrainingDatasetJoin> joins = new ArrayList<>();
    // add the first feature group
    int index = 0;
    joins.add(new TrainingDatasetJoin(trainingDataset, query.getFeaturegroup(), (short) 0, index++));

    if (query.getJoins() != null && !query.getJoins().isEmpty()) {
      for (Join join : query.getJoins()) {
        TrainingDatasetJoin tdJoin = new TrainingDatasetJoin(trainingDataset,
            join.getRightQuery().getFeaturegroup(),
            (short) join.getJoinType().ordinal(),
            index++);

        tdJoin.setConditions(collectJoinConditions(join, tdJoin));

        joins.add(tdJoin);
      }
    }

    return joins;
  }

  private List<TrainingDatasetJoinCondition> collectJoinConditions(Join join, TrainingDatasetJoin tdJoin) {
    if (join.getOn() != null)  {
      return join.getOn().stream()
          .map(f -> new TrainingDatasetJoinCondition(tdJoin, f.getName(), f.getName()))
          .collect(Collectors.toList());
    }

    return Streams.zip(join.getLeftOn().stream(), join.getRightOn().stream(),
      (left, right) -> new TrainingDatasetJoinCondition(tdJoin, left.getName(), right.getName()))
        .collect(Collectors.toList());
  }

  private List<TrainingDatasetFeature> getTrainingDatasetFeaturesDTO(List<TrainingDatasetFeatureDTO> featureList,
                                                                     TrainingDataset trainingDataset) {
    List<TrainingDatasetFeature> trainingDatasetFeatureList = new ArrayList<>();
    int index = 0;
    for (TrainingDatasetFeatureDTO f : featureList) {
      trainingDatasetFeatureList.add(
          new TrainingDatasetFeature(trainingDataset, f.getName(), f.getType(), index++));
    }

    return trainingDatasetFeatureList;
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
    throws FeaturestoreException, ServiceException {
    TrainingDataset trainingDataset = getTrainingDatasetById(featurestore, id);
    return convertTrainingDatasetToDTO(trainingDataset);
  }

  public TrainingDataset getTrainingDatasetById(Featurestore featurestore, Integer id) throws FeaturestoreException {
    return trainingDatasetFacade.findByIdAndFeaturestore(id, featurestore)
        .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_NOT_FOUND,
            Level.FINE, "trainingDatasetId: " + id));
  }

  /**
   * Retrieves a list of trainingDataset with a particular name from a particular feature store
   *
   * @param name name of the trainingDataset
   * @param featurestore the featurestore that the trainingDataset belongs to
   * @return XML/JSON DTO of the trainingDataset
   * @throws FeaturestoreException
   * @throws ServiceException
   */
  public List<TrainingDatasetDTO> getTrainingDatasetWithNameAndFeaturestore(Featurestore featurestore, String name)
      throws FeaturestoreException, ServiceException {
    List<TrainingDataset> trainingDatasetList = trainingDatasetFacade.findByNameAndFeaturestore(name, featurestore);
    if (trainingDatasetList == null || trainingDatasetList.isEmpty()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_NOT_FOUND,
          Level.FINE, "training dataset name : " + name);
    }

    List<TrainingDatasetDTO> trainingDatasetDTOS = new ArrayList<>();
    for (TrainingDataset td : trainingDatasetList) {
      trainingDatasetDTOS.add(convertTrainingDatasetToDTO(td));
    }

    return trainingDatasetDTOS;
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
        String name, Integer version) throws FeaturestoreException, ServiceException {

    Optional<TrainingDataset> trainingDataset =
        trainingDatasetFacade.findByNameVersionAndFeaturestore(name, version, featurestore);
    return convertTrainingDatasetToDTO(trainingDataset
        .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_NOT_FOUND,
            Level.FINE, "training dataset name : " + name)));
  }

  public String delete(Users user, Project project, Featurestore featurestore, Integer trainingDatasetId)
      throws FeaturestoreException {

    TrainingDataset trainingDataset =
        trainingDatasetFacade.findByIdAndFeaturestore(trainingDatasetId, featurestore)
        .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_NOT_FOUND,
            Level.FINE, "training dataset id:" + trainingDatasetId));

    featurestoreUtils.verifyUserRole(trainingDataset, featurestore, user, project);

    statisticsController.deleteStatistics(project, user, trainingDataset);
    trainingDatasetFacade.removeTrainingDataset(trainingDataset);

    // If the training datasets was an HopsFS Training Dataset, then remove also the directory
    if (trainingDataset.getTrainingDatasetType() == TrainingDatasetType.HOPSFS_TRAINING_DATASET) {
      String dsPath = inodeController.getPath(trainingDataset.getHopsfsTrainingDataset().getInode());
      String username = hdfsUsersBean.getHdfsUserName(project, user);

      DistributedFileSystemOps udfso = dfs.getDfsOps(username);
      try {
        // TODO(Fabio): if Data owner *In project* do operation as superuser
        udfso.rm(dsPath, true);
      } catch (IOException e) {

      } finally {
        if (udfso != null) {
          dfs.closeDfsClient(udfso);
        }
      }
    }

    return trainingDataset.getName();
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
      Featurestore featurestore, TrainingDatasetDTO trainingDatasetDTO) throws FeaturestoreException, ServiceException {
    TrainingDataset trainingDataset = verifyTrainingDatasetId(trainingDatasetDTO.getId(), featurestore);
  
    // Verify general entity related information
    featurestoreInputValidation.verifyUserInput(trainingDatasetDTO);

    //Get jobs
    List<Jobs> jobs = getJobs(trainingDatasetDTO.getJobs(), featurestore.getProject());
    //Store jobs
    featurestoreJobFacade.insertJobs(trainingDataset, jobs);

    // Update metadata
    trainingDataset.setDescription(trainingDatasetDTO.getDescription());
    trainingDatasetFacade.updateTrainingDatasetMetadata(trainingDataset);

    // Refetch the updated entry from the database
    TrainingDataset updatedTrainingDataset =
        trainingDatasetFacade.findByIdAndFeaturestore(trainingDatasetDTO.getId(), featurestore)
            .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_NOT_FOUND,
                Level.FINE, "training dataset id: " + trainingDatasetDTO.getId()));

    return convertTrainingDatasetToDTO(updatedTrainingDataset);
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
   * Verfiy user input split information
   *
   * @param trainingDatasetSplitDTOs the list of training dataset splits
   * @throws FeaturestoreException
   */
  private void verifyTrainingDatasetSplits(List<TrainingDatasetSplitDTO> trainingDatasetSplitDTOs)
    throws FeaturestoreException {
    if (trainingDatasetSplitDTOs != null && !trainingDatasetSplitDTOs.isEmpty()) {
      Pattern namePattern = FeaturestoreConstants.FEATURESTORE_REGEX;
      Set<String> splitNames = new HashSet<>();
      for (TrainingDatasetSplitDTO trainingDatasetSplitDTO : trainingDatasetSplitDTOs) {
        if (!namePattern.matcher(trainingDatasetSplitDTO.getName()).matches()) {
          throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_TRAINING_DATASET_SPLIT_NAME,
            Level.FINE, ", the provided training dataset split name " + trainingDatasetSplitDTO.getName() + " is " +
            "invalid. Split names can only contain lower case characters, numbers and underscores and cannot be " +
            "longer than " + FeaturestoreConstants.FEATURESTORE_ENTITY_NAME_MAX_LENGTH + " characters or empty.");
        }
        if (trainingDatasetSplitDTO.getPercentage() == null) {
          throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_TRAINING_DATASET_SPLIT_PERCENTAGE,
            Level.FINE, ", the provided training dataset split percentage is invalid. Percentages can only be numeric" +
            ". Weights will be normalized if they don’t sum up to 1.0.");
        }
        if (!splitNames.add(trainingDatasetSplitDTO.getName())) {
          throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_DUPLICATE_SPLIT_NAMES,
            Level.FINE, " The split names must be unique");
        }
      }
    }
  }
  
  /**
   * Verify training dataset specific input
   *
   * @param trainingDatasetDTO the provided user input
   * @throws FeaturestoreException
   */
  private void verifyTrainingDatasetInput(TrainingDatasetDTO trainingDatasetDTO)
    throws FeaturestoreException {
    // Verify general entity related information
    featurestoreInputValidation.verifyUserInput(trainingDatasetDTO);
    verifyTrainingDatasetType(trainingDatasetDTO.getTrainingDatasetType());
    verifyTrainingDatasetVersion(trainingDatasetDTO.getVersion());
    verifyTrainingDatasetDataFormat(trainingDatasetDTO.getDataFormat());
    verifyTrainingDatasetSplits(trainingDatasetDTO.getSplits());
  }


  /**
   * Reconstruct the query used to generate the training datset, fetching the features and the joins
   * in the proper order from the database.
   * @param featurestore
   * @param trainingDatasetId
   * @return
   * @throws FeaturestoreException
   */
  public Query getQuery(Featurestore featurestore, Integer trainingDatasetId) throws FeaturestoreException {
    TrainingDataset trainingDataset =
        trainingDatasetFacade.findByIdAndFeaturestore(trainingDatasetId, featurestore)
        .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_NOT_FOUND,
            Level.FINE, "Could not find training dataset with id: " + trainingDatasetId));

    if (!trainingDataset.isQuery()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_NO_QUERY,
          Level.FINE, "Inference vector is only available for datasets generated by queries");
    }

    List<TrainingDatasetJoin> joins = getJoinsSorted(trainingDataset);

    // Convert all the TrainingDatasetFeatures to QueryFeatures
    Map<Integer, String> fgAliasLookup = getAliasLookupTable(joins);

    List<TrainingDatasetFeature> tdFeatures = getFeaturesSorted(trainingDataset);

    // Check that all the feature groups still exists, if not throw a reasonable error
    if (tdFeatures.stream().anyMatch(j -> j.getFeatureGroup() == null)) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_QUERY_FG_DELETED, Level.FINE);
    }

    List<Feature> features = tdFeatures.stream()
        .map(f -> new Feature(f.getName(), fgAliasLookup.get(f.getTrainingDatasetJoin().getId())))
        .collect(Collectors.toList());

    // Keep a map feature store id -> feature store name
    Map<Integer, String> fsLookup = getFsLookupTableJoins(joins);

    Query query = new Query(
        fsLookup.get(joins.get(0).getFeatureGroup().getFeaturestore().getId()),
        onlineFeaturestoreController
            .getOnlineFeaturestoreDbName(joins.get(0).getFeatureGroup().getFeaturestore().getProject()),
        joins.get(0).getFeatureGroup(),
        fgAliasLookup.get(joins.get(0).getId()));

    query.setFeatures(features);

    // The first join is set
    query.setFeaturegroup(joins.get(0).getFeatureGroup());

    // Set the remaining feature groups as join
    List<Join> queryJoins = new ArrayList<>();
    for (int i = 1; i < joins.size(); i++) {
      // we can get the "left side" of the join by fetching the join at i-1, the counter starts at 1
      // so we are guarantee it exists
      queryJoins.add(getQueryJoin(joins.get(i-1), joins.get(i), fgAliasLookup, fsLookup));
    }
    query.setJoins(queryJoins);
    return query;
  }

  private Map<Integer, String> getAliasLookupTable(List<TrainingDatasetJoin> tdJoins) {
    // Keep a map of fg Id to fgAlias;
    int i = 0;
    Map<Integer, String> fgAlias = new HashMap<>();

    for (TrainingDatasetJoin tdJoin : tdJoins) {
      fgAlias.put(tdJoin.getId(), "fg" + i++);
    }

    return fgAlias;
  }

  // generally in a query there are several feature groups from the same feature store
  // instead of making a db query for each of it, build an hashmap once and use it while constructing the query
  private Map<Integer, String> getFsLookupTableJoins(List<TrainingDatasetJoin> tdJoins) {
    Map<Integer, String> fsLookup = new HashMap<>();
    for (TrainingDatasetJoin join : tdJoins) {
      if (!fsLookup.containsKey(join.getFeatureGroup().getFeaturestore().getId())) {
        fsLookup.put(join.getFeatureGroup().getFeaturestore().getId(),
            featurestoreFacade.getHiveDbName(join.getFeatureGroup().getFeaturestore().getHiveDbId()));
      }
    }

    return fsLookup;
  }

  private Map<Integer, String> getFsLookupTableFeatures(List<TrainingDatasetFeature> tdFeatures) {
    Map<Integer, String> fsLookup = new HashMap<>();
    for (TrainingDatasetFeature tdFeature : tdFeatures) {
      if (tdFeature.getFeatureGroup() != null &&
          !fsLookup.containsKey(tdFeature.getFeatureGroup().getFeaturestore().getId())) {
        fsLookup.put(tdFeature.getFeatureGroup().getFeaturestore().getId(),
            featurestoreFacade.getHiveDbName(tdFeature.getFeatureGroup().getFeaturestore().getHiveDbId()));
      }
    }

    return fsLookup;
  }

  private List<TrainingDatasetFeature> getFeaturesSorted(TrainingDataset trainingDataset) {
    return trainingDataset.getFeatures().stream()
        .sorted((t1, t2) -> {
          if (t1.getIndex() != null) {
            // compare based on index
            return t1.getIndex().compareTo(t2.getIndex());
          } else {
            // Old training dataset with no index. compare based on name
            return t1.getName().compareTo(t2.getName());
          }
        })
        .collect(Collectors.toList());
  }

  private List<TrainingDatasetJoin> getJoinsSorted(TrainingDataset trainingDataset) {
    return trainingDataset.getJoins().stream()
        .sorted(Comparator.comparing(TrainingDatasetJoin::getIndex))
        .collect(Collectors.toList());
  }

  // Rebuild query object so that the query constructor can be build the string
  private Join getQueryJoin(TrainingDatasetJoin leftTdJoin, TrainingDatasetJoin rightTdJoin,
                            Map<Integer, String> fgAliasLookup, Map<Integer, String> fsLookup) {

    // The query constructor will use the left side for the "AS" part of the query
    String leftAs = fgAliasLookup.get(leftTdJoin.getId());
    Query leftQuery = new Query(
        fsLookup.get(leftTdJoin.getFeatureGroup().getFeaturestore().getId()),
        onlineFeaturestoreController
            .getOnlineFeaturestoreDbName(leftTdJoin.getFeatureGroup().getFeaturestore().getProject()),
        leftTdJoin.getFeatureGroup(),
        leftAs);

    String rightAs = fgAliasLookup.get(rightTdJoin.getId());
    Query rightQuery = new Query(
        fsLookup.get(rightTdJoin.getFeatureGroup().getFeaturestore().getId()),
        onlineFeaturestoreController
            .getOnlineFeaturestoreDbName(rightTdJoin.getFeatureGroup().getFeaturestore().getProject()),
        rightTdJoin.getFeatureGroup(),
        rightAs);

    List<Feature> leftOn = rightTdJoin.getConditions().stream()
        .map(c -> new Feature(c.getLeftFeature(), leftAs))
        .collect(Collectors.toList());

    List<Feature> rightOn = rightTdJoin.getConditions().stream()
        .map(c -> new Feature(c.getRightFeature(), rightAs))
        .collect(Collectors.toList());

    JoinType joinType = JoinType.values()[rightTdJoin.getType()];
    return new Join(leftQuery, rightQuery, leftOn, rightOn, joinType);
  }
}
