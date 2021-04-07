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

import com.logicalclocks.shaded.com.google.common.collect.Streams;
import io.hops.hopsworks.common.featurestore.FeaturestoreConstants;
import io.hops.hopsworks.common.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.featurestore.FeaturestoreFacade;
import io.hops.hopsworks.common.featurestore.activity.FeaturestoreActivityFacade;
import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.feature.TrainingDatasetFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.online.OnlineFeaturegroupController;
import io.hops.hopsworks.common.featurestore.query.ServingPreparedStatementDTO;
import io.hops.hopsworks.common.featurestore.query.PreparedStatementParameterDTO;
import io.hops.hopsworks.common.featurestore.query.filter.Filter;
import io.hops.hopsworks.common.featurestore.query.filter.FilterController;
import io.hops.hopsworks.common.featurestore.query.filter.FilterLogic;
import io.hops.hopsworks.common.featurestore.query.filter.SqlFilterCondition;
import io.hops.hopsworks.common.featurestore.statistics.StatisticsController;
import io.hops.hopsworks.common.featurestore.online.OnlineFeaturestoreController;
import io.hops.hopsworks.common.featurestore.query.ConstructorController;
import io.hops.hopsworks.common.featurestore.query.Feature;
import io.hops.hopsworks.common.featurestore.query.Join;
import io.hops.hopsworks.common.featurestore.query.Query;
import io.hops.hopsworks.common.featurestore.query.QueryDTO;
import io.hops.hopsworks.common.featurestore.statistics.columns.StatisticColumnController;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreConnectorFacade;
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
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.provenance.core.HopsFSProvenanceController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.activity.FeaturestoreActivityMeta;
import io.hops.hopsworks.persistence.entity.featurestore.statistics.StatisticColumn;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.FeaturegroupType;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.TimeTravelFormat;
import io.hops.hopsworks.persistence.entity.featurestore.statistics.StatisticsConfig;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnectorType;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetFeature;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetJoin;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetJoinCondition;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetType;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.external.ExternalTrainingDataset;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.hopsfs.HopsfsTrainingDataset;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.split.TrainingDatasetSplit;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
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
  private HopsfsTrainingDatasetController hopsfsTrainingDatasetController;
  @EJB
  private HopsfsTrainingDatasetFacade hopsfsTrainingDatasetFacade;
  @EJB
  private ExternalTrainingDatasetController externalTrainingDatasetController;
  @EJB
  private ExternalTrainingDatasetFacade externalTrainingDatasetFacade;
  @EJB
  private FeaturestoreInputValidation featurestoreInputValidation;
  @EJB
  private InodeController inodeController;
  @EJB
  private HopsFSProvenanceController fsProvenanceController;
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
  @EJB
  private FeaturegroupController featuregroupController;
  @EJB
  private FeaturestoreConnectorFacade featurestoreConnectorFacade;
  @EJB
  private FeaturestoreActivityFacade fsActivityFacade;
  @EJB
  private StatisticColumnController statisticColumnController;
  @EJB
  private FilterController filterController;
  @EJB
  private FeaturestoreController featurestoreController;
  @EJB
  private OnlineFeaturegroupController onlineFeaturegroupController;


  // this is used to overwrite feature type in prepared statement
  private final static String PREPARED_STATEMENT_TYPE =  "parameter";

  /**
   * Gets all trainingDatasets for a particular featurestore and project
   *
   * @param featurestore featurestore to query trainingDatasets for
   * @return list of XML/JSON DTOs of the trainingDatasets
   */
  public List<TrainingDatasetDTO> getTrainingDatasetsForFeaturestore(Users user, Project project,
                                                                     Featurestore featurestore)
      throws ServiceException, FeaturestoreException {
    List<TrainingDatasetDTO> trainingDatasets = new ArrayList<>();
    for (TrainingDataset td : trainingDatasetFacade.findByFeaturestore(featurestore)) {
      trainingDatasets.add(convertTrainingDatasetToDTO(user, project, td));
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
  private TrainingDatasetDTO convertTrainingDatasetToDTO(Users user, Project project, TrainingDataset trainingDataset)
      throws ServiceException, FeaturestoreException {
    TrainingDatasetDTO trainingDatasetDTO = new TrainingDatasetDTO(trainingDataset);

    String featurestoreName = featurestoreFacade.getHiveDbName(trainingDataset.getFeaturestore().getHiveDbId());
    trainingDatasetDTO.setFeaturestoreName(featurestoreName);

    // Set features
    List<TrainingDatasetFeature> tdFeatures = getFeaturesSorted(trainingDataset, true);
    Map<Integer, String> fsLookupTable = getFsLookupTableFeatures(tdFeatures);
    trainingDatasetDTO.setFeatures(tdFeatures
        .stream()
        .map(f -> new TrainingDatasetFeatureDTO(f.getName(), f.getType(),
            f.getFeatureGroup() != null ?
                new FeaturegroupDTO(f.getFeatureGroup().getFeaturestore().getId(),
                    fsLookupTable.get(f.getFeatureGroup().getFeaturestore().getId()),
                    f.getFeatureGroup().getId(),
                    f.getFeatureGroup().getName(), f.getFeatureGroup().getVersion(),
                    onlineFeaturegroupController.onlineFeatureGroupTopicName(project.getId(),
                      Utils.getFeaturegroupName(f.getFeatureGroup())))
                : null,
            f.getIndex(), f.isLabel()))
        .collect(Collectors.toList()));

    switch (trainingDataset.getTrainingDatasetType()) {
      case HOPSFS_TRAINING_DATASET:
        return hopsfsTrainingDatasetController.convertHopsfsTrainingDatasetToDTO(trainingDatasetDTO, trainingDataset);
      case EXTERNAL_TRAINING_DATASET:
        return externalTrainingDatasetController.convertExternalTrainingDatasetToDTO(user, project,
            trainingDatasetDTO, trainingDataset);
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

    // If the training dataset is constructed from a query, verify that it compiles correctly
    Query query = null;
    if (trainingDatasetDTO.getQueryDTO() != null) {
      query = constructQuery(trainingDatasetDTO.getQueryDTO(), project, user);
    } else if (trainingDatasetDTO.getFeatures() == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_NO_SCHEMA,
          Level.FINE, "The training dataset doesn't have any feature");
    }
  
    // Verify input
    verifyTrainingDatasetInput(trainingDatasetDTO, query);

    Inode inode = null;
    FeaturestoreConnector featurestoreConnector;
    if(trainingDatasetDTO.getTrainingDatasetType() == TrainingDatasetType.HOPSFS_TRAINING_DATASET) {
      if (trainingDatasetDTO.getStorageConnector() != null &&
          trainingDatasetDTO.getStorageConnector().getId() != null) {
        featurestoreConnector = featurestoreConnectorFacade
            .findByIdType(trainingDatasetDTO.getStorageConnector().getId(), FeaturestoreConnectorType.HOPSFS)
            .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.HOPSFS_CONNECTOR_NOT_FOUND,
                Level.FINE, "HOPSFS Connector: " + trainingDatasetDTO.getStorageConnector().getId()));
      } else {
        String connectorName =
            featurestore.getProject().getName() + "_" + Settings.ServiceDataset.TRAININGDATASETS.getName();
        featurestoreConnector = featurestoreConnectorFacade.findByFeaturestoreName(featurestore, connectorName)
            .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.HOPSFS_CONNECTOR_NOT_FOUND,
                Level.FINE, "HOPSFS Connector: " + connectorName));
      }

      Dataset trainingDatasetsFolder = featurestoreConnector.getHopsfsConnector().getHopsfsDataset();

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
        TrainingDatasetDTO completeTrainingDatasetDTO = createTrainingDatasetMetadata(user, project,
            featurestore, trainingDatasetDTO, query, featurestoreConnector, inode);
        fsProvenanceController.trainingDatasetAttachXAttr(trainingDatasetPath, completeTrainingDatasetDTO, udfso);
        return completeTrainingDatasetDTO;
      } finally {
        if (udfso != null) {
          dfs.closeDfsClient(udfso);
        }
      }
    } else {
      if (trainingDatasetDTO.getStorageConnector() == null) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.CONNECTOR_NOT_FOUND,
            Level.FINE, "Storage connector is empty");
      }

      featurestoreConnector = featurestoreConnectorFacade
          .findById(trainingDatasetDTO.getStorageConnector().getId())
          .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.CONNECTOR_NOT_FOUND,
              Level.FINE, "Connector: " + trainingDatasetDTO.getStorageConnector().getId()));
      return createTrainingDatasetMetadata(user, project, featurestore, trainingDatasetDTO,
          query, featurestoreConnector, null);
    }
  }

  /**
   * Creates the metadata structure in DB for the training dataset
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  private TrainingDatasetDTO createTrainingDatasetMetadata(Users user, Project project, Featurestore featurestore,
                                                           TrainingDatasetDTO trainingDatasetDTO, Query query,
                                                           FeaturestoreConnector featurestoreConnector, Inode inode)
      throws FeaturestoreException, ServiceException {
    //Create specific dataset type
    HopsfsTrainingDataset hopsfsTrainingDataset = null;
    ExternalTrainingDataset externalTrainingDataset = null;
    switch (trainingDatasetDTO.getTrainingDatasetType()) {
      case HOPSFS_TRAINING_DATASET:
        hopsfsTrainingDataset =
            hopsfsTrainingDatasetFacade.createHopsfsTrainingDataset(featurestoreConnector, inode);
        break;
      case EXTERNAL_TRAINING_DATASET:
        externalTrainingDataset = externalTrainingDatasetFacade.createExternalTrainingDataset(featurestoreConnector,
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
    trainingDataset.setCoalesce(trainingDatasetDTO.getCoalesce() != null ? trainingDatasetDTO.getCoalesce() : false);

    StatisticsConfig statisticsConfig = new StatisticsConfig(trainingDatasetDTO.getStatisticsConfig().getEnabled(),
      trainingDatasetDTO.getStatisticsConfig().getCorrelations(),
      trainingDatasetDTO.getStatisticsConfig().getHistograms());
    statisticsConfig.setTrainingDataset(trainingDataset);
    statisticsConfig.setStatisticColumns(trainingDatasetDTO.getStatisticsConfig().getColumns().stream()
      .map(sc -> new StatisticColumn(statisticsConfig, sc)).collect(Collectors.toList()));
    trainingDataset.setStatisticsConfig(statisticsConfig);

    // set features/query
    trainingDataset.setQuery(trainingDatasetDTO.getQueryDTO() != null);
    if (trainingDataset.isQuery()) {
      setTrainingDatasetQuery(query, trainingDatasetDTO.getFeatures(), trainingDataset);
    } else {
      trainingDataset.setFeatures(getTrainingDatasetFeatures(trainingDatasetDTO.getFeatures(), trainingDataset));
    }

    TrainingDataset dbTrainingDataset = trainingDatasetFacade.update(trainingDataset);

    // Log the metadata operation
    fsActivityFacade.logMetadataActivity(user, dbTrainingDataset, FeaturestoreActivityMeta.TD_CREATED);

    //Get final entity from the database
    return convertTrainingDatasetToDTO(user, project, dbTrainingDataset);
  }


  private Query constructQuery(QueryDTO queryDTO, Project project, Users user) throws FeaturestoreException {
    // Convert the queryDTO to the internal representation
    Map<Integer, String> fgAliasLookup = new HashMap<>();
    Map<Integer, Featuregroup> fgLookup = new HashMap<>();
    Map<Integer, List<Feature>> availableFeatureLookup = new HashMap<>();

    constructorController.populateFgLookupTables(queryDTO, 0, fgAliasLookup, fgLookup, availableFeatureLookup,
        project, user);
    return constructorController.convertQueryDTO(queryDTO, fgAliasLookup, fgLookup, availableFeatureLookup);
  }

  private void setTrainingDatasetQuery(Query query,
                                       List<TrainingDatasetFeatureDTO> features,
                                       TrainingDataset trainingDataset) {
    // Convert the joins from the query object into training dataset joins
    List<TrainingDatasetJoin> tdJoins = collectJoins(query, trainingDataset);
    trainingDataset.setJoins(tdJoins);
    trainingDataset.setFeatures(collectFeatures(query, features, trainingDataset, 0, tdJoins, 0));
  }

  // Here we need to pass the list of training dataset joins so that we can rebuild the aliases.
  // and handle correctly the case in which a feature group is joined with itself.
  private List<TrainingDatasetFeature> collectFeatures(Query query, List<TrainingDatasetFeatureDTO> featureDTOs,
                                                       TrainingDataset trainingDataset, int featureIndex,
                                                       List<TrainingDatasetJoin> tdJoins, int joinIndex) {
    List<TrainingDatasetFeature> features = new ArrayList<>();
    boolean isLabel = false;
    for (Feature f : query.getFeatures()) {
      if (featureDTOs != null && !featureDTOs.isEmpty()) {
        isLabel = featureDTOs.stream().anyMatch(dto -> f.getName().equals(dto.getName()) && dto.getLabel());
      }
      features.add(new TrainingDatasetFeature(trainingDataset, tdJoins.get(joinIndex), query.getFeaturegroup(),
          f.getName(), f.getType(), featureIndex++, isLabel));
    }

    if (query.getJoins() != null) {
      for (Join join : query.getJoins()) {
        joinIndex++;
        List<TrainingDatasetFeature> joinFeatures
            = collectFeatures(join.getRightQuery(), featureDTOs, trainingDataset, featureIndex, tdJoins, joinIndex);
        features.addAll(joinFeatures);
        featureIndex += joinFeatures.size();
      }
    }
    return features;
  }

  private List<TrainingDatasetJoin> collectJoins(Query query, TrainingDataset trainingDataset) {
    List<TrainingDatasetJoin> joins = new ArrayList<>();
    // add the first feature group
    int index = 0;
    if (query.getFeaturegroup().getFeaturegroupType() == FeaturegroupType.CACHED_FEATURE_GROUP &&
        query.getFeaturegroup().getCachedFeaturegroup().getTimeTravelFormat() == TimeTravelFormat.HUDI){
      joins.add(new TrainingDatasetJoin(trainingDataset, query.getFeaturegroup(),
          query.getLeftFeatureGroupEndCommitId() , (short) 0, index++));
    } else {
      joins.add(new TrainingDatasetJoin(trainingDataset, query.getFeaturegroup(), (short) 0, index++));
    }

    if (query.getJoins() != null && !query.getJoins().isEmpty()) {
      for (Join join : query.getJoins()) {
        TrainingDatasetJoin tdJoin;
        if (query.getFeaturegroup().getFeaturegroupType() == FeaturegroupType.CACHED_FEATURE_GROUP &&
            query.getFeaturegroup().getCachedFeaturegroup().getTimeTravelFormat() == TimeTravelFormat.HUDI){
          tdJoin = new TrainingDatasetJoin(trainingDataset,
              join.getRightQuery().getFeaturegroup(), join.getRightQuery().getLeftFeatureGroupEndCommitId(),
              (short) join.getJoinType().ordinal(),
              index++);
        } else {
          tdJoin = new TrainingDatasetJoin(trainingDataset,
              join.getRightQuery().getFeaturegroup(),
              (short) join.getJoinType().ordinal(),
              index++);
        }
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

  private List<TrainingDatasetFeature> getTrainingDatasetFeatures(List<TrainingDatasetFeatureDTO> featureList,
                                                                  TrainingDataset trainingDataset) {
    List<TrainingDatasetFeature> trainingDatasetFeatureList = new ArrayList<>();
    int index = 0;
    for (TrainingDatasetFeatureDTO f : featureList) {
      trainingDatasetFeatureList.add(
          new TrainingDatasetFeature(trainingDataset, f.getName(), f.getType(), index++, f.getLabel()));
    }

    return trainingDatasetFeatureList;
  }

  public TrainingDatasetDTO getTrainingDatasetWithIdAndFeaturestore(Users user, Project project,
                                                                    Featurestore featurestore, Integer id)
    throws FeaturestoreException, ServiceException {
    TrainingDataset trainingDataset = getTrainingDatasetById(featurestore, id);
    return convertTrainingDatasetToDTO(user, project, trainingDataset);
  }

  public TrainingDataset getTrainingDatasetById(Featurestore featurestore, Integer id) throws FeaturestoreException {
    return trainingDatasetFacade.findByIdAndFeaturestore(id, featurestore)
        .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_NOT_FOUND,
            Level.FINE, "trainingDatasetId: " + id));
  }

  public List<TrainingDatasetDTO> getWithNameAndFeaturestore(Users user, Project project,
                                                             Featurestore featurestore, String name)
      throws FeaturestoreException, ServiceException {
    List<TrainingDataset> trainingDatasetList = trainingDatasetFacade.findByNameAndFeaturestore(name, featurestore);
    if (trainingDatasetList == null || trainingDatasetList.isEmpty()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_NOT_FOUND,
          Level.FINE, "training dataset name : " + name);
    }

    List<TrainingDatasetDTO> trainingDatasetDTOS = new ArrayList<>();
    for (TrainingDataset td : trainingDatasetList) {
      trainingDatasetDTOS.add(convertTrainingDatasetToDTO(user, project, td));
    }

    return trainingDatasetDTOS;
  }

  public TrainingDatasetDTO getWithNameVersionAndFeaturestore(Users user, Project project, Featurestore featurestore,
                                                              String name, Integer version)
      throws FeaturestoreException, ServiceException {

    Optional<TrainingDataset> trainingDataset =
        trainingDatasetFacade.findByNameVersionAndFeaturestore(name, version, featurestore);
    return convertTrainingDatasetToDTO(user, project, trainingDataset
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
  public TrainingDatasetDTO updateTrainingDatasetMetadata(Users user, Project project,
                                                          Featurestore featurestore,
                                                          TrainingDatasetDTO trainingDatasetDTO)
      throws FeaturestoreException, ServiceException {
    TrainingDataset trainingDataset = verifyTrainingDatasetId(trainingDatasetDTO.getId(), featurestore);
  
    // Verify general entity related information
    featurestoreInputValidation.verifyUserInput(trainingDatasetDTO);

    // Update metadata
    trainingDataset.setDescription(trainingDatasetDTO.getDescription());
    trainingDatasetFacade.update(trainingDataset);

    // Refetch the updated entry from the database
    TrainingDataset updatedTrainingDataset =
        trainingDatasetFacade.findByIdAndFeaturestore(trainingDatasetDTO.getId(), featurestore)
            .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_NOT_FOUND,
                Level.FINE, "training dataset id: " + trainingDatasetDTO.getId()));

    return convertTrainingDatasetToDTO(user, project, updatedTrainingDataset);
  }

  public TrainingDatasetDTO updateTrainingDatasetStatsConfig(Users user, Project project, Featurestore featurestore,
                                                             TrainingDatasetDTO trainingDatasetDTO)
      throws FeaturestoreException, ServiceException {
    TrainingDataset trainingDataset = getTrainingDatasetById(featurestore, trainingDatasetDTO.getId());
    if (trainingDatasetDTO.getStatisticsConfig().getEnabled() != null) {
      trainingDataset.getStatisticsConfig().setDescriptive(trainingDatasetDTO.getStatisticsConfig().getEnabled());
    }
    if (trainingDatasetDTO.getStatisticsConfig().getHistograms() != null) {
      trainingDataset.getStatisticsConfig().setHistograms(trainingDatasetDTO.getStatisticsConfig().getHistograms());
    }
    if (trainingDatasetDTO.getStatisticsConfig().getCorrelations() != null) {
      trainingDataset.getStatisticsConfig().setCorrelations(trainingDatasetDTO.getStatisticsConfig().getCorrelations());
    }
    // compare against schema from database, as client doesn't need to send schema in update request
    statisticColumnController.verifyStatisticColumnsExist(trainingDatasetDTO, trainingDataset);
    trainingDataset = trainingDatasetFacade.update(trainingDataset);
    statisticColumnController
      .persistStatisticColumns(trainingDataset, trainingDatasetDTO.getStatisticsConfig().getColumns());
    // get feature group again with persisted columns - this trip to the database can be saved
    trainingDataset = getTrainingDatasetById(featurestore, trainingDatasetDTO.getId());
    return convertTrainingDatasetToDTO(user, project, trainingDataset);
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

  // Collect Features for verification
  private List<Feature> collectFeatures(Query query) {
    List<Feature> features = new ArrayList<>(query.getFeatures());
    if (query.getJoins() != null) {
      for (Join join : query.getJoins()) {
        features.addAll(collectFeatures(join.getRightQuery()));
      }
    }

    return features;
  }

  private void verifyFeatures(Query query, List<TrainingDatasetFeatureDTO> featuresDTOs) throws FeaturestoreException {
    if (query == null || featuresDTOs == null) {
      // If the query is null the features are taken from the featuresDTO, so we are guarantee that the label
      // features exists
      // if the featuresDTOs is null and the query is not, the the user didn't specify a label object, no validation
      // needed.
      return;
    }

    List<TrainingDatasetFeatureDTO> labels = featuresDTOs.stream()
        .filter(TrainingDatasetFeatureDTO::getLabel)
        .collect(Collectors.toList());
    List<Feature> features = collectFeatures(query);

    for (TrainingDatasetFeatureDTO label : labels) {
      if (features.stream().noneMatch(f -> f.getName().equals(label.getName()))) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.LABEL_NOT_FOUND, Level.FINE,
            "Label: " + label.getName() + " is missing");
      }
    }
  }
  
  /**
   * Verify training dataset specific input
   */
  private void verifyTrainingDatasetInput(TrainingDatasetDTO trainingDatasetDTO, Query query)
      throws FeaturestoreException {
    // Verify general entity related information
    featurestoreInputValidation.verifyUserInput(trainingDatasetDTO);
    statisticColumnController.verifyStatisticColumnsExist(trainingDatasetDTO, query);
    verifyTrainingDatasetType(trainingDatasetDTO.getTrainingDatasetType());
    verifyTrainingDatasetVersion(trainingDatasetDTO.getVersion());
    verifyTrainingDatasetDataFormat(trainingDatasetDTO.getDataFormat());
    verifyTrainingDatasetSplits(trainingDatasetDTO.getSplits());
    verifyFeatures(query, trainingDatasetDTO.getFeatures());
  }


  /**
   * Reconstruct the query used to generate the training datset, fetching the features and the joins
   * in the proper order from the database.
   * @param trainingDataset
   * @return
   * @throws FeaturestoreException
   */
  public Query getQuery(TrainingDataset trainingDataset, boolean withLabel, Project project,
                        Users user) throws FeaturestoreException {

    if (!trainingDataset.isQuery()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_NO_QUERY,
          Level.FINE, "Inference vector is only available for datasets generated by queries");
    }

    List<TrainingDatasetJoin> joins = getJoinsSorted(trainingDataset);

    // Convert all the TrainingDatasetFeatures to QueryFeatures
    Map<Integer, String> fgAliasLookup = getAliasLookupTable(joins);

    // These features are for the select part and are from different feature groups
    // to respect the ordering, all selected features are added to the left most Query instead of splitting them
    // over the querys for their respective origin feature group
    List<TrainingDatasetFeature> tdFeatures = getFeaturesSorted(trainingDataset, withLabel);

    // Check that all the feature groups still exists, if not throw a reasonable error
    if (tdFeatures.stream().anyMatch(j -> j.getFeatureGroup() == null)) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_QUERY_FG_DELETED, Level.FINE);
    }

    // Get available features for all involved feature groups once, and save in map fgId -> availableFeatures
    Map<Integer, List<Feature>> availableFeaturesLookup = new HashMap<>();
    for (TrainingDatasetJoin join : joins) {
      if (!availableFeaturesLookup.containsKey(join.getFeatureGroup().getId())) {
        List<Feature> availableFeatures = featuregroupController.getFeatures(join.getFeatureGroup(), project, user)
          .stream()
          .map(f -> new Feature(f.getName(),
            fgAliasLookup.get(join.getId()), f.getType(), f.getPrimary(), f.getDefaultValue()))
          .collect(Collectors.toList());
        availableFeaturesLookup.put(join.getFeatureGroup().getId(), availableFeatures);
      }
    }
  
    List<Feature> features = new ArrayList<>();
    for (TrainingDatasetFeature requestedFeature : tdFeatures) {
      features.add(availableFeaturesLookup.get(requestedFeature.getFeatureGroup().getId())
        .stream()
        .filter(af -> af.getName().equals(requestedFeature.getName()))
        // instantiate new feature since alias in available feature is not correct if fg is joined with itself
        .map(af -> new Feature(af.getName(), fgAliasLookup.get(requestedFeature.getTrainingDatasetJoin().getId()),
          af.getType(), af.getDefaultValue()))
        .findFirst()
        .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURE_DOES_NOT_EXIST, Level.FINE,
          "Feature: " + requestedFeature.getName() + " not found in feature group: " +
            requestedFeature.getFeatureGroup().getName())));
    }

    // Keep a map feature store id -> feature store name
    Map<Integer, String> fsLookup = getFsLookupTableJoins(joins);

    Query query = new Query(
      fsLookup.get(joins.get(0).getFeatureGroup().getFeaturestore().getId()),
        onlineFeaturestoreController
          .getOnlineFeaturestoreDbName(joins.get(0).getFeatureGroup().getFeaturestore().getProject()),
      joins.get(0).getFeatureGroup(),
      fgAliasLookup.get(joins.get(0).getId()),
      features,
      availableFeaturesLookup.get(joins.get(0).getFeatureGroup().getId()));

    // Set the remaining feature groups as join
    List<Join> queryJoins = new ArrayList<>();
    for (int i = 1; i < joins.size(); i++) {
      // left side of the join stays fixed, the counter starts at 1
      queryJoins.add(getQueryJoin(query, joins.get(i), fgAliasLookup, fsLookup, availableFeaturesLookup));
    }
    query.setJoins(queryJoins);
    return query;
  }

  /**
   * Construct jdbc prepared statement for online feature store
   * @param trainingDataset
   * @param project
   * @param user
   * @return
   * @throws FeaturestoreException
   */

  public List<ServingPreparedStatementDTO> getPreparedStatementDTO(TrainingDataset trainingDataset, Project project,
                                                                   Users user) throws FeaturestoreException {

    if (!trainingDataset.isQuery()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_NO_QUERY,
          Level.FINE, "Inference vector is only available for datasets generated by queries");
    }

    List<TrainingDatasetJoin> joins = getJoinsSorted(trainingDataset);

    List<TrainingDatasetFeature> tdFeatures = getFeaturesSorted(trainingDataset, false);

    // Check that all the feature groups still exists, if not throw a reasonable error
    if (tdFeatures.stream().anyMatch(j -> j.getFeatureGroup() == null)) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_QUERY_FG_DELETED, Level.FINE);
    }

    List<ServingPreparedStatementDTO> servingPreparedStatementDTOS = new ArrayList<>();

    // Iterate over each feature group
    for (TrainingDatasetJoin join : joins) {
      Featuregroup featuregroup = join.getFeatureGroup();

      if (!featuregroup.getCachedFeaturegroup().isOnlineEnabled()){
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ONLINE_NOT_ENABLED,
            Level.FINE, "Inference vector is only available for training datasets generated by online enabled " +
            "feature groups");
      }

      // Identify and create primary key features for this feature group. Primary key features may not be the part of
      //  query that generated the training dataset.
      List<FeatureGroupFeatureDTO> availableFeatures = featuregroupController.getFeatures(join.getFeatureGroup(),
          project, user);
      List<Feature> primaryKeys =  availableFeatures.stream().filter(FeatureGroupFeatureDTO::getPrimary)
          .map(af -> new Feature(af.getName(), "fg0", af.getType(), af.getPrimary(), af.getDefaultValue()))
          .collect(Collectors.toList());
      if (primaryKeys.size() == 0 ) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.PRIMARY_KEY_REQUIRED,
            Level.FINE, "Inference vector is only available for training datasets generated by feature groups with " +
            "at least 1 primary key");
      }

      // create td features
      List<String> tdFeaturesNames = tdFeatures.stream()
          .filter(tdf -> tdf.getTrainingDatasetJoin().getId().equals(join.getId())).map(tdf -> tdf.getName())
          .collect(Collectors.toList());
      List<Feature> features =  availableFeatures.stream().filter(af -> tdFeaturesNames.contains(af.getName()))
          .map(af -> new Feature(af.getName(), "fg0", af.getType(), af.getPrimary(), af.getDefaultValue()))
          .collect(Collectors.toList());

      // construct query for this feature group
      String featureStore = featurestoreController.getOfflineFeaturestoreDbName(project);
      String projectName = onlineFeaturestoreController.getOnlineFeaturestoreDbName(
          featuregroup.getFeaturestore().getProject());
      Query query = new Query();
      query.setFeatureStore(featureStore);
      query.setProject(projectName);
      query.setFeaturegroup(featuregroup);
      query.setAs("fg0");
      query.setFeatures(features);

      // construct ServingPreparedStatementDTO and add to the list
      servingPreparedStatementDTOS.add(buildServingPreparedStatementDTO(join.getIndex(), primaryKeys, query));
    }

    return servingPreparedStatementDTOS;
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

  private List<TrainingDatasetFeature> getFeaturesSorted(TrainingDataset trainingDataset, boolean withLabel) {
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
        // drop label features if desired
        .filter(f -> !f.isLabel() || withLabel)
        .collect(Collectors.toList());
  }

  private List<TrainingDatasetJoin> getJoinsSorted(TrainingDataset trainingDataset) {
    return trainingDataset.getJoins().stream()
        .sorted(Comparator.comparing(TrainingDatasetJoin::getIndex))
        .collect(Collectors.toList());
  }

  // Rebuild query object so that the query constructor can be build the string
  private Join getQueryJoin(Query leftQuery, TrainingDatasetJoin rightTdJoin, Map<Integer, String> fgAliasLookup,
    Map<Integer, String> fsLookup, Map<Integer, List<Feature>> availableFeaturesLookup) throws FeaturestoreException {

    String rightAs = fgAliasLookup.get(rightTdJoin.getId());
    Query rightQuery = new Query(
      fsLookup.get(rightTdJoin.getFeatureGroup().getFeaturestore().getId()),
      onlineFeaturestoreController
        .getOnlineFeaturestoreDbName(rightTdJoin.getFeatureGroup().getFeaturestore().getProject()),
      rightTdJoin.getFeatureGroup(),
      rightAs,
      // no requested features as they are all in the left base query
      null,
      availableFeaturesLookup.get(rightTdJoin.getFeatureGroup().getId()));

    List<Feature> leftOn = rightTdJoin.getConditions().stream()
      .map(c -> new Feature(c.getLeftFeature())).collect(Collectors.toList());

    List<Feature> rightOn = rightTdJoin.getConditions().stream()
      .map(c -> new Feature(c.getRightFeature())).collect(Collectors.toList());

    JoinType joinType = JoinType.values()[rightTdJoin.getType()];
    return constructorController.extractLeftRightOn(leftQuery, rightQuery, leftOn, rightOn, joinType);
  }

  private ServingPreparedStatementDTO buildServingPreparedStatementDTO(Integer preparedStatementIndex,
                                                                       List<Feature> primaryKeys, Query query)
      throws FeaturestoreException {

    // create primary key prepared statement filters for the query
    List<PreparedStatementParameterDTO> preparedStatementParameterDTOS = new ArrayList<>();

    // define filter logic for prepared statement
    FilterLogic filterLogic;
    // record pk position in the prepared statement
    Integer primaryKeyIndex = 1;
    Feature pkFeature;
    pkFeature = primaryKeys.get(0);
    pkFeature.setType(PREPARED_STATEMENT_TYPE);
    filterLogic =  new FilterLogic(new Filter(pkFeature, SqlFilterCondition.EQUALS, "?"));
    preparedStatementParameterDTOS.add( new PreparedStatementParameterDTO(primaryKeys.get(0).getName(),
        primaryKeyIndex++));
    for (int i = 1; i < primaryKeys.size(); i++) {
      pkFeature = primaryKeys.get(i);
      pkFeature.setType(PREPARED_STATEMENT_TYPE);
      filterLogic = filterLogic.and(new Filter(pkFeature, SqlFilterCondition.EQUALS, "?"));
      preparedStatementParameterDTOS.add(new PreparedStatementParameterDTO(primaryKeys.get(i).getName(),
          primaryKeyIndex++));
    }
    query.setFilter(filterLogic);

    // set prepared statement parameters
    ServingPreparedStatementDTO servingPreparedStatementDTO = new ServingPreparedStatementDTO();
    servingPreparedStatementDTO.setPreparedStatementIndex(preparedStatementIndex);
    servingPreparedStatementDTO.setQueryOnline(constructorController.generateSQL(query, true));
    servingPreparedStatementDTO.setPreparedStatementParameters(preparedStatementParameterDTOS);
    return servingPreparedStatementDTO;
  }
}
