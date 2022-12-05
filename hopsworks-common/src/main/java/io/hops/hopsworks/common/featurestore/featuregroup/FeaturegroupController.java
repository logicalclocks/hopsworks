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
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.common.featurestore.FeaturestoreFacade;
import io.hops.hopsworks.common.featurestore.activity.FeaturestoreActivityFacade;
import io.hops.hopsworks.common.featurestore.app.FsJobManagerController;
import io.hops.hopsworks.common.featurestore.datavalidationv2.reports.ValidationReportController;
import io.hops.hopsworks.common.featurestore.datavalidationv2.suites.ExpectationSuiteController;
import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.CachedFeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.CachedFeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.CachedFeaturegroupFacade;
import io.hops.hopsworks.common.featurestore.featuregroup.ondemand.OnDemandFeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.ondemand.OnDemandFeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.online.OnlineFeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.stream.StreamFeatureGroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.stream.StreamFeatureGroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.stream.StreamFeatureGroupFacade;
import io.hops.hopsworks.common.featurestore.statistics.StatisticsController;
import io.hops.hopsworks.common.featurestore.statistics.columns.StatisticColumnController;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorController;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.common.featurestore.utils.FeaturestoreInputValidation;
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
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.exceptions.JobException;
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
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.hive.HivePartitions;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.ondemand.OnDemandFeaturegroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.stream.StreamFeatureGroup;
import io.hops.hopsworks.persistence.entity.featurestore.statistics.StatisticColumn;
import io.hops.hopsworks.persistence.entity.featurestore.statistics.StatisticsConfig;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
  private StreamFeatureGroupController streamFeatureGroupController;
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
  private StreamFeatureGroupFacade streamFeatureGroupFacade;
  @EJB
  private HopsFSProvenanceController fsController;
  @EJB
  private StatisticsController statisticsController;
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
  private FeatureGroupInputValidation featureGroupInputValidation;
  @EJB
  private FeaturestoreUtils featurestoreUtils;
  @EJB
  private ExpectationSuiteController expectationSuiteController;
  @EJB
  private ValidationReportController validationReportController;
  @EJB
  private QuotasEnforcement quotasEnforcement;
  @Inject
  private FsJobManagerController fsJobManagerController;

  /**
   * Gets all featuregroups for a particular featurestore and project, using the userCerts to query Hive
   *
   * @param featurestore featurestore to query featuregroups for
   * @return list of XML/JSON DTOs of the featuregroups
   */
  public List<FeaturegroupDTO> getFeaturegroupsForFeaturestore(Featurestore featurestore, Project project, Users user)
          throws FeaturestoreException, ServiceException {
    List<Featuregroup> featuregroups = featuregroupFacade.findByFeaturestore(featurestore);
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
    KafkaException, SchemaException, ProjectException, UserException, HopsSecurityException, JobException {
    switch (featuregroup.getFeaturegroupType()) {
      case CACHED_FEATURE_GROUP:
      case STREAM_FEATURE_GROUP:
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
    KafkaException, SchemaException, ProjectException, UserException, HopsSecurityException, JobException {

    enforceFeaturegroupQuotas(featurestore, featuregroupDTO);
    featureGroupInputValidation.verifySchemaProvided(featuregroupDTO);
    
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
    expectationSuiteController.verifyExpectationSuite(featuregroupDTO.getExpectationSuite());
    return createFeaturegroupNoValidation(featurestore, featuregroupDTO, project, user);
  }

  public FeaturegroupDTO createFeaturegroupNoValidation(Featurestore featurestore, FeaturegroupDTO featuregroupDTO,
                                                      Project project, Users user)
    throws FeaturestoreException, SQLException, ProvenanceException, ServiceException, KafkaException,
    SchemaException, ProjectException, UserException, IOException, HopsSecurityException, JobException {

    //Persist specific feature group metadata (cached fg or on-demand fg)
    OnDemandFeaturegroup onDemandFeaturegroup = null;
    CachedFeaturegroup cachedFeaturegroup = null;
    StreamFeatureGroup streamFeatureGroup = null;
  
    List<FeatureGroupFeatureDTO> featuresNoHudi = null;
    
    if (featuregroupDTO instanceof CachedFeaturegroupDTO) {
      // make copy of schema without hudi columns
      featuresNoHudi = new ArrayList<>(featuregroupDTO.getFeatures());
      cachedFeaturegroup = cachedFeaturegroupController.createCachedFeaturegroup(featurestore,
          (CachedFeaturegroupDTO) featuregroupDTO, project, user);
    } else if (featuregroupDTO instanceof StreamFeatureGroupDTO){
      // make copy of schema without hudi columns
      featuresNoHudi = new ArrayList<>(featuregroupDTO.getFeatures());
      streamFeatureGroup = streamFeatureGroupController.createStreamFeatureGroup(featurestore,
        (StreamFeatureGroupDTO) featuregroupDTO, project, user);
    } else {
      onDemandFeaturegroup = onDemandFeaturegroupController.createOnDemandFeaturegroup(featurestore,
          (OnDemandFeaturegroupDTO) featuregroupDTO, project, user);
    }

    //Persist basic feature group metadata
    Featuregroup featuregroup = persistFeaturegroupMetadata(featurestore, project, user, featuregroupDTO,
      cachedFeaturegroup, streamFeatureGroup, onDemandFeaturegroup);
  
    // online feature group needs to be set up after persisting metadata in order to get feature group id
    if (featuregroupDTO instanceof CachedFeaturegroupDTO && settings.isOnlineFeaturestore()
      && featuregroup.getCachedFeaturegroup().isOnlineEnabled()){
      onlineFeaturegroupController.setupOnlineFeatureGroup(featurestore, featuregroup, featuresNoHudi, project, user);
    } else if (featuregroupDTO instanceof StreamFeatureGroupDTO) {
      if (settings.isOnlineFeaturestore() && featuregroup.getStreamFeatureGroup().isOnlineEnabled()) {
        // setup kafka topic and online feature group in rondb
        onlineFeaturegroupController.setupOnlineFeatureGroup(featurestore, featuregroup, featuresNoHudi, project, user);
      } else {
        // not online enabled so set up only kafka topic
        streamFeatureGroupController.setupOfflineStreamFeatureGroup(project, featuregroup, featuresNoHudi);
      }
    }

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
  public FeaturegroupDTO convertFeaturegrouptoDTO(Featuregroup featuregroup, Project project, Users user)
          throws FeaturestoreException, ServiceException {
    String featurestoreName = featurestoreFacade.getHiveDbName(featuregroup.getFeaturestore().getHiveDbId());
    switch (featuregroup.getFeaturegroupType()) {
      case CACHED_FEATURE_GROUP:
        CachedFeaturegroupDTO cachedFeaturegroupDTO =
          cachedFeaturegroupController.convertCachedFeaturegroupToDTO(featuregroup, project, user);
        cachedFeaturegroupDTO.setFeaturestoreName(featurestoreName);
        return cachedFeaturegroupDTO;
      case STREAM_FEATURE_GROUP:
        StreamFeatureGroupDTO streamFeatureGroupDTO =
          streamFeatureGroupController.convertStreamFeatureGroupToDTO(featuregroup, project, user);
        streamFeatureGroupDTO.setFeaturestoreName(featurestoreName);
        return streamFeatureGroupDTO;
      case ON_DEMAND_FEATURE_GROUP:
        FeaturestoreStorageConnectorDTO storageConnectorDTO =
            connectorController.convertToConnectorDTO(user, project,
                    featuregroup.getOnDemandFeaturegroup().getFeaturestoreConnector());

        OnDemandFeaturegroupDTO onDemandFeaturegroupDTO =
                new OnDemandFeaturegroupDTO(featurestoreName, featuregroup, storageConnectorDTO);

        try {
          String path = getFeatureGroupLocation(featuregroup);
          String location = featurestoreUtils.prependNameNode(path);
          onDemandFeaturegroupDTO.setLocation(location);
        } catch (ServiceDiscoveryException e) {
          throw new ServiceException(RESTCodes.ServiceErrorCode.SERVICE_NOT_FOUND, Level.SEVERE);
        }
        return onDemandFeaturegroupDTO;
      default:
        throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_TYPE.getMessage()
            + ", Recognized Feature group types are: " + FeaturegroupType.ON_DEMAND_FEATURE_GROUP + "," +
          FeaturegroupType.STREAM_FEATURE_GROUP + ",  and: " + FeaturegroupType.CACHED_FEATURE_GROUP +
          ". The provided feature group type was not recognized: " + featuregroup.getFeaturegroupType());
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
    // currently supports updating:
    // adding new features
    // feature group description
    // feature descriptions
    
    // Verify general entity related information
    featurestoreInputValidation.verifyDescription(featuregroupDTO);
    featureGroupInputValidation.verifyFeatureGroupFeatureList(featuregroupDTO.getFeatures());
    featureGroupInputValidation.verifyOnlineOfflineTypeMatch(featuregroupDTO);
    featureGroupInputValidation.verifyOnlineSchemaValid(featuregroupDTO);
    featureGroupInputValidation.verifyPrimaryKeySupported(featuregroupDTO);
    
    // Update on-demand feature group metadata
    if (featuregroup.getFeaturegroupType() == FeaturegroupType.CACHED_FEATURE_GROUP) {
      cachedFeaturegroupController
          .updateMetadata(project, user, featuregroup, (CachedFeaturegroupDTO) featuregroupDTO);
    } else if (featuregroup.getFeaturegroupType() == FeaturegroupType.STREAM_FEATURE_GROUP) {
      streamFeatureGroupController.updateMetadata(project, user, featuregroup,
        (StreamFeatureGroupDTO) featuregroupDTO);
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
    if (featureGroupDTO.getStatisticsConfig().getExactUniqueness() != null) {
      featuregroup.getStatisticsConfig().setExactUniqueness(featureGroupDTO.getStatisticsConfig().getExactUniqueness());
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
    throws SQLException, FeaturestoreException, ServiceException, IOException, SchemaException, KafkaException,
    JobException {
    // In some cases, fg metadata was not deleted. https://hopsworks.atlassian.net/browse/FSTORE-377
    // This enables users to delete a corrupted fg using the hsfs client.
    if (featuregroup.getOnDemandFeaturegroup() == null
        && featuregroup.getCachedFeaturegroup() == null
        && featuregroup.getStreamFeatureGroup() == null) {
      deleteFeatureGroupMeta(featuregroup);
      return;
    }
    switch (featuregroup.getFeaturegroupType()) {
      case CACHED_FEATURE_GROUP:
        //Delete hive_table will cascade to cached_featuregroup_table which will cascade to feature_group table
        cachedFeaturegroupController.dropHiveFeaturegroup(featuregroup, project, user);
        //Delete mysql table and metadata
        if(settings.isOnlineFeaturestore() && featuregroup.getCachedFeaturegroup().isOnlineEnabled()) {
          onlineFeaturegroupController.disableOnlineFeatureGroup(featuregroup, project, user);
        }
        break;
      case STREAM_FEATURE_GROUP:
        //Delete hive_table will cascade to stream_featuregroup_table which will cascade to feature_group table
        cachedFeaturegroupController.dropHiveFeaturegroup(featuregroup, project, user);
        //Delete mysql table and metadata
        if (settings.isOnlineFeaturestore() && featuregroup.getStreamFeatureGroup().isOnlineEnabled()) {
          onlineFeaturegroupController.disableOnlineFeatureGroup(featuregroup, project, user);
        } else {
          // only topics need to be deleted, but no RonDB table
          streamFeatureGroupController.deleteOfflineStreamFeatureGroupTopic(project, featuregroup);
        }
        //Delete associated delta streamer job
        fsJobManagerController.deleteDeltaStreamerJob(project,user, featuregroup);
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

    // Statistics adn validation files need to be deleted explicitly
    validationReportController.deleteFeaturegroupDataValidationDir(user, featuregroup);
    statisticsController.deleteStatistics(project, user, featuregroup);
    // In some cases, fg metadata was not deleted. https://hopsworks.atlassian.net/browse/FSTORE-377
    // Remove the metadata if it still exists.
    deleteFeatureGroupMeta(featuregroup);
  }

  private void deleteFeatureGroupMeta(Featuregroup featuregroup) {
    if (featuregroupFacade.findByIdAndFeaturestore(featuregroup.getId(),
        featuregroup.getFeaturestore()).isPresent()) {
      featuregroupFacade.remove(featuregroup);
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
      case STREAM_FEATURE_GROUP:
        return streamFeatureGroupFacade.getHiveTablePartitions(
          featuregroup.getStreamFeatureGroup().getHiveTbls(), offset, limit);
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
   * @param streamFeatureGroup the stream feature group that the feature group is linked to (if any)
   * @param onDemandFeaturegroup the on-demand feature group that the feature group is linked to (if any)
   * @return the created entity
   */
  private Featuregroup persistFeaturegroupMetadata(Featurestore featurestore, Project project,  Users user,
                                                   FeaturegroupDTO featuregroupDTO,
                                                   CachedFeaturegroup cachedFeaturegroup,
                                                   StreamFeatureGroup streamFeatureGroup,
                                                   OnDemandFeaturegroup onDemandFeaturegroup)
    throws FeaturestoreException, JobException {
    Featuregroup featuregroup = new Featuregroup();
    featuregroup.setName(featuregroupDTO.getName());
    featuregroup.setFeaturestore(featurestore);
    featuregroup.setCreated(new Date());
    featuregroup.setCreator(user);
    featuregroup.setVersion(featuregroupDTO.getVersion());
    
    if (featuregroupDTO instanceof CachedFeaturegroupDTO) {
      featuregroup.setFeaturegroupType(FeaturegroupType.CACHED_FEATURE_GROUP);
    } else if (featuregroupDTO instanceof  StreamFeatureGroupDTO) {
      featuregroup.setFeaturegroupType(FeaturegroupType.STREAM_FEATURE_GROUP);
      // if its stream feature group create delta streamer job
      StreamFeatureGroupDTO streamFeatureGroupDTO = (StreamFeatureGroupDTO) featuregroupDTO;
      fsJobManagerController.setupHudiDeltaStreamerJob(project, user, featuregroup,
        streamFeatureGroupDTO.getDeltaStreamerJobConf());
    } else {
      featuregroup.setFeaturegroupType(FeaturegroupType.ON_DEMAND_FEATURE_GROUP);
    }

    featuregroup.setCachedFeaturegroup(cachedFeaturegroup);
    featuregroup.setStreamFeatureGroup(streamFeatureGroup);
    featuregroup.setOnDemandFeaturegroup(onDemandFeaturegroup);
    featuregroup.setEventTime(featuregroupDTO.getEventTime());

    StatisticsConfig statisticsConfig = new StatisticsConfig(featuregroupDTO.getStatisticsConfig().getEnabled(),
      featuregroupDTO.getStatisticsConfig().getCorrelations(), featuregroupDTO.getStatisticsConfig().getHistograms(),
      featuregroupDTO.getStatisticsConfig().getExactUniqueness());
    statisticsConfig.setFeaturegroup(featuregroup);
    statisticsConfig.setStatisticColumns(featuregroupDTO.getStatisticsConfig().getColumns().stream()
      .map(sc -> new StatisticColumn(statisticsConfig, sc)).collect(Collectors.toList()));
    featuregroup.setStatisticsConfig(statisticsConfig);

    if (featuregroupDTO.getExpectationSuite() != null) {
      featuregroup.setExpectationSuite(expectationSuiteController.convertExpectationSuiteDTOToPersistent(
        featuregroup, featuregroupDTO.getExpectationSuite()));
    }
    
    featuregroupFacade.persist(featuregroup);
    return featuregroup;
  }

  public List<FeatureGroupFeatureDTO> getFeatures(Featuregroup featuregroup, Project project, Users user)
    throws FeaturestoreException {
    switch (featuregroup.getFeaturegroupType()) {
      case CACHED_FEATURE_GROUP:
        return cachedFeaturegroupController.getFeaturesDTO(featuregroup.getCachedFeaturegroup(), featuregroup.getId(),
          featuregroup.getFeaturestore(), project, user);
      case STREAM_FEATURE_GROUP:
        return cachedFeaturegroupController.getFeaturesDTO(featuregroup.getStreamFeatureGroup(), featuregroup.getId(),
          featuregroup.getFeaturestore(), project, user);
      case ON_DEMAND_FEATURE_GROUP:
        return featuregroup.getOnDemandFeaturegroup().getFeatures().stream()
          .map(f -> new FeatureGroupFeatureDTO(f.getName(), f.getType(), f.getPrimary(), null, featuregroup.getId()))
          .collect(Collectors.toList());
    }
    return new ArrayList<>();
  }

  /**
   * Gets the featuregroup table name
   *
   * @param featuregroupName name of the featuregroup
   * @param version          version of the featuregroup
   * @return                 the table name of the featuregroup (featuregroup_version)
   */
  public String getTblName(String featuregroupName, Integer version) {
    return featuregroupName + "_" + version.toString();
  }

  public String getFeatureGroupLocation(Featuregroup featureGroup) {
    // Cached feature groups also have a `location` field.
    // the issue is that the host is slightly different due to a configuration of Hive
    // so here we resolve only the path based on the indoe
    if (featureGroup.getFeaturegroupType() == FeaturegroupType.CACHED_FEATURE_GROUP) {
      return inodeController.getPath(featureGroup.getCachedFeaturegroup().getHiveTbls().getSdId().getInode());
    } else if (featureGroup.getFeaturegroupType() == FeaturegroupType.STREAM_FEATURE_GROUP) {
      return inodeController.getPath(featureGroup.getStreamFeatureGroup().getHiveTbls().getSdId().getInode());
    } else {
      return inodeController.getPath(featureGroup.getOnDemandFeaturegroup().getInode());
    }
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
    featureGroupInputValidation.verifyUserInput(featureGroupDTO);
    featureGroupInputValidation.verifyEventTimeFeature(featureGroupDTO.getEventTime(), featureGroupDTO.getFeatures());
    featureGroupInputValidation.verifyOnlineOfflineTypeMatch(featureGroupDTO);
    featureGroupInputValidation.verifyOnlineSchemaValid(featureGroupDTO);
    featureGroupInputValidation.verifyPrimaryKeySupported(featureGroupDTO);
    featureGroupInputValidation.verifyPartitionKeySupported(featureGroupDTO);
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

  private void enforceFeaturegroupQuotas(Featurestore featurestore, FeaturegroupDTO featuregroup)
          throws FeaturestoreException {
    try {
      boolean onlineEnabled = false;
      if (featuregroup instanceof CachedFeaturegroupDTO) {
        onlineEnabled = ((CachedFeaturegroupDTO) featuregroup).getOnlineEnabled();
      } else if (featuregroup instanceof StreamFeatureGroupDTO) {
        onlineEnabled = ((StreamFeatureGroupDTO) featuregroup).getOnlineEnabled();
      }
      quotasEnforcement.enforceFeaturegroupsQuota(featurestore, onlineEnabled);
    } catch (QuotaEnforcementException ex) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_CREATE_FEATUREGROUP, Level.SEVERE,
              ex.getMessage(), ex.getMessage(), ex);
    }
  }
}
