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

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.logicalclocks.shaded.com.google.common.collect.Streams;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.featurestore.FeaturestoreFacade;
import io.hops.hopsworks.common.featurestore.activity.FeaturestoreActivityFacade;
import io.hops.hopsworks.common.featurestore.feature.TrainingDatasetFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.online.OnlineFeaturegroupController;
import io.hops.hopsworks.common.featurestore.online.OnlineFeaturestoreController;
import io.hops.hopsworks.common.featurestore.query.Feature;
import io.hops.hopsworks.common.featurestore.query.Query;
import io.hops.hopsworks.common.featurestore.query.QueryController;
import io.hops.hopsworks.common.featurestore.query.QueryDTO;
import io.hops.hopsworks.common.featurestore.query.filter.Filter;
import io.hops.hopsworks.common.featurestore.query.filter.FilterController;
import io.hops.hopsworks.common.featurestore.query.filter.FilterLogic;
import io.hops.hopsworks.common.featurestore.query.filter.FilterValue;
import io.hops.hopsworks.common.featurestore.query.join.Join;
import io.hops.hopsworks.common.featurestore.query.pit.PitJoinController;
import io.hops.hopsworks.common.featurestore.statistics.StatisticsController;
import io.hops.hopsworks.common.featurestore.statistics.columns.StatisticColumnController;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreConnectorFacade;
import io.hops.hopsworks.common.featurestore.trainingdatasets.external.ExternalTrainingDatasetController;
import io.hops.hopsworks.common.featurestore.trainingdatasets.hopsfs.HopsfsTrainingDatasetController;
import io.hops.hopsworks.common.featurestore.trainingdatasets.hopsfs.HopsfsTrainingDatasetFacade;
import io.hops.hopsworks.common.featurestore.transformationFunction.TransformationFunctionFacade;
import io.hops.hopsworks.common.featurestore.utils.FeaturestoreUtils;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.provenance.core.HopsFSProvenanceController;
import io.hops.hopsworks.common.security.QuotasEnforcement;
import io.hops.hopsworks.common.security.QuotaEnforcementException;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.activity.FeaturestoreActivityMeta;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.FeaturegroupType;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.TimeTravelFormat;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.statistics.StatisticColumn;
import io.hops.hopsworks.persistence.entity.featurestore.statistics.StatisticsConfig;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnectorType;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.SqlFilterLogic;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetFeature;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetFilter;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetFilterCondition;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetJoin;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetJoinCondition;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetType;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.external.ExternalTrainingDataset;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.hopsfs.HopsfsTrainingDataset;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.split.TrainingDatasetSplit;
import io.hops.hopsworks.persistence.entity.featurestore.transformationFunction.TransformationFunction;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.activity.ActivityFlag;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.calcite.sql.JoinType;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.split.SplitType.RANDOM_SPLIT;
import static io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetType.EXTERNAL_TRAINING_DATASET;
import static io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetType.HOPSFS_TRAINING_DATASET;
import static io.hops.hopsworks.restutils.RESTCodes.FeaturestoreErrorCode.FAILED_TO_DELETE_TD_DATA;

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
  private TrainingDatasetInputValidation trainingDatasetInputValidation;
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
  private OnlineFeaturegroupController onlineFeaturegroupController;
  @EJB
  private TransformationFunctionFacade transformationFunctionFacade;
  @EJB
  private TrainingDatasetInputValidation inputValidation;
  @EJB
  private PitJoinController pitJoinController;
  @EJB
  private QueryController queryController;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private QuotasEnforcement quotasEnforcement;
  @EJB
  private FilterController filterController;

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
   * @param user
   * @param project
   * @param trainingDataset trainingDataset entity
   * @return JSON/XML DTO of the trainingDataset
   * @throws ServiceException
   * @throws FeaturestoreException
   */
  public TrainingDatasetDTO convertTrainingDatasetToDTO(Users user, Project project, TrainingDataset trainingDataset)
      throws ServiceException, FeaturestoreException {
    return convertTrainingDatasetToDTO(user, project, trainingDataset, false);
  }

  /**
   * Converts a trainingDataset entity to a TrainingDataset DTO
   *
   * @param user
   * @param project
   * @param trainingDataset trainingDataset entity
   * @param skipFeature do not include feature
   * @return JSON/XML DTO of the trainingDataset
   * @throws ServiceException
   * @throws FeaturestoreException
   */
  public TrainingDatasetDTO convertTrainingDatasetToDTO(Users user, Project project, TrainingDataset trainingDataset,
      Boolean skipFeature)
      throws ServiceException, FeaturestoreException {
    TrainingDatasetDTO trainingDatasetDTO = new TrainingDatasetDTO(trainingDataset);

    String featurestoreName = featurestoreFacade.getHiveDbName(trainingDataset.getFeaturestore().getHiveDbId());
    trainingDatasetDTO.setFeaturestoreName(featurestoreName);

    if (!skipFeature) {
      // Set features
      List<TrainingDatasetFeature> tdFeatures = getFeaturesSorted(trainingDataset, true);
      Map<Integer, String> fsLookupTable = getFsLookupTableFeatures(tdFeatures);
      trainingDatasetDTO.setFeatures(tdFeatures
          .stream()
          .map(f -> new TrainingDatasetFeatureDTO(checkPrefix(f), f.getType(),
              f.getFeatureGroup() != null ?
                  new FeaturegroupDTO(f.getFeatureGroup().getFeaturestore().getId(),
                      fsLookupTable.get(f.getFeatureGroup().getFeaturestore().getId()),
                      f.getFeatureGroup().getId(), f.getFeatureGroup().getName(),
                      f.getFeatureGroup().getVersion(),
                      onlineFeaturegroupController.onlineFeatureGroupTopicName(project.getId(),
                          f.getFeatureGroup().getId(), Utils.getFeaturegroupName(f.getFeatureGroup())))
                  : null,
              f.getIndex(), f.isLabel()))
          .collect(Collectors.toList()));
    }

    switch (trainingDataset.getTrainingDatasetType()) {
      case HOPSFS_TRAINING_DATASET:
      case IN_MEMORY_TRAINING_DATASET:
        return hopsfsTrainingDatasetController.convertHopsfsTrainingDatasetToDTO(trainingDatasetDTO, trainingDataset);
      case EXTERNAL_TRAINING_DATASET:
        return externalTrainingDatasetController.convertExternalTrainingDatasetToDTO(user, project,
            trainingDatasetDTO, trainingDataset);
      default:
        throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_TRAINING_DATASET_TYPE.getMessage() +
          ", Recognized training dataset types are: " + HOPSFS_TRAINING_DATASET + ", and: " +
          EXTERNAL_TRAINING_DATASET + ". The provided training dataset type was not recognized: "
          + trainingDataset.getTrainingDatasetType());
    }
  }

  public TrainingDatasetDTO createTrainingDataset(Users user, Project project, Featurestore featurestore,
      FeatureView featureView, TrainingDatasetDTO trainingDatasetDTO)
      throws FeaturestoreException, ProvenanceException, IOException, ServiceException {
    // Name of Training data = <feature view name>_<feature view version>, version is needed
    // because there can be multiple training dataset of same name from different version of feature view
    trainingDatasetDTO.setName(featureView.getName() + "_" + featureView.getVersion());
    Query query = queryController.makeQuery(featureView, project, user, true, false);

    return createTrainingDataset(user, project, featurestore, featureView, trainingDatasetDTO, query, true);
  }

  public TrainingDatasetDTO createTrainingDataset(Users user, Project project, Featurestore featurestore,
                                                 TrainingDatasetDTO trainingDatasetDTO)
      throws FeaturestoreException, ProvenanceException, IOException, ServiceException {

    // If the training dataset is constructed from a query, verify that it compiles correctly
    Query query = null;
    if (trainingDatasetDTO.getQueryDTO() != null) {
      query = constructQuery(trainingDatasetDTO.getQueryDTO(), project, user);
    } else if (trainingDatasetDTO.getFeatures() == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_NO_SCHEMA,
          Level.FINE, "The training dataset doesn't have any feature");
    }
    return createTrainingDataset(user, project, featurestore, null, trainingDatasetDTO, query, false);
  }


  private TrainingDatasetDTO createTrainingDataset(Users user, Project project, Featurestore featurestore,
      FeatureView featureView, TrainingDatasetDTO trainingDatasetDTO, Query query, Boolean skipFeature)
      throws FeaturestoreException, ProvenanceException, IOException, ServiceException {

    try {
      quotasEnforcement.enforceTrainingDatasetsQuota(featurestore);
    } catch (QuotaEnforcementException ex) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_COULD_NOT_BE_CREATED,
              Level.SEVERE, ex.getMessage(), ex.getMessage(), ex);
    }

    // if version not provided, get latest and increment
    if (trainingDatasetDTO.getVersion() == null) {
      // returns ordered list by desc version
      List<TrainingDataset> tdPrevious;
      if (featureView != null) {
        tdPrevious = trainingDatasetFacade.findByFeatureViewAndVersionOrderedDescVersion(featureView);
      } else {
        tdPrevious = trainingDatasetFacade.findByNameAndFeaturestoreOrderedDescVersion(
            trainingDatasetDTO.getName(), featurestore);
      }
      if (tdPrevious != null && !tdPrevious.isEmpty()) {
        trainingDatasetDTO.setVersion(tdPrevious.get(0).getVersion() + 1);
      } else {
        trainingDatasetDTO.setVersion(1);
      }
    }

    // Check that training dataset doesn't already exists
    if ((featureView != null && trainingDatasetFacade.findByFeatureViewAndVersionNullable(
        featureView, trainingDatasetDTO.getVersion()).isPresent()) ||
        (featureView == null && trainingDatasetFacade.findByNameVersionAndFeaturestore(
            trainingDatasetDTO.getName(), trainingDatasetDTO.getVersion(), featurestore).isPresent())
    ) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_ALREADY_EXISTS, Level.FINE,
          "Training Dataset: " + trainingDatasetDTO.getName() + ", version: " + trainingDatasetDTO.getVersion());
    }

    // Verify input
    inputValidation.validate(trainingDatasetDTO, query);

    Inode inode = null;
    FeaturestoreConnector featurestoreConnector;
    TrainingDatasetDTO completeTrainingDatasetDTO;
    if (trainingDatasetDTO.getTrainingDatasetType() == HOPSFS_TRAINING_DATASET) {
      if (trainingDatasetDTO.getStorageConnector() != null &&
          trainingDatasetDTO.getStorageConnector().getId() != null) {
        featurestoreConnector = featurestoreConnectorFacade
            .findByIdType(trainingDatasetDTO.getStorageConnector().getId(), FeaturestoreConnectorType.HOPSFS)
            .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.HOPSFS_CONNECTOR_NOT_FOUND,
                Level.FINE, "HOPSFS Connector: " + trainingDatasetDTO.getStorageConnector().getId()));
      } else {
        featurestoreConnector = getDefaultHopsFSTrainingDatasetConnector(featurestore);
      }
    } else if (trainingDatasetDTO.getTrainingDatasetType() == TrainingDatasetType.EXTERNAL_TRAINING_DATASET) {
      if (trainingDatasetDTO.getStorageConnector() == null) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.CONNECTOR_NOT_FOUND,
            Level.FINE, "Storage connector is empty");
      }

      featurestoreConnector = featurestoreConnectorFacade
          .findById(trainingDatasetDTO.getStorageConnector().getId())
          .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.CONNECTOR_NOT_FOUND,
              Level.FINE, "Connector: " + trainingDatasetDTO.getStorageConnector().getId()));
    } else {
      featurestoreConnector = getDefaultHopsFSTrainingDatasetConnector(featurestore);
    }

    // for HopsFS TD it will either be the default connector already or it will be a connector pointing to another
    // HopsFS Directory
    // for external TD or in-memory TD we will use default connector
    Dataset trainingDatasetsFolder;
    if (featurestoreConnector.getHopsfsConnector() != null) {
      trainingDatasetsFolder = featurestoreConnector.getHopsfsConnector().getHopsfsDataset();
    } else {
      trainingDatasetsFolder =
          getDefaultHopsFSTrainingDatasetConnector(featurestore).getHopsfsConnector().getHopsfsDataset();
    }

    // TODO(Fabio) account for path
    // we allow specifying the path in the training dataset dir, but it is not really used, this option will be
    // deprecated for hopsfs training datasets.
    String trainingDatasetPath = getTrainingDatasetPath(
        inodeController.getPath(trainingDatasetsFolder.getInode()),
        trainingDatasetDTO.getName(), trainingDatasetDTO.getVersion());

    DistributedFileSystemOps udfso = null;
    String username = hdfsUsersBean.getHdfsUserName(project, user);
    try {
      udfso = dfs.getDfsOps(username);
      udfso.mkdir(trainingDatasetPath);

      inode = inodeController.getInodeAtPath(trainingDatasetPath);
      completeTrainingDatasetDTO = createTrainingDatasetMetadata(user, project,
          featurestore, featureView, trainingDatasetDTO, query, featurestoreConnector, inode, skipFeature);
      if (featureView == null) {
        fsProvenanceController.trainingDatasetAttachXAttr(trainingDatasetPath, completeTrainingDatasetDTO, udfso);
      }
      activityFacade.persistActivity(ActivityFacade.CREATED_TRAINING_DATASET +
          completeTrainingDatasetDTO.getName(), project, user, ActivityFlag.SERVICE);
      return completeTrainingDatasetDTO;
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }

  public FeaturestoreConnector getHopsFsConnector(TrainingDataset trainingDataset) throws FeaturestoreException {
    FeaturestoreConnector featurestoreConnector = null;
    switch (trainingDataset.getTrainingDatasetType()) {
      case HOPSFS_TRAINING_DATASET:
      case IN_MEMORY_TRAINING_DATASET:
        featurestoreConnector = trainingDataset.getHopsfsTrainingDataset().getFeaturestoreConnector();
        break;
      case EXTERNAL_TRAINING_DATASET:
        featurestoreConnector = getDefaultHopsFSTrainingDatasetConnector(trainingDataset.getFeaturestore());
        break;
    }
    return featurestoreConnector;
  }

  public Boolean isTrainingDatasetAvailable(TrainingDataset trainingDataset, Users user)
      throws FeaturestoreException, IOException {
    switch (trainingDataset.getTrainingDatasetType()) {
      case IN_MEMORY_TRAINING_DATASET:
        return false;
      case EXTERNAL_TRAINING_DATASET:
        return null;
      default: {
        String trainingDatasetRootPath = getTrainingDatasetPath(inodeController.getPath(
            getHopsFsConnector(trainingDataset).getHopsfsConnector().getHopsfsDataset().getInode()),
            trainingDataset.getName(), trainingDataset.getVersion());

        String username = hdfsUsersBean.getHdfsUserName(trainingDataset.getFeaturestore().getProject(), user);

        DistributedFileSystemOps dfso = null;
        try {
          dfso = dfs.getDfsOps(username);
          if (!trainingDataset.getSplits().isEmpty()) {
            for (TrainingDatasetSplit split : trainingDataset.getSplits()) {
              if (!dfso.exists(trainingDatasetRootPath + Path.SEPARATOR + split.getName())) {
                return false;
              }
            }
            return true;
          } else {
            return dfso.exists(trainingDatasetRootPath + Path.SEPARATOR + trainingDataset.getName());
          }
        } finally {
          dfs.closeDfsClient(dfso);
        }
      }
    }
  }

  private FeaturestoreConnector getDefaultHopsFSTrainingDatasetConnector(Featurestore featurestore)
      throws FeaturestoreException {
    String connectorName =
        featurestore.getProject().getName() + "_" + Settings.ServiceDataset.TRAININGDATASETS.getName();
    return featurestoreConnectorFacade.findByFeaturestoreName(featurestore, connectorName)
        .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.HOPSFS_CONNECTOR_NOT_FOUND,
            Level.FINE, "HOPSFS Connector: " + connectorName));
  }

  /**
   * Creates the metadata structure in DB for the training dataset
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  private TrainingDatasetDTO createTrainingDatasetMetadata(Users user, Project project, Featurestore featurestore,
      FeatureView featureView, TrainingDatasetDTO trainingDatasetDTO, Query query,
      FeaturestoreConnector featurestoreConnector, Inode inode, Boolean skipFeature)
      throws FeaturestoreException, ServiceException {
    //Create specific dataset type
    HopsfsTrainingDataset hopsfsTrainingDataset = null;
    ExternalTrainingDataset externalTrainingDataset = null;
    switch (trainingDatasetDTO.getTrainingDatasetType()) {
      case HOPSFS_TRAINING_DATASET:
      case IN_MEMORY_TRAINING_DATASET:
        // inode is required for keyword and tag for even for in-memory training dataset
        hopsfsTrainingDataset =
            hopsfsTrainingDatasetFacade.createHopsfsTrainingDataset(featurestoreConnector, inode);
        break;
      case EXTERNAL_TRAINING_DATASET:
        externalTrainingDataset = externalTrainingDatasetController.create(featurestoreConnector,
          trainingDatasetDTO.getLocation(), inode);
        break;
      default:
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_TRAINING_DATASET_TYPE, Level.FINE,
          ", Recognized training dataset types are: " + HOPSFS_TRAINING_DATASET + ", and: " +
          TrainingDatasetType.EXTERNAL_TRAINING_DATASET + ". The provided training dataset type was not recognized: "
          + trainingDatasetDTO.getTrainingDatasetType());
    }

    //Store trainingDataset metadata in Hopsworks
    TrainingDataset trainingDataset = new TrainingDataset();
    trainingDataset.setFeatureView(featureView);
    if (trainingDatasetDTO.getEventStartTime() != null) {
      trainingDataset.setStartTime(trainingDatasetDTO.getEventStartTime());
    }
    if (trainingDatasetDTO.getEventEndTime() != null) {
      trainingDataset.setEndTime(trainingDatasetDTO.getEventEndTime());
    }
    trainingDataset.setSampleRatio(trainingDatasetDTO.getSampleRatio());
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
        .map(split -> {
          if (RANDOM_SPLIT.equals(split.getSplitType())) {
            return new TrainingDatasetSplit(trainingDataset, split.getName(), split.getPercentage());
          } else {
            return new TrainingDatasetSplit(trainingDataset, split.getName(), split.getStartTime(), split.getEndTime());
          }
        })
        .collect(Collectors.toList()));
    trainingDataset.setCoalesce(trainingDatasetDTO.getCoalesce() != null ? trainingDatasetDTO.getCoalesce() : false);

    StatisticsConfig statisticsConfig = new StatisticsConfig(trainingDatasetDTO.getStatisticsConfig().getEnabled(),
      trainingDatasetDTO.getStatisticsConfig().getCorrelations(),
      trainingDatasetDTO.getStatisticsConfig().getHistograms(),
      trainingDatasetDTO.getStatisticsConfig().getExactUniqueness());
    statisticsConfig.setTrainingDataset(trainingDataset);
    statisticsConfig.setStatisticColumns(trainingDatasetDTO.getStatisticsConfig().getColumns().stream()
      .map(sc -> new StatisticColumn(statisticsConfig, sc)).collect(Collectors.toList()));
    trainingDataset.setStatisticsConfig(statisticsConfig);
    trainingDataset.setTrainSplit(trainingDatasetDTO.getTrainSplit());

    // Query info is stored in FeatureView instead of TrainingDataset
    if (featureView == null) {
      // set features/query
      trainingDataset.setQuery(trainingDatasetDTO.getQueryDTO() != null);
      if (trainingDataset.isQuery()) {
        setTrainingDatasetQuery(query, trainingDatasetDTO.getFeatures(), trainingDataset);
      } else if (trainingDatasetDTO.getFeatures() != null) {
        trainingDataset.setFeatures(getTrainingDatasetFeatures(trainingDatasetDTO.getFeatures(), trainingDataset));
      }
    } else {
      if (trainingDatasetDTO.getExtraFilter() != null) {
        trainingDataset.setFilters(
            convertToFilterEntities(
                filterController.convertFilterLogic(project, user, query, trainingDatasetDTO.getExtraFilter()),
                trainingDataset,
                "L")
        );
      }
    }

    TrainingDataset dbTrainingDataset = trainingDatasetFacade.update(trainingDataset);

    // Log the metadata operation
    fsActivityFacade.logMetadataActivity(user, dbTrainingDataset, featureView, FeaturestoreActivityMeta.TD_CREATED);

    //Get final entity from the database
    return convertTrainingDatasetToDTO(user, project, dbTrainingDataset, skipFeature);
  }


  private Query constructQuery(QueryDTO queryDTO, Project project, Users user) throws FeaturestoreException {
    // Convert the queryDTO to the internal representation
    Map<Integer, String> fgAliasLookup = new HashMap<>();
    Map<Integer, Featuregroup> fgLookup = new HashMap<>();
    Map<Integer, List<Feature>> availableFeatureLookup = new HashMap<>();

    queryController.populateFgLookupTables(queryDTO, 0, fgAliasLookup, fgLookup, availableFeatureLookup,
        project, user, null);
    return queryController.convertQueryDTO(queryDTO, fgAliasLookup, fgLookup, availableFeatureLookup,
      pitJoinController.isPitEnabled(queryDTO));
  }

  private void setTrainingDatasetQuery(Query query,
                                       List<TrainingDatasetFeatureDTO> features,
                                       TrainingDataset trainingDataset) throws FeaturestoreException {
    // Convert the joins from the query object into training dataset joins
    List<TrainingDatasetJoin> tdJoins = collectJoins(query, trainingDataset, null);
    trainingDataset.setJoins(tdJoins);
    List<TrainingDatasetFeature> tdFeatures = collectFeatures(query, features, trainingDataset, null, 0, tdJoins, 0);
    trainingDataset.setFeatures(tdFeatures);
    List<TrainingDatasetFilter> filters = convertToFilterEntities(query.getFilter(), trainingDataset, "L");
    trainingDataset.setFilters(filters);
  }

  List<TrainingDatasetFilter> convertToFilterEntities(FilterLogic filterLogic,
      TrainingDataset trainingDataset, String path) {
    return convertToFilterEntities(filterLogic, null, trainingDataset, path);
  }

  public List<TrainingDatasetFilter> convertToFilterEntities(FilterLogic filterLogic,
      FeatureView featureView, String path) {
    return convertToFilterEntities(filterLogic, featureView, null, path);
  }

  private List<TrainingDatasetFilter> convertToFilterEntities(FilterLogic filterLogic,
      FeatureView featureView, TrainingDataset trainingDataset, String path) {
    List<TrainingDatasetFilter> filters = new ArrayList<>();
    if (filterLogic == null) {
      return filters;
    }
    if (filterLogic.getType().equals(SqlFilterLogic.SINGLE)) {
      if (filterLogic.getLeftFilter() == null) {
        filters.add(
            makeTrainingDatasetFilter(path, featureView, trainingDataset, filterLogic.getRightFilter(),
                SqlFilterLogic.SINGLE));
      } else {
        filters.add(
            makeTrainingDatasetFilter(path, featureView, trainingDataset, filterLogic.getLeftFilter(),
                filterLogic.getType()));
      }
    } else {
      filters.add(
          makeTrainingDatasetFilter(path,featureView, trainingDataset, null, filterLogic.getType()));
      if (filterLogic.getLeftFilter() != null) {
        filters.add(makeTrainingDatasetFilter(
            path + ".L", featureView, trainingDataset, filterLogic.getLeftFilter(), SqlFilterLogic.SINGLE));
      }
      if (filterLogic.getRightFilter() != null) {
        filters.add(makeTrainingDatasetFilter(
            path + ".R", featureView, trainingDataset, filterLogic.getRightFilter(), SqlFilterLogic.SINGLE));
      }
      filters.addAll(convertToFilterEntities(filterLogic.getLeftLogic(), featureView, trainingDataset, path + ".L"));
      filters.addAll(convertToFilterEntities(filterLogic.getRightLogic(), featureView, trainingDataset, path + ".R"));
    }
    return filters;
  }

  private TrainingDatasetFilter makeTrainingDatasetFilter(String path, FeatureView featureView,
      TrainingDataset trainingDataset, Filter filter, SqlFilterLogic type) {
    TrainingDatasetFilter trainingDatasetFilter = featureView == null ?
        new TrainingDatasetFilter(trainingDataset) : new TrainingDatasetFilter(featureView);
    TrainingDatasetFilterCondition condition = filter == null ? null : convertFilter(filter, trainingDatasetFilter);
    trainingDatasetFilter.setCondition(condition);
    trainingDatasetFilter.setPath(path);
    trainingDatasetFilter.setType(type);
    return trainingDatasetFilter;
  }

  private TrainingDatasetFilterCondition convertFilter(Filter filter, TrainingDatasetFilter trainingDatasetFilter) {
    return new TrainingDatasetFilterCondition(
        trainingDatasetFilter,
        filter.getFeatures().get(0).getFeatureGroup(),
        filter.getFeatures().get(0).getName(),
        filter.getCondition(),
        filter.getValue().getFeatureGroupId(),
        filter.getValue().getValue()
    );
  }

  // Here we need to pass the list of training dataset joins so that we can rebuild the aliases.
  // and handle correctly the case in which a feature group is joined with itself.
  public List<TrainingDatasetFeature> collectFeatures(Query query, List<TrainingDatasetFeatureDTO> featureDTOs,
      TrainingDataset trainingDataset, FeatureView featureView,
      int featureIndex, List<TrainingDatasetJoin> tdJoins, int joinIndex)
      throws FeaturestoreException {
    List<TrainingDatasetFeature> features = new ArrayList<>();
    boolean isLabel = false;
    TransformationFunction transformationFunction = null;
    for (Feature f : query.getFeatures()) {
      if (featureDTOs != null && !featureDTOs.isEmpty()) {
        // identify if feature is label
        isLabel = featureDTOs.stream().anyMatch(dto -> f.getName().equals(dto.getName()) && dto.getLabel());
        // get transformation function for this feature
        transformationFunction = getTransformationFunction(f, featureDTOs);
      }
      features.add(trainingDataset != null ?
          new TrainingDatasetFeature(trainingDataset, tdJoins.get(joinIndex), query.getFeaturegroup(),
          f.getName(), f.getType(), featureIndex++, isLabel, transformationFunction):
          new TrainingDatasetFeature(featureView, tdJoins.get(joinIndex), query.getFeaturegroup(),
              f.getName(), f.getType(), featureIndex++, isLabel, transformationFunction));
    }

    if (query.getJoins() != null) {
      for (Join join : query.getJoins()) {
        joinIndex++;
        List<TrainingDatasetFeature> joinFeatures
            = collectFeatures(join.getRightQuery(), featureDTOs, trainingDataset, featureView, featureIndex, tdJoins,
            joinIndex);
        features.addAll(joinFeatures);
        featureIndex += joinFeatures.size();
      }
    }
    return features;
  }

  public List<TrainingDatasetJoin> collectJoins(Query query, TrainingDataset trainingDataset,
                                                FeatureView featureView) {
    List<TrainingDatasetJoin> joins = new ArrayList<>();
    // add the first feature group
    Featuregroup featuregroup = query.getFeaturegroup();
    int index = 0;
    if ((featuregroup.getFeaturegroupType() == FeaturegroupType.CACHED_FEATURE_GROUP &&
        featuregroup.getCachedFeaturegroup().getTimeTravelFormat() == TimeTravelFormat.HUDI) ||
        featuregroup.getFeaturegroupType() == FeaturegroupType.STREAM_FEATURE_GROUP
    ){
      joins.add(makeTrainingDatasetJoin(trainingDataset, featureView, featuregroup,
          query.getLeftFeatureGroupEndCommitId() , (short) 0, index++, null));
    } else {
      joins.add(makeTrainingDatasetJoin(trainingDataset, featureView, query.getFeaturegroup(),
          null, (short) 0, index++, null));
    }

    if (query.getJoins() != null && !query.getJoins().isEmpty()) {
      for (Join join : query.getJoins()) {
        TrainingDatasetJoin tdJoin;
        Featuregroup rightFeaturegroup = join.getRightQuery().getFeaturegroup();
        if ((rightFeaturegroup.getFeaturegroupType() == FeaturegroupType.CACHED_FEATURE_GROUP &&
            rightFeaturegroup.getCachedFeaturegroup().getTimeTravelFormat() == TimeTravelFormat.HUDI) ||
            rightFeaturegroup.getFeaturegroupType() == FeaturegroupType.STREAM_FEATURE_GROUP
        ){
          tdJoin = makeTrainingDatasetJoin(trainingDataset, featureView,
              rightFeaturegroup, join.getRightQuery().getLeftFeatureGroupEndCommitId(),
              (short) join.getJoinType().ordinal(),
              index++, join.getPrefix());
        } else {
          tdJoin = makeTrainingDatasetJoin(trainingDataset, featureView,
              rightFeaturegroup, null,
              (short) join.getJoinType().ordinal(),
              index++, join.getPrefix());
        }
        tdJoin.setConditions(collectJoinConditions(join, tdJoin));
        joins.add(tdJoin);
      }
    }

    return joins;
  }

  private TrainingDatasetJoin makeTrainingDatasetJoin(TrainingDataset trainingDataset,
      FeatureView featureView, Featuregroup featureGroup, Long featureGroupCommitId,
      short type, int index, String prefix) {
    if (trainingDataset != null) {
      return new TrainingDatasetJoin(trainingDataset, featureGroup, featureGroupCommitId, type, index, prefix);
    } else {
      return new TrainingDatasetJoin(featureView, featureGroup, featureGroupCommitId, type, index, prefix);
    }
  }

  private List<TrainingDatasetJoinCondition> collectJoinConditions(Join join, TrainingDatasetJoin tdJoin) {
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
          new TrainingDatasetFeature(trainingDataset, f.getName(), f.getType(), index++, f.getLabel(), null));
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

  public TrainingDataset getTrainingDatasetByFeatureViewAndVersion(FeatureView featureView, Integer version)
      throws FeaturestoreException {
    return trainingDatasetFacade.findByFeatureViewAndVersion(featureView, version);
  }

  public List<TrainingDataset> getTrainingDatasetByFeatureView(FeatureView featureView) {
    return trainingDatasetFacade.findByFeatureView(featureView);
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
    return delete(user, project, featurestore, trainingDataset);
  }

  public void delete(Users user, Project project, Featurestore featurestore, FeatureView featureView,
      Integer trainingDatasetVersion) throws FeaturestoreException {
    TrainingDataset trainingDataset = getTrainingDatasetByFeatureViewAndVersion(featureView, trainingDatasetVersion);
    String trainingDatasetName = delete(user, project, featurestore, trainingDataset);
    activityFacade.persistActivity(ActivityFacade.DELETED_TRAINING_DATASET + trainingDatasetName
            + " and version " + trainingDataset.getVersion(),
        project, user, ActivityFlag.SERVICE);
  }

  public void delete(Users user, Project project, Featurestore featurestore, FeatureView featureView)
      throws FeaturestoreException {
    List<TrainingDataset> trainingDatasets = getTrainingDatasetByFeatureView(featureView);

    // Delete all only if user has the right to delete all versions of training dataset.
    for (TrainingDataset trainingDataset: trainingDatasets) {
      featurestoreUtils.verifyUserRole(trainingDataset, featurestore, user, project);
    }

    for (TrainingDataset trainingDataset: trainingDatasets) {
      // Since this method is seldomly called, so ok that retrieving TrainingDataset from DB twice.
      delete(user, project, featurestore, featureView, trainingDataset.getVersion());
    }
  }

  public String delete(Users user, Project project, Featurestore featurestore, TrainingDataset trainingDataset)
      throws FeaturestoreException {
    featurestoreUtils.verifyUserRole(trainingDataset, featurestore, user, project);

    statisticsController.deleteStatistics(project, user, trainingDataset);

    trainingDatasetFacade.removeTrainingDataset(trainingDataset);
    deleteHopsfsTrainingData(user, project, featurestore, trainingDataset, false, false);
    return trainingDataset.getName();
  }

  public void deleteDataOnly(Users user, Project project, Featurestore featurestore, FeatureView featureView,
      Integer trainingDatasetVersion) throws FeaturestoreException {
    TrainingDataset trainingDataset = getTrainingDatasetByFeatureViewAndVersion(featureView, trainingDatasetVersion);
    checkDeleteDataOnly(trainingDataset);
    deleteHopsfsTrainingData(user, project, featurestore, trainingDataset, true, true);
    activityFacade.persistActivity(ActivityFacade.DELETED_TRAINING_DATASET_DATA_ONLY + trainingDataset.getName()
            + " and version " + trainingDataset.getVersion(),
        project, user, ActivityFlag.SERVICE);
  }

  public void deleteDataOnly(Users user, Project project, Featurestore featurestore, FeatureView featureView)
      throws FeaturestoreException {
    List<TrainingDataset> trainingDatasets = getTrainingDatasetByFeatureView(featureView);

    // Delete all only if user has the right to delete all versions of training dataset.
    for (TrainingDataset trainingDataset: trainingDatasets) {
      featurestoreUtils.verifyUserRole(trainingDataset, featurestore, user, project);
    }
    List<Integer> failedTd = Lists.newArrayList();
    for (TrainingDataset trainingDataset: trainingDatasets) {
      try {
        deleteDataOnly(user, project, featurestore, featureView, trainingDataset.getVersion());
      } catch (FeaturestoreException e) {
        if (FAILED_TO_DELETE_TD_DATA.equals(e.getErrorCode())) {
          failedTd.add(trainingDataset.getVersion());
        } else {
          throw e;
        }
      }
    }
    if (!failedTd.isEmpty()) {
      throw new FeaturestoreException(
          FAILED_TO_DELETE_TD_DATA, Level.FINE,
          String.format("Failed to delete data for training dataset version: %s. "
                  + "Delete data only is only applicable to 'HOPSFS_TRAINING_DATASET'",
              Joiner.on(", ").join(failedTd.stream().sorted().collect(Collectors.toList())))
      );
    }
  }

  private void checkDeleteDataOnly(TrainingDataset trainingDataset) throws FeaturestoreException {
    if (!HOPSFS_TRAINING_DATASET.equals(trainingDataset.getTrainingDatasetType())) {
      throw new FeaturestoreException(
          FAILED_TO_DELETE_TD_DATA, Level.FINE,
          String.format("Failed to delete data for training dataset version: %d. "
                  + "Delete data only is only applicable to 'HOPSFS_TRAINING_DATASET' but not %s.",
              trainingDataset.getVersion(), trainingDataset.getTrainingDatasetType())
      );
    }
  }

  public void deleteHopsfsTrainingData(Users user, Project project, Featurestore featurestore,
      TrainingDataset trainingDataset, Boolean verifyRole, Boolean keepMetadata)
      throws FeaturestoreException {
    if (verifyRole) {
      featurestoreUtils.verifyUserRole(trainingDataset, featurestore, user, project);
    }
    String dsPath = getTrainingDatasetInodePath(trainingDataset);
    String username = hdfsUsersBean.getHdfsUserName(project, user);

    DistributedFileSystemOps udfso = dfs.getDfsOps(username);
    try {
      // TODO(Fabio): if Data owner *In project* do operation as superuser
      if (keepMetadata) {
        // Since keywords and tags are stored as attribute of the folder, delete contents in the folder only.
        FileStatus[] fileStatuses = udfso.listStatus(new Path(dsPath));
        for (FileStatus fileStatus : fileStatuses) {
          udfso.rm(fileStatus.getPath(), true);
        }
      } else {
        udfso.rm(dsPath, true);
      }
    } catch (IOException e) {
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }

  public String getTrainingDatasetInodePath(TrainingDataset trainingDataset) {
    if (trainingDataset.getTrainingDatasetType() == TrainingDatasetType.EXTERNAL_TRAINING_DATASET) {
      return inodeController.getPath(trainingDataset.getExternalTrainingDataset().getInode());
    } else {
      return inodeController.getPath(trainingDataset.getHopsfsTrainingDataset().getInode());
    }
  }

  /**
   * Updates a training dataset with new metadata
   *
   * @param user
   * @param project
   * @param featurestore             the featurestore that the trainingDataset is linked to
   * @param trainingDatasetDTO       the user input data for updating the training dataset
   * @return a JSON/XML DTO of the updated training dataset
   * @throws FeaturestoreException
   * @throws ServiceException
   */
  public TrainingDatasetDTO updateTrainingDatasetMetadata(Users user, Project project,
                                                          Featurestore featurestore,
                                                          TrainingDatasetDTO trainingDatasetDTO)
      throws FeaturestoreException, ServiceException {
    TrainingDataset trainingDataset =
        trainingDatasetFacade.findByIdAndFeaturestore(trainingDatasetDTO.getId(), featurestore)
            .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_NOT_FOUND,
            Level.FINE, "training dataset id: " + trainingDatasetDTO.getId()));

    // Verify general entity related information
    trainingDatasetInputValidation.verifyUserInput(trainingDatasetDTO);

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
    if (trainingDatasetDTO.getStatisticsConfig().getExactUniqueness() != null) {
      trainingDataset.getStatisticsConfig().setExactUniqueness(trainingDatasetDTO.getStatisticsConfig()
        .getExactUniqueness());
    }
    // compare against schema from database, as client doesn't need to send schema in update request
    statisticColumnController.verifyStatisticColumnsExist(trainingDatasetDTO, trainingDataset);
    trainingDataset = trainingDatasetFacade.update(trainingDataset);
    statisticColumnController
      .persistStatisticColumns(trainingDataset, trainingDatasetDTO.getStatisticsConfig().getColumns());
    // get feature group again with persisted columns - this trip to the database can be saved
    trainingDataset = getTrainingDatasetById(featurestore, trainingDatasetDTO.getId());

    activityFacade.persistActivity(ActivityFacade.EDITED_TRAINING_DATASET + trainingDatasetDTO.getName(),
        project, user, ActivityFlag.SERVICE);

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

  //TODO feature view: remove
  /**
   * Reconstruct the query used to generate the training datset, fetching the features and the joins
   * in the proper order from the database.
   * @param trainingDataset
   * @return
   * @throws FeaturestoreException
   */
  public Query getQuery(TrainingDataset trainingDataset, boolean withLabel, Project project,
                        Users user, Boolean isHiveEngine) throws FeaturestoreException {

    if (!trainingDataset.isQuery()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_NO_QUERY,
              Level.FINE, "Inference vector is only available for datasets generated by queries");
    }

    List<TrainingDatasetJoin> joins = getJoinsSorted(trainingDataset);

    // These features are for the select part and are from different feature groups
    // to respect the ordering, all selected features are added to the left most Query instead of splitting them
    // over the querys for their respective origin feature group
    List<TrainingDatasetFeature> tdFeatures = getFeaturesSorted(trainingDataset, withLabel);
    return getQuery(joins, tdFeatures, trainingDataset.getFilters(), project, user, isHiveEngine);
  }

  public Query getQuery(List<TrainingDatasetJoin> joins, List<TrainingDatasetFeature> tdFeatures,
      Collection<TrainingDatasetFilter> trainingDatasetFilters, Project project,
      Users user, Boolean isHiveEngine) throws FeaturestoreException {
    return getQuery(joins, tdFeatures, trainingDatasetFilters, project, user, isHiveEngine, Lists.newArrayList());
  }

  public Query getQuery(List<TrainingDatasetJoin> joins, List<TrainingDatasetFeature> tdFeatures,
      Collection<TrainingDatasetFilter> trainingDatasetFilters, Project project,
      Users user, Boolean isHiveEngine, Collection<TrainingDatasetFilter> extraFilters) throws FeaturestoreException {
    // Convert all the TrainingDatasetFeatures to QueryFeatures
    Map<Integer, String> fgAliasLookup = getAliasLookupTable(joins);

    // Check that all the feature groups still exists, if not collect missing feature names to provide information to
    // user
    if (tdFeatures.stream().anyMatch(j -> j.getFeatureGroup() == null)) {
      return new Query(tdFeatures.stream().filter(j -> j.getFeatureGroup() == null)
          .map(TrainingDatasetFeature::getName)
          .collect(Collectors.toList()));
    }

    // Get available features for all involved feature groups once, and save in map fgId -> availableFeatures
    Map<Integer, List<Feature>> availableFeaturesLookup = new HashMap<>();
    for (TrainingDatasetJoin join : joins) {
      if (!availableFeaturesLookup.containsKey(join.getFeatureGroup().getId())) {
        List<Feature> availableFeatures = featuregroupController.getFeatures(join.getFeatureGroup(), project, user)
          .stream()
          .map(f -> new Feature(f.getName(),
                  fgAliasLookup.get(join.getId()), f.getType(), f.getPrimary(), f.getDefaultValue(), join.getPrefix(),
                  join.getFeatureGroup()))
          .collect(Collectors.toList());
        availableFeaturesLookup.put(join.getFeatureGroup().getId(), availableFeatures);
      }
    }

    Map<String, Feature> featureLookup = availableFeaturesLookup.values().stream().flatMap(List::stream)
        .collect(Collectors.toMap(
          f -> makeFeatureLookupKey(f.getFeatureGroup().getId(), f.getName()), f -> f, (f1, f2) -> f1));

    List<Feature> features = new ArrayList<>();
    for (TrainingDatasetFeature requestedFeature : tdFeatures) {
      Feature tdFeature = featureLookup.get(makeFeatureLookupKey(requestedFeature.getFeatureGroup().getId(),
          requestedFeature.getName()));
      if (tdFeature == null) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURE_DOES_NOT_EXIST, Level.FINE,
            "Feature: " + requestedFeature.getName() + " not found in feature group: " +
                requestedFeature.getFeatureGroup().getName());
      }
      // instantiate new feature since alias in available feature is not correct if fg is joined with itself
      Feature featureWithCorrectAlias = new Feature(tdFeature.getName(),
        fgAliasLookup.get(requestedFeature.getTrainingDatasetJoin().getId()),
        tdFeature.getType(), tdFeature.getDefaultValue(), tdFeature.getPrefix(), requestedFeature.getFeatureGroup(),
        requestedFeature.getIndex());
      features.add(featureWithCorrectAlias);
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
        availableFeaturesLookup.get(joins.get(0).getFeatureGroup().getId()),
        isHiveEngine);

    // Set the remaining feature groups as join
    List<Join> queryJoins = new ArrayList<>();
    for (int i = 1; i < joins.size(); i++) {
      // left side of the join stays fixed, the counter starts at 1
      queryJoins.add(getQueryJoin(query, joins.get(i), fgAliasLookup, fsLookup, availableFeaturesLookup, isHiveEngine));
    }
    query.setJoins(queryJoins);
    FilterLogic filterLogic = convertToFilterLogic(trainingDatasetFilters, featureLookup, "L");
    query.setFilter(filterLogic);

    if (!extraFilters.isEmpty()) {
      FilterLogic extraFilterLogic = convertToFilterLogic(extraFilters, featureLookup, "L");
      queryController.appendFilter(query, SqlFilterLogic.AND, extraFilterLogic);
    }

    return query;
  }

  /**
   * Reconstruct {@link io.hops.hopsworks.common.featurestore.query.filter.FilterLogic} from a list of
   * {@link io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetFilter} entity
   * Logic:
   * 1. get head node
   * 2. if type is  single, return Filter
   *    else get left/right children and assign left/right filterLogic
   *
   * @param trainingDatasetFilters
   * @param features
   * @param headPath
   * @return filter logic
   * @throws FeaturestoreException
   */
  public FilterLogic convertToFilterLogic(Collection<TrainingDatasetFilter> trainingDatasetFilters,
      Map<String, Feature> features, String headPath) throws FeaturestoreException {

    if (trainingDatasetFilters == null || trainingDatasetFilters.size() == 0) {
      return null;
    }
    FilterLogic filterLogic = new FilterLogic();
    TrainingDatasetFilter headNode = trainingDatasetFilters.stream()
        .filter(filter -> filter.getPath().equals(headPath)).findFirst()
        .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_GET_QUERY_FILTER,
            Level.WARNING));
    filterLogic.setType(headNode.getType());

    if (headNode.getType().equals(SqlFilterLogic.SINGLE)) {
      Filter filter = convertToFilter(headNode.getCondition(), features);
      filterLogic.setLeftFilter(filter);
    } else {
      List<TrainingDatasetFilter> leftChildren = trainingDatasetFilters.stream()
          .filter(filter -> filter.getPath().startsWith(headPath+".L"))
          .collect(Collectors.toList());
      List<TrainingDatasetFilter> rightChildren = trainingDatasetFilters.stream()
          .filter(filter -> filter.getPath().startsWith(headPath+".R"))
          .collect(Collectors.toList());
      if (!leftChildren.isEmpty()) {
        if (leftChildren.size() == 1) {
          filterLogic.setLeftFilter(convertToFilter(leftChildren.get(0).getCondition(), features));
        } else {
          filterLogic.setLeftLogic(convertToFilterLogic(leftChildren, features, headPath + ".L"));
        }
      }
      if (!rightChildren.isEmpty()) {
        if (rightChildren.size() == 1) {
          filterLogic.setRightFilter(convertToFilter(rightChildren.get(0).getCondition(), features));
        } else {
          filterLogic.setRightLogic(convertToFilterLogic(rightChildren, features, headPath + ".R"));
        }
      }
    }
    return filterLogic;
  }

  private Filter convertToFilter(TrainingDatasetFilterCondition condition, Map<String, Feature> features) {
    FilterValue filterValue;
    if (condition.getValueFeatureGroupId() == null) {
      filterValue = new FilterValue(condition.getValue());
    } else {
      Feature filterValueFeature = features.get(
          makeFeatureLookupKey(condition.getValueFeatureGroupId(), condition.getValue()));
      filterValue = new FilterValue(
          condition.getValueFeatureGroupId(), filterValueFeature.getFgAlias(), condition.getValue());
    }
    return new Filter(
        features.get(makeFeatureLookupKey(condition.getFeatureGroup().getId(), condition.getFeature())),
        condition.getCondition(),
        filterValue
    );
  }

  private String makeFeatureLookupKey(Integer featureGroupId, String featureName) {
    return featureGroupId + "." + featureName;
  }

  public Map<Integer, String> getAliasLookupTable(List<TrainingDatasetJoin> tdJoins) {
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
  public Map<Integer, String> getFsLookupTableJoins(List<TrainingDatasetJoin> tdJoins) {
    Map<Integer, String> fsLookup = new HashMap<>();
    for (TrainingDatasetJoin join : tdJoins) {
      if (!fsLookup.containsKey(join.getFeatureGroup().getFeaturestore().getId())) {
        fsLookup.put(join.getFeatureGroup().getFeaturestore().getId(),
            featurestoreFacade.getHiveDbName(join.getFeatureGroup().getFeaturestore().getHiveDbId()));
      }
    }

    return fsLookup;
  }

  public Map<Integer, String> getFsLookupTableFeatures(List<TrainingDatasetFeature> tdFeatures) {
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

  public List<TrainingDatasetFeature> getFeaturesSorted(TrainingDataset trainingDataset, boolean withLabel) {
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

  public List<TrainingDatasetJoin> getJoinsSorted(TrainingDataset trainingDataset) {
    return getJoinsSorted(trainingDataset.getJoins());
  }

  public List<TrainingDatasetJoin> getJoinsSorted(Collection<TrainingDatasetJoin> joins) {
    return joins.stream()
        .sorted(Comparator.comparing(TrainingDatasetJoin::getIndex))
        .collect(Collectors.toList());
  }

  // Rebuild query object so that the query constructor can be build the string
  public Join getQueryJoin(Query leftQuery, TrainingDatasetJoin rightTdJoin, Map<Integer, String> fgAliasLookup,
    Map<Integer, String> fsLookup, Map<Integer, List<Feature>> availableFeaturesLookup, Boolean isHiveEngine)
      throws FeaturestoreException {

    String rightAs = fgAliasLookup.get(rightTdJoin.getId());
    Query rightQuery = new Query(
        fsLookup.get(rightTdJoin.getFeatureGroup().getFeaturestore().getId()),
        onlineFeaturestoreController
            .getOnlineFeaturestoreDbName(rightTdJoin.getFeatureGroup().getFeaturestore().getProject()),
        rightTdJoin.getFeatureGroup(),
        rightAs,
        // no requested features as they are all in the left base query
        new ArrayList<>(),
        availableFeaturesLookup.get(rightTdJoin.getFeatureGroup().getId()),
        isHiveEngine);

    List<Feature> leftOn = rightTdJoin.getConditions().stream()
      .map(c -> new Feature(c.getLeftFeature())).collect(Collectors.toList());

    List<Feature> rightOn = rightTdJoin.getConditions().stream()
      .map(c -> new Feature(c.getRightFeature())).collect(Collectors.toList());

    JoinType joinType = JoinType.values()[rightTdJoin.getType()];
    return queryController.extractLeftRightOn(leftQuery, rightQuery, leftOn, rightOn, joinType,
        rightTdJoin.getPrefix());
  }

  private TransformationFunction getTransformationFunction(
      Feature feature, List<TrainingDatasetFeatureDTO> featureDTOs) throws FeaturestoreException {
    TrainingDatasetFeatureDTO featureDTO = featureDTOs.stream().filter(dto ->
        feature.getName().equals(dto.getFeatureGroupFeatureName())).findFirst().orElse(null);
    TransformationFunction transformationFunction = null;
    if (featureDTO != null && featureDTO.getTransformationFunction() != null){
      transformationFunction = transformationFunctionFacade.findById(featureDTO.getTransformationFunction().getId())
          .orElseThrow(() ->
              new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRANSFORMATION_FUNCTION_DOES_NOT_EXIST,
                  Level.FINE, "Could not find transformation function with ID" +
                  featureDTO.getTransformationFunction().getId()));
    }
    return transformationFunction;
  }

  public String checkPrefix(TrainingDatasetFeature feature) {
    if (feature.getTrainingDatasetJoin() != null && feature.getTrainingDatasetJoin().getPrefix() != null){
      return feature.getTrainingDatasetJoin().getPrefix() + feature.getName();
    } else {
      return feature.getName();
    }
  }
}
