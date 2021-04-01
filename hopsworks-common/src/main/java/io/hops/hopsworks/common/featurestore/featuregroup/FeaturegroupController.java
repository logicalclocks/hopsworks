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

package io.hops.hopsworks.common.featurestore.featuregroup;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.featurestore.FeaturestoreFacade;
import io.hops.hopsworks.common.featurestore.activity.FeaturestoreActivityFacade;
import io.hops.hopsworks.common.featurestore.datavalidation.FeatureGroupExpectationFacade;
import io.hops.hopsworks.common.featurestore.datavalidation.FeatureGroupValidationsController;
import io.hops.hopsworks.common.featurestore.datavalidation.FeatureStoreExpectationFacade;
import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.CachedFeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.CachedFeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.CachedFeaturegroupFacade;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.FeaturegroupPreview;
import io.hops.hopsworks.common.featurestore.featuregroup.ondemand.OnDemandFeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.ondemand.OnDemandFeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.online.OnlineFeaturegroupController;
import io.hops.hopsworks.common.featurestore.statistics.StatisticsController;
import io.hops.hopsworks.common.featurestore.statistics.columns.StatisticColumnController;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorController;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.common.featurestore.utils.FeaturestoreInputValidation;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.provenance.core.HopsFSProvenanceController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.exceptions.SchemaException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.activity.FeaturestoreActivityMeta;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.FeaturegroupType;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.CachedFeaturegroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.HivePartitions;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.ValidationType;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.FeatureGroupExpectation;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.FeatureStoreExpectation;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.ondemand.OnDemandFeaturegroup;
import io.hops.hopsworks.persistence.entity.featurestore.statistics.StatisticColumn;
import io.hops.hopsworks.persistence.entity.featurestore.statistics.StatisticsConfig;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Class controlling the interaction with the feature_group table and required business logic
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeaturegroupController {
  @EJB
  private FeaturegroupFacade featuregroupFacade;
  @EJB
  private CachedFeaturegroupController cachedFeaturegroupController;
  @EJB
  private OnDemandFeaturegroupController onDemandFeaturegroupController;
  @EJB
  private FeaturestoreFacade featurestoreFacade;
  @EJB
  private StatisticColumnController statisticColumnController;
  @EJB
  private FeaturestoreInputValidation featurestoreInputValidation;
  @EJB
  private CachedFeaturegroupFacade cachedFeaturegroupFacade;
  @EJB
  private HopsFSProvenanceController fsController;
  @EJB
  private StatisticsController statisticsController;
  @EJB
  private FeatureGroupValidationsController featureGroupValidationsController;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private Settings settings;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private InodeController inodeController;
  @EJB
  private OnlineFeaturegroupController onlineFeaturegroupController;
  @EJB
  private FeaturestoreActivityFacade fsActivityFacade;
  @EJB
  private FeaturestoreStorageConnectorController connectorController;
  @EJB
  private FeatureGroupExpectationFacade featureGroupExpectationFacade;
  @EJB
  private FeatureStoreExpectationFacade featureStoreExpectationFacade;

  /**
   * Gets all featuregroups for a particular featurestore and project, using the userCerts to query Hive
   *
   * @param featurestore featurestore to query featuregroups for
   * @return list of XML/JSON DTOs of the featuregroups
   */
  public List<FeaturegroupDTO> getFeaturegroupsForFeaturestore(Featurestore featurestore, Project project, Users user)
          throws FeaturestoreException, ServiceException {
    return getFeaturegroupsForFeaturestore(featurestore, project, user, null);
  }

  /**
   * Gets all featuregroups for a particular featurestore and project, using the userCerts to query Hive
   *
   * @param featurestore featurestore to query featuregroups for
   * @return list of XML/JSON DTOs of the featuregroups
   */
  public List<FeaturegroupDTO> getFeaturegroupsForFeaturestore(Featurestore featurestore, Project project, Users user,
                                                               Set<String> expectationNames)
    throws FeaturestoreException, ServiceException {
    List<Featuregroup> featuregroups = new ArrayList<>();
    if (expectationNames != null && !expectationNames.isEmpty()) {
      for(String name : expectationNames) {
        for (FeatureGroupExpectation featureGroupExpectation :
                featureStoreExpectationFacade
                        .findByFeaturestoreAndName(featurestore, name)
                        .orElseThrow(() -> new FeaturestoreException(
                                RESTCodes.FeaturestoreErrorCode.FEATURE_STORE_EXPECTATION_NOT_FOUND,
                                Level.FINE, name))
                        .getFeatureGroupExpectations()) {
          if (featuregroups.isEmpty()) {
            featuregroups.add(featureGroupExpectation.getFeaturegroup());
          } else {
            boolean found = featuregroups
                              .stream()
                              .anyMatch(fg -> fg.getId().equals(featureGroupExpectation.getFeaturegroup().getId()));
            if (!found) {
              featuregroups.add(featureGroupExpectation.getFeaturegroup());
            }
          }
        }
      }
    } else {
      featuregroups = featuregroupFacade.findByFeaturestore(featurestore);
    }
    List<FeaturegroupDTO> featuregroupDTOS = new ArrayList<>();
    for (Featuregroup featuregroup : featuregroups) {
      featuregroupDTOS.add(convertFeaturegrouptoDTO(featuregroup, project, user));
    }
    return featuregroupDTOS;
  }

  /**
   * Clears the contents of a feature group (obviously only works for cached feature groups)
   *
   * @param featuregroup
   * @param project
   * @param user            the user making the request
   * @return a DTO representation of the cleared feature group
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   * @throws SQLException
   */
  public FeaturegroupDTO clearFeaturegroup(Featuregroup featuregroup, Project project, Users user)
      throws FeaturestoreException, SQLException, ProvenanceException, IOException, ServiceException,
      KafkaException, SchemaException, ProjectException, UserException, HopsSecurityException {
    switch (featuregroup.getFeaturegroupType()) {
      case CACHED_FEATURE_GROUP:
        FeaturegroupDTO featuregroupDTO = convertFeaturegrouptoDTO(featuregroup, project, user);
        deleteFeaturegroup(featuregroup, project, user);
        return createFeaturegroupNoValidation(featuregroup.getFeaturestore(), featuregroupDTO, project, user);
      case ON_DEMAND_FEATURE_GROUP:
        throw new FeaturestoreException(
            RESTCodes.FeaturestoreErrorCode.CLEAR_OPERATION_NOT_SUPPORTED_FOR_ON_DEMAND_FEATUREGROUPS,
            Level.FINE, "featuregroupId: " + featuregroup.getId());
      default:
        throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_TYPE.getMessage()
            + ", Recognized Feature group types are: " + FeaturegroupType.ON_DEMAND_FEATURE_GROUP + ", and: " +
            FeaturegroupType.CACHED_FEATURE_GROUP + ". The provided feature group type was not recognized: "
            + featuregroup.getFeaturegroupType());
    }
  }


  /**
   * Creates a new feature group in a featurestore
   * @param featurestore
   * @param featuregroupDTO
   * @param project
   * @param user
   * @return
   * @throws FeaturestoreException
   * @throws SQLException
   * @throws ProvenanceException
   * @throws ServiceException
   */
  public FeaturegroupDTO createFeaturegroup(Featurestore featurestore, FeaturegroupDTO featuregroupDTO,
                                            Project project, Users user)
      throws FeaturestoreException, ServiceException, SQLException, ProvenanceException, IOException,
      KafkaException, SchemaException, ProjectException, UserException, HopsSecurityException {

    // if version not provided, get latest and increment
    if (featuregroupDTO.getVersion() == null) {
      // returns ordered list by desc version
      List<Featuregroup> fgPrevious = featuregroupFacade.findByNameAndFeaturestoreOrderedDescVersion(
        featuregroupDTO.getName(), featurestore);
      if (fgPrevious != null && !fgPrevious.isEmpty()) {
        featuregroupDTO.setVersion(fgPrevious.get(0).getVersion() + 1);
      } else {
        featuregroupDTO.setVersion(1);
      }
    }

    verifyFeatureGroupInput(featuregroupDTO);
    verifyFeaturesNoDefaultValue(featuregroupDTO.getFeatures());
    return createFeaturegroupNoValidation(featurestore, featuregroupDTO, project, user);
  }

  public FeaturegroupDTO createFeaturegroupNoValidation(Featurestore featurestore, FeaturegroupDTO featuregroupDTO,
                                                      Project project, Users user)
      throws FeaturestoreException, SQLException, ProvenanceException, ServiceException, KafkaException,
      SchemaException, ProjectException, UserException, IOException, HopsSecurityException {

    //Persist specific feature group metadata (cached fg or on-demand fg)
    OnDemandFeaturegroup onDemandFeaturegroup = null;
    CachedFeaturegroup cachedFeaturegroup = null;
    if (featuregroupDTO instanceof CachedFeaturegroupDTO) {
      cachedFeaturegroup = cachedFeaturegroupController.createCachedFeaturegroup(featurestore,
          (CachedFeaturegroupDTO) featuregroupDTO, project, user);
    } else {
      onDemandFeaturegroup = onDemandFeaturegroupController.createOnDemandFeaturegroup(featurestore,
          (OnDemandFeaturegroupDTO) featuregroupDTO, project, user);
    }

    //Persist basic feature group metadata
    Featuregroup featuregroup = persistFeaturegroupMetadata(featurestore, user, featuregroupDTO,
      cachedFeaturegroup, onDemandFeaturegroup);

    FeaturegroupDTO completeFeaturegroupDTO = convertFeaturegrouptoDTO(featuregroup, project, user);

    //Extract metadata
    String hdfsUsername = hdfsUsersController.getHdfsUserName(project, user);
    DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUsername);
    try {
      String fgPath = Utils.getFeaturestorePath(featurestore.getProject(), settings)
          + "/" + Utils.getFeaturegroupName(featuregroup);
      fsController.featuregroupAttachXAttrs(fgPath, completeFeaturegroupDTO, udfso);
    } finally {
      dfs.closeDfsClient(udfso);
    }

    // Log activity
    fsActivityFacade.logMetadataActivity(user, featuregroup, FeaturestoreActivityMeta.FG_CREATED, null);

    return completeFeaturegroupDTO;
  }

  /**
   * Convert a featuregroup entity to a DTO representation
   *
   * @param featuregroup the entity to convert
   * @return a DTO representation of the entity
   */
  private FeaturegroupDTO convertFeaturegrouptoDTO(Featuregroup featuregroup, Project project, Users user)
    throws FeaturestoreException, ServiceException {
    String featurestoreName = featurestoreFacade.getHiveDbName(featuregroup.getFeaturestore().getHiveDbId());
    switch (featuregroup.getFeaturegroupType()) {
      case CACHED_FEATURE_GROUP:
        CachedFeaturegroupDTO cachedFeaturegroupDTO =
          cachedFeaturegroupController.convertCachedFeaturegroupToDTO(featuregroup, project, user);
        cachedFeaturegroupDTO.setFeaturestoreName(featurestoreName);
        return cachedFeaturegroupDTO;
      case ON_DEMAND_FEATURE_GROUP:
        FeaturestoreStorageConnectorDTO storageConnectorDTO =
            connectorController.convertToConnectorDTO(user, project,
                    featuregroup.getOnDemandFeaturegroup().getFeaturestoreConnector());
        return new OnDemandFeaturegroupDTO(featurestoreName, featuregroup, storageConnectorDTO);
      default:
        throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_TYPE.getMessage()
            + ", Recognized Feature group types are: " + FeaturegroupType.ON_DEMAND_FEATURE_GROUP + ", and: " +
            FeaturegroupType.CACHED_FEATURE_GROUP + ". The provided feature group type was not recognized: "
            + featuregroup.getFeaturegroupType());
    }
  }

  /**
   * Retrieves a list of feature groups with a specific name from a specific feature store
   *
   * @param name name of the featuregroup
   * @param featurestore the featurestore that the featuregroup belongs to
   * @return XML/JSON DTO of the featuregroup
   */
  public List<FeaturegroupDTO> getFeaturegroupWithNameAndFeaturestore(Featurestore featurestore, String name,
                                                                      Project project, Users user)
      throws FeaturestoreException, ServiceException {
    List<Featuregroup> featuregroups = verifyFeaturegroupName(featurestore, name);
    List<FeaturegroupDTO> featuregroupDTOS = new ArrayList<>();
    for (Featuregroup featuregroup : featuregroups) {
      featuregroupDTOS.add(convertFeaturegrouptoDTO(featuregroup, project, user));
    }
    return featuregroupDTOS;
  }

  /**
   * Retrieves a list of feature groups with a specific name from a specific feature store
   *
   * @param name name of the featuregroup
   * @param featurestore the featurestore that the featuregroup belongs to
   * @return XML/JSON DTO of the featuregroup
   */
  public FeaturegroupDTO getFeaturegroupWithNameVersionAndFeaturestore(Featurestore featurestore, String name,
                                                                       Integer version, Project project, Users user)
      throws FeaturestoreException, ServiceException {
    Featuregroup featuregroup = verifyFeaturegroupNameVersion(featurestore, name, version);
    return convertFeaturegrouptoDTO(featuregroup, project, user);
  }

  /**
   * Retrieves a featuregroup with a particular id from a particular featurestore
   *
   * @param id           id of the featuregroup
   * @param featurestore the featurestore that the featuregroup belongs to
   * @return XML/JSON DTO of the featuregroup
   */
  public FeaturegroupDTO getFeaturegroupWithIdAndFeaturestore(Featurestore featurestore, Integer id, Project project,
                                                              Users user)
      throws FeaturestoreException, ServiceException {
    Featuregroup featuregroup = getFeaturegroupById(featurestore, id);
    return convertFeaturegrouptoDTO(featuregroup, project, user);
  }

  /**
   * Updates metadata for a featuregroup
   *
   * @param featurestore    the featurestore where the featuregroup resides
   * @param featuregroupDTO the updated featuregroup metadata
   * @return DTO of the updated feature group
   * @throws FeaturestoreException
   */
  public FeaturegroupDTO updateFeaturegroupMetadata(Project project, Users user, Featurestore featurestore,
                                                    FeaturegroupDTO featuregroupDTO)
      throws FeaturestoreException, SQLException, ProvenanceException, ServiceException, SchemaException,
      KafkaException {
    Featuregroup featuregroup = getFeaturegroupById(featurestore, featuregroupDTO.getId());
    // Verify general entity related information
    featurestoreInputValidation.verifyDescription(featuregroupDTO);
    featurestoreInputValidation.verifyFeatureGroupFeatureList(featuregroupDTO.getFeatures());

    // Update on-demand feature group metadata
    if (featuregroup.getFeaturegroupType() == FeaturegroupType.CACHED_FEATURE_GROUP) {
      cachedFeaturegroupController
          .updateMetadata(project, user, featuregroup, (CachedFeaturegroupDTO) featuregroupDTO);
    } else if (featuregroup.getFeaturegroupType() == FeaturegroupType.ON_DEMAND_FEATURE_GROUP) {
      onDemandFeaturegroupController.updateOnDemandFeaturegroupMetadata(featuregroup.getOnDemandFeaturegroup(),
        (OnDemandFeaturegroupDTO) featuregroupDTO);
    }

    // get feature group object again after alter table
    featuregroup = getFeaturegroupById(featurestore, featuregroupDTO.getId());
    featuregroupDTO = convertFeaturegrouptoDTO(featuregroup, project, user);

    String hdfsUsername = hdfsUsersController.getHdfsUserName(project, user);
    DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUsername);
    try {
      String fgPath = Utils.getFeaturestorePath(featurestore.getProject(), settings)
          + "/" + Utils.getFeaturegroupName(featuregroup);
      fsController.featuregroupAttachXAttrs(fgPath, featuregroupDTO, udfso);
    } finally {
      dfs.closeDfsClient(udfso);
    }

    return featuregroupDTO;
  }

  /**
   * Enable online feature serving of a feature group that is currently only offline
   *
   * @param featurestore    the featurestore where the featuregroup resides
   * @param featuregroupDTO the updated featuregroup metadata
   * @return DTO of the updated feature group
   * @throws FeaturestoreException
   */
  public FeaturegroupDTO enableFeaturegroupOnline(Featurestore featurestore, FeaturegroupDTO featuregroupDTO,
                                                  Project project, Users user)
      throws FeaturestoreException, SQLException, ServiceException, KafkaException,
      SchemaException, ProjectException, UserException, IOException, HopsSecurityException {
    Featuregroup featuregroup = getFeaturegroupById(featurestore, featuregroupDTO.getId());
    if(featuregroup.getFeaturegroupType() == FeaturegroupType.ON_DEMAND_FEATURE_GROUP){
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.ONLINE_FEATURE_SERVING_NOT_SUPPORTED_FOR_ON_DEMAND_FEATUREGROUPS, Level.FINE,
        ", Online feature serving is only supported for featuregroups of type: "
          + FeaturegroupType.CACHED_FEATURE_GROUP + ", and the user requested to enable feature serving on a " +
          "featuregroup with type:" + FeaturegroupType.ON_DEMAND_FEATURE_GROUP);
    }
    cachedFeaturegroupController.enableFeaturegroupOnline(featurestore, featuregroup, project, user);

    // Log activity
    fsActivityFacade.logMetadataActivity(user, featuregroup, FeaturestoreActivityMeta.ONLINE_ENABLED, null);

    return convertFeaturegrouptoDTO(featuregroup, project, user);
  }

  /**
   * Disable online feature serving of a feature group
   *
   * @param featuregroup
   * @param project
   * @return DTO of the updated feature group
   * @throws FeaturestoreException
   */
  public FeaturegroupDTO disableFeaturegroupOnline(Featuregroup featuregroup, Project project, Users user)
      throws FeaturestoreException, SQLException, ServiceException, SchemaException, KafkaException {
    if(featuregroup.getFeaturegroupType() == FeaturegroupType.ON_DEMAND_FEATURE_GROUP) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.ONLINE_FEATURE_SERVING_NOT_SUPPORTED_FOR_ON_DEMAND_FEATUREGROUPS, Level.FINE,
        ", Online feature serving is only supported for featuregroups of type: "
          + FeaturegroupType.CACHED_FEATURE_GROUP + ", and the user requested to a feature serving operation on a " +
          "featuregroup with type:" + FeaturegroupType.ON_DEMAND_FEATURE_GROUP);
    }
    cachedFeaturegroupController.disableFeaturegroupOnline(featuregroup, project, user);

    // Log activity
    fsActivityFacade.logMetadataActivity(user, featuregroup, FeaturestoreActivityMeta.ONLINE_DISABLED, null);

    return convertFeaturegrouptoDTO(featuregroup, project, user);
  }

  /**
   * Updates statistics settings for a featuregroup
   *
   * @param featurestore    the featurestore where the featuregroup resides
   * @param featureGroupDTO a DTO containing the updated featuregroup stats
   * @return DTO of the updated feature group
   * @throws FeaturestoreException
   */
  public FeaturegroupDTO updateFeatureGroupStatsConfig(Featurestore featurestore, FeaturegroupDTO featureGroupDTO,
    Project project, Users user) throws FeaturestoreException, ServiceException {
    Featuregroup featuregroup = getFeaturegroupById(featurestore, featureGroupDTO.getId());
    if (featureGroupDTO.getStatisticsConfig().getEnabled() != null) {
      featuregroup.getStatisticsConfig().setDescriptive(featureGroupDTO.getStatisticsConfig().getEnabled());
    }
    if (featureGroupDTO.getStatisticsConfig().getHistograms() != null) {
      featuregroup.getStatisticsConfig().setHistograms(featureGroupDTO.getStatisticsConfig().getHistograms());
    }
    if (featureGroupDTO.getStatisticsConfig().getCorrelations() != null) {
      featuregroup.getStatisticsConfig().setCorrelations(featureGroupDTO.getStatisticsConfig().getCorrelations());
    }
    // compare against schema from database, as client doesn't need to send schema in update request
    statisticColumnController.verifyStatisticColumnsExist(featureGroupDTO, featuregroup, getFeatures(featuregroup,
      project, user));
    featuregroupFacade.updateFeaturegroupMetadata(featuregroup);
    statisticColumnController.persistStatisticColumns(featuregroup, featureGroupDTO.getStatisticsConfig().getColumns());
    // get feature group again with persisted columns - this trip to the database can be saved
    featuregroup = getFeaturegroupById(featurestore, featureGroupDTO.getId());
    return convertFeaturegrouptoDTO(featuregroup, project, user);
  }

  /**
   * Updated validation type for a featuregroup
   *
   * @param featuregroup    the feature group to update
   * @return DTO of the updated feature group
   * @throws FeaturestoreException
   */
  public FeaturegroupDTO updateValidationType(Featuregroup featuregroup,
                                              ValidationType validationType, Project project,
                                              Users user) throws FeaturestoreException, ServiceException {
    Featuregroup toUpdate = featuregroupFacade.findByNameVersionAndFeaturestore(featuregroup.getName(),
            featuregroup.getVersion(), featuregroup.getFeaturestore()).orElseThrow(() ->
            new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_NOT_FOUND, Level.FINE,
                    "featuregroup: " + featuregroup.getName()));
    toUpdate.setValidationType(validationType);
    featuregroupFacade.updateFeaturegroupMetadata(toUpdate);
    return convertFeaturegrouptoDTO(featuregroup, project, user);
  }

  /**
   * Check if the feature group described by the DTO exists
   *
   * @param featurestore    the featurestore that the featuregroup belongs to
   * @param featuregroupDTO DTO representation of the feature group
   * @return
   */
  public boolean featuregroupExists(Featurestore featurestore, FeaturegroupDTO featuregroupDTO) {
    if (!Strings.isNullOrEmpty(featuregroupDTO.getName()) && featuregroupDTO.getVersion() != null) {
      return featuregroupFacade.findByNameVersionAndFeaturestore(featuregroupDTO.getName(),
        featuregroupDTO.getVersion(), featurestore).isPresent();
    }
    return false;
  }

  /**
   * Deletes a featuregroup with a particular id or name from a featurestore
   * @param featuregroup
   * @param project
   * @param user
   * @return
   * @throws SQLException
   * @throws FeaturestoreException
   * @throws ServiceException
   * @throws IOException
   */
  public void deleteFeaturegroup(Featuregroup featuregroup, Project project, Users user)
    throws SQLException, FeaturestoreException, ServiceException, IOException, SchemaException, KafkaException {
    switch (featuregroup.getFeaturegroupType()) {
      case CACHED_FEATURE_GROUP:
        //Delete hive_table will cascade to cached_featuregroup_table which will cascade to feature_group table
        cachedFeaturegroupController.dropHiveFeaturegroup(featuregroup, project, user);
        //Delete mysql table and metadata
        if(settings.isOnlineFeaturestore() && featuregroup.getCachedFeaturegroup().isOnlineEnabled()) {
          onlineFeaturegroupController.disableOnlineFeatureGroup(featuregroup, project, user);
        }
        break;
      case ON_DEMAND_FEATURE_GROUP:
        //Delete on_demand_feature_group will cascade will cascade to feature_group table
        onDemandFeaturegroupController
            .removeOnDemandFeaturegroup(featuregroup.getFeaturestore(), featuregroup, project, user);
        break;
      default:
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_TYPE, Level.FINE,
          ", Recognized Feature group types are: " + FeaturegroupType.ON_DEMAND_FEATURE_GROUP + ", and: " +
            FeaturegroupType.CACHED_FEATURE_GROUP + ". The provided feature group type was not recognized: "
            + featuregroup.getFeaturegroupType());
    }

    // Statistics files need to be deleted explicitly
    statisticsController.deleteStatistics(project, user, featuregroup);
  }


  /**
   * Previews a given featuregroup by doing a SELECT LIMIT query on the Hive and MySQL Tables
   *
   * @param featuregroup
   * @param project
   * @param user
   * @param partition
   * @param online
   * @param limit
   * @return
   * @throws SQLException
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  public FeaturegroupPreview getFeaturegroupPreview(Featuregroup featuregroup, Project project, Users user,
                                                    String partition, boolean online, int limit)
      throws SQLException, FeaturestoreException, HopsSecurityException {
    switch (featuregroup.getFeaturegroupType()) {
      case CACHED_FEATURE_GROUP:
        return cachedFeaturegroupController.getFeaturegroupPreview(featuregroup, project, user,
            partition, online, limit);
      case ON_DEMAND_FEATURE_GROUP:
        throw new FeaturestoreException(
            RESTCodes.FeaturestoreErrorCode.PREVIEW_NOT_SUPPORTED_FOR_ON_DEMAND_FEATUREGROUPS,
            Level.FINE, "featuregroupId: " + featuregroup.getId());
      default:
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_TYPE, Level.FINE,
            ", Recognized Feature group types are: " + FeaturegroupType.ON_DEMAND_FEATURE_GROUP + ", and: " +
            FeaturegroupType.CACHED_FEATURE_GROUP + ". The provided feature group type was not recognized: "
            + featuregroup.getFeaturegroupType());
    }
  }

  /**
   * Get a list of partitions for offline feature groups
   * @param featuregroup
   * @return
   * @throws FeaturestoreException in case the feature group is not offline
   */
  public List<HivePartitions> getPartitions(Featuregroup featuregroup, Integer offset, Integer limit)
      throws FeaturestoreException {
    switch (featuregroup.getFeaturegroupType()) {
      case CACHED_FEATURE_GROUP:
        return cachedFeaturegroupFacade.getHiveTablePartitions(
            featuregroup.getCachedFeaturegroup().getHiveTbls(), offset, limit);
      case ON_DEMAND_FEATURE_GROUP:
        throw new FeaturestoreException(
            RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_ONDEMAND_NO_PARTS, Level.FINE,
            "featuregroupId: " + featuregroup.getId());
      default:
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_TYPE, Level.FINE,
            ", Recognized Feature group types are: " + FeaturegroupType.ON_DEMAND_FEATURE_GROUP + ", and: " +
                FeaturegroupType.CACHED_FEATURE_GROUP + ". The provided feature group type was not recognized: "
                + featuregroup.getFeaturegroupType());
    }
  }

  /**
   * Verifies the id of a feature group
   *
   * @param featurestore the featurestore to query
   * @param featuregroupId the id of the feature group
   * @return the featuregroup with the id if it passed the validation
   */
  public Featuregroup getFeaturegroupById(Featurestore featurestore, Integer featuregroupId)
      throws FeaturestoreException {
    return featuregroupFacade.findByIdAndFeaturestore(featuregroupId, featurestore)
        .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_NOT_FOUND, Level.FINE,
            "Feature group id: " + featuregroupId));
  }

  /**
   * Verifies the name of a feature group
   *
   * @param featurestore the featurestore to query
   * @param featureGroupName the name of the feature group
   * @return the featuregroup with the id if it passed the validation
   */
  private List<Featuregroup> verifyFeaturegroupName(Featurestore featurestore, String featureGroupName) {
    List<Featuregroup> featuregroup = featuregroupFacade.findByNameAndFeaturestore(featureGroupName, featurestore);
    if (featuregroup == null || featuregroup.isEmpty()) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_NOT_FOUND +
        " feature group name " + featureGroupName);
    }
    return featuregroup;
  }

  /**
   * Verifies the name and version of a feature group
   *
   * @param featurestore the featurestore to query
   * @param featureGroupName the name of the feature group
   * @param version the version of the feature group
   * @return the featuregroup with the id if it passed the validation
   */
  public Featuregroup verifyFeaturegroupNameVersion(Featurestore featurestore, String featureGroupName,
                                                     Integer version) throws FeaturestoreException {
    return featuregroupFacade.findByNameVersionAndFeaturestore(featureGroupName, version, featurestore)
        .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_NOT_FOUND, Level.FINE,
        "feature group name: " + featureGroupName + " feature group version: " + version));
  }

  /**
   * Persists metadata of a new feature group in the feature_group table
   *
   * @param featurestore the featurestore of the feature group
   * @param user the Hopsworks user making the request
   * @param featuregroupDTO DTO of the feature group
   * @param cachedFeaturegroup the cached feature group that the feature group is linked to (if any)
   * @param onDemandFeaturegroup the on-demand feature group that the feature group is linked to (if any)
   * @return the created entity
   */
  private Featuregroup persistFeaturegroupMetadata(Featurestore featurestore, Users user,
                                                   FeaturegroupDTO featuregroupDTO,
                                                   CachedFeaturegroup cachedFeaturegroup,
                                                   OnDemandFeaturegroup onDemandFeaturegroup)
                                                   throws FeaturestoreException {
    Featuregroup featuregroup = new Featuregroup();
    featuregroup.setName(featuregroupDTO.getName());
    featuregroup.setFeaturestore(featurestore);
    featuregroup.setCreated(new Date());
    featuregroup.setCreator(user);
    featuregroup.setVersion(featuregroupDTO.getVersion());
    if (featuregroupDTO.getValidationType() != null) {
      featuregroup.setValidationType(featuregroupDTO.getValidationType());
    }
    featuregroup.setFeaturegroupType(
      featuregroupDTO instanceof CachedFeaturegroupDTO ?
        FeaturegroupType.CACHED_FEATURE_GROUP :
        FeaturegroupType.ON_DEMAND_FEATURE_GROUP);

    featuregroup.setCachedFeaturegroup(cachedFeaturegroup);
    featuregroup.setOnDemandFeaturegroup(onDemandFeaturegroup);

    StatisticsConfig statisticsConfig = new StatisticsConfig(featuregroupDTO.getStatisticsConfig().getEnabled(),
      featuregroupDTO.getStatisticsConfig().getCorrelations(), featuregroupDTO.getStatisticsConfig().getHistograms());
    statisticsConfig.setFeaturegroup(featuregroup);
    statisticsConfig.setStatisticColumns(featuregroupDTO.getStatisticsConfig().getColumns().stream()
      .map(sc -> new StatisticColumn(statisticsConfig, sc)).collect(Collectors.toList()));
    featuregroup.setStatisticsConfig(statisticsConfig);
    if (featuregroupDTO.getExpectationsNames() != null ) {
      List<FeatureGroupExpectation> featureGroupExpectations = new ArrayList<>();
      for (String name : featuregroupDTO.getExpectationsNames()) {
        FeatureStoreExpectation featureStoreExpectation =
                featureGroupValidationsController.getFeatureStoreExpectation(featuregroup.getFeaturestore(), name);
        FeatureGroupExpectation featureGroupExpectation;
        Optional<FeatureGroupExpectation> e =
                featureGroupExpectationFacade.findByFeaturegroupAndExpectation(featuregroup, featureStoreExpectation);
        featureGroupValidationsController.checkFeaturesExist(featureStoreExpectation, featuregroup, name,
                featurestore.getProject(), user);
        if (!e.isPresent()) {
          featureGroupExpectation = new FeatureGroupExpectation();
          featureGroupExpectation.setFeaturegroup(featuregroup);
          featureGroupExpectation.setFeatureStoreExpectation(featureStoreExpectation);
        } else {
          featureGroupExpectation = e.get();
        }
        featureGroupExpectations.add(featureGroupExpectation);
      }
      featuregroup.setExpectations(featureGroupExpectations);
    }

    featuregroupFacade.persist(featuregroup);
    return featuregroup;
  }

  public List<FeatureGroupFeatureDTO> getFeatures(Featuregroup featuregroup, Project project, Users user)
    throws FeaturestoreException {
    switch (featuregroup.getFeaturegroupType()) {
      case CACHED_FEATURE_GROUP:
        return cachedFeaturegroupController.getFeaturesDTO(featuregroup, project, user);
      case ON_DEMAND_FEATURE_GROUP:
        return featuregroup.getOnDemandFeaturegroup().getFeatures().stream()
          .map(f -> new FeatureGroupFeatureDTO(f.getName(), f.getType(), f.getPrimary(), null, featuregroup.getId()))
          .collect(Collectors.toList());
    }
    return new ArrayList<>();
  }

  public String getFeatureGroupLocation(Featuregroup featureGroup) {
    // Cached feature groups also have a `location` field.
    // the issue is that the host is slightly different due to a configuration of Hive
    // so here we resolve only the path based on the indoe
    return inodeController.getPath(featureGroup.getFeaturegroupType() == FeaturegroupType.CACHED_FEATURE_GROUP ?
        featureGroup.getCachedFeaturegroup().getHiveTbls().getSdId().getInode() :
        featureGroup.getOnDemandFeaturegroup().getInode());
  }

  /**
   * Verify feature group specific input
   *
   * @param featureGroupDTO the provided user input
   * @throws FeaturestoreException
   */
  private void verifyFeatureGroupInput(FeaturegroupDTO featureGroupDTO)
    throws FeaturestoreException {
    // Verify general entity related information
    featurestoreInputValidation.verifyUserInput(featureGroupDTO);
    verifyFeatureGroupVersion(featureGroupDTO.getVersion());
    statisticColumnController.verifyStatisticColumnsExist(featureGroupDTO);
  }

  /**
   * Verify user input feature group version
   *
   * @param version the version to verify
   * @throws FeaturestoreException
   */
  private void verifyFeatureGroupVersion(Integer version) throws FeaturestoreException {
    if (version == null) {
      throw new IllegalArgumentException(
        RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_VERSION_NOT_PROVIDED.getMessage());
    }
    if(version <= 0) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_VERSION, Level.FINE,
        "version cannot be negative or zero");
    }
  }

  void verifyFeaturesNoDefaultValue(List<FeatureGroupFeatureDTO> features)
      throws FeaturestoreException {
    if (features.stream().anyMatch(f -> f.getDefaultValue() != null)) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATURE_GROUP_FEATURE_DEFAULT_VALUE,
        Level.FINE, "default values for features cannot be set during feature group creation, only allowed for appened"
        + "features");
    }
  }
}
