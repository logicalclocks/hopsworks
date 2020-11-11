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
import io.hops.hopsworks.common.dao.jobs.description.JobFacade;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.featurestore.FeaturestoreFacade;
import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.CachedFeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.CachedFeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.CachedFeaturegroupFacade;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.FeaturegroupPreview;
import io.hops.hopsworks.common.featurestore.featuregroup.ondemand.OnDemandFeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.ondemand.OnDemandFeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.jobs.FeaturestoreJobDTO;
import io.hops.hopsworks.common.featurestore.jobs.FeaturestoreJobFacade;
import io.hops.hopsworks.common.featurestore.statistics.StatisticsController;
import io.hops.hopsworks.common.featurestore.statistics.columns.StatisticColumnController;
import io.hops.hopsworks.common.featurestore.statistics.columns.StatisticColumnFacade;
import io.hops.hopsworks.common.featurestore.storageconnectors.jdbc.FeaturestoreJdbcConnectorDTO;
import io.hops.hopsworks.common.featurestore.utils.FeaturestoreInputValidation;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.provenance.core.HopsFSProvenanceController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.FeaturegroupType;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.CachedFeaturegroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.HivePartitions;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.ondemand.OnDemandFeaturegroup;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.activity.ActivityFlag;
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
  private FeaturestoreJobFacade featurestoreJobFacade;
  @EJB
  private JobFacade jobFacade;
  @EJB
  private StatisticColumnController statisticColumnController;
  @EJB
  private StatisticColumnFacade statisticColumnFacade;
  @EJB
  private FeaturestoreInputValidation featurestoreInputValidation;
  @EJB
  private CachedFeaturegroupFacade cachedFeaturegroupFacade;
  @EJB
  private HopsFSProvenanceController fsController;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private StatisticsController statisticsController;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private Settings settings;
  @EJB
  private HdfsUsersController hdfsUsersController;

  /**
   * Gets all featuregroups for a particular featurestore and project, using the userCerts to query Hive
   *
   * @param featurestore featurestore to query featuregroups for
   * @return list of XML/JSON DTOs of the featuregroups
   */
  public List<FeaturegroupDTO> getFeaturegroupsForFeaturestore(Featurestore featurestore, Project project, Users user)
    throws FeaturestoreException {
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
    throws FeaturestoreException, SQLException, ProvenanceException, IOException, ServiceException {
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
   * @throws IOException
   * @throws ServiceException
   */
  public FeaturegroupDTO createFeaturegroup(Featurestore featurestore, FeaturegroupDTO featuregroupDTO,
                                            Project project, Users user)
      throws FeaturestoreException, ServiceException, SQLException, ProvenanceException, IOException {

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
      throws FeaturestoreException, SQLException, ProvenanceException, IOException, ServiceException {

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
    
    //Store statistic columns setting
    statisticColumnController.persistStatisticColumns(featuregroup, featuregroupDTO.getStatisticColumns());
    featuregroup.setStatisticColumns(statisticColumnFacade.findByFeaturegroup(featuregroup));

    //Get jobs
    List<Jobs> jobs = getJobs(featuregroupDTO.getJobs(), featurestore.getProject());
    
    //Store jobs
    featurestoreJobFacade.insertJobs(featuregroup, jobs);
    
    FeaturegroupDTO completeFeaturegroupDTO = convertFeaturegrouptoDTO(featuregroup, project, user);

    //Extract metadata
    String hdfsUsername = hdfsUsersController.getHdfsUserName(project, user);
    DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUsername);
    try {
      String fgPath = Utils.getFeaturestorePath(featurestore.getProject(), settings)
          + "/" + Utils.getFeaturegroupName(featuregroup.getName(), featuregroup.getVersion());
      fsController.featuregroupAttachXAttrs(fgPath, completeFeaturegroupDTO, udfso);
    } finally {
      dfs.closeDfsClient(udfso);
    }

    return completeFeaturegroupDTO;
  }
  
  /**
   * Lookup jobs by list of jobNames
   *
   * @param jobDTOs the DTOs with the job names
   * @param project the project that owns the jobs
   * @return a list of job entities
   */
  private List<Jobs> getJobs(List<FeaturestoreJobDTO> jobDTOs, Project project) {
    if(jobDTOs != null) {
      return jobDTOs.stream().filter(jobDTO -> jobDTO != null && !Strings.isNullOrEmpty(jobDTO.getJobName()))
          .map(jobDTO -> jobDTO.getJobName()).distinct().map(jobName ->
          jobFacade.findByProjectAndName(project, jobName)).collect(Collectors.toList());
    } else {
      return new ArrayList<>();
    }
  }

  /**
   * Convert a featuregroup entity to a DTO representation
   *
   * @param featuregroup the entity to convert
   * @return a DTO representation of the entity
   */
  private FeaturegroupDTO convertFeaturegrouptoDTO(Featuregroup featuregroup, Project project, Users user)
    throws FeaturestoreException {
    String featurestoreName = featurestoreFacade.getHiveDbName(featuregroup.getFeaturestore().getHiveDbId());
    switch (featuregroup.getFeaturegroupType()) {
      case CACHED_FEATURE_GROUP:
        CachedFeaturegroupDTO cachedFeaturegroupDTO =
          cachedFeaturegroupController.convertCachedFeaturegroupToDTO(featuregroup, project, user);
        cachedFeaturegroupDTO.setFeaturestoreName(featurestoreName);
        return cachedFeaturegroupDTO;
      case ON_DEMAND_FEATURE_GROUP:
        FeaturestoreJdbcConnectorDTO storageConnectorDTO =
            new FeaturestoreJdbcConnectorDTO(featuregroup.getOnDemandFeaturegroup().getFeaturestoreJdbcConnector());
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
    Project project, Users user) throws FeaturestoreException {
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
      throws FeaturestoreException {
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
      throws FeaturestoreException{
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
      throws FeaturestoreException, SQLException, ProvenanceException {
    Featuregroup featuregroup = getFeaturegroupById(featurestore, featuregroupDTO.getId());
    // Verify general entity related information
    featurestoreInputValidation.verifyDescription(featuregroupDTO);
    featurestoreInputValidation.verifyFeatureGroupFeatureList(featuregroupDTO.getFeatures());

    // Update on-demand feature group metadata
    if (featuregroup.getFeaturegroupType() == FeaturegroupType.CACHED_FEATURE_GROUP) {
      cachedFeaturegroupController.updateMetadata(project, user, featuregroup, (CachedFeaturegroupDTO) featuregroupDTO);
    } else if (featuregroup.getFeaturegroupType() == FeaturegroupType.ON_DEMAND_FEATURE_GROUP) {
      onDemandFeaturegroupController.updateOnDemandFeaturegroupMetadata(featuregroup.getOnDemandFeaturegroup(),
        (OnDemandFeaturegroupDTO) featuregroupDTO);
    }

    // also in this case we should update the jobs. just in case there is a client relying on the fact that the
    // updateMetadata queryparam is also updating the jobs
    //Get jobs
    List<Jobs> jobs = getJobs(featuregroupDTO.getJobs(), featurestore.getProject());
    //Store jobs
    featurestoreJobFacade.insertJobs(featuregroup, jobs);

    // get feature group object again after alter table
    featuregroup = getFeaturegroupById(featurestore, featuregroupDTO.getId());
    featuregroupDTO = convertFeaturegrouptoDTO(featuregroup, project, user);

    String hdfsUsername = hdfsUsersController.getHdfsUserName(project, user);
    DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUsername);
    try {
      String fgPath = Utils.getFeaturestorePath(featurestore.getProject(), settings)
          + "/" + Utils.getFeaturegroupName(featuregroupDTO.getName(), featuregroupDTO.getVersion());
      fsController.featuregroupAttachXAttrs(fgPath, featuregroupDTO, udfso);
    } finally {
      dfs.closeDfsClient(udfso);
    }

    return featuregroupDTO;
  }

  /**
   * Updates jobs for a featuregroup
   *
   * @param featurestore    the featurestore where the featuregroup resides
   * @param featuregroupDTO the updated featuregroup metadata
   */
  public FeaturegroupDTO updateFeaturegroupJob(Featurestore featurestore, FeaturegroupDTO featuregroupDTO,
    Project project, Users user)
      throws FeaturestoreException {
    Featuregroup featuregroup = getFeaturegroupById(featurestore, featuregroupDTO.getId());
    //Get jobs
    List<Jobs> jobs = getJobs(featuregroupDTO.getJobs(), featurestore.getProject());
    //Store jobs
    featurestoreJobFacade.insertJobs(featuregroup, jobs);

    return convertFeaturegrouptoDTO(featuregroup, project, user);
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
      throws FeaturestoreException, SQLException {
    Featuregroup featuregroup = getFeaturegroupById(featurestore, featuregroupDTO.getId());
    if(featuregroup.getFeaturegroupType() == FeaturegroupType.ON_DEMAND_FEATURE_GROUP){
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.ONLINE_FEATURE_SERVING_NOT_SUPPORTED_FOR_ON_DEMAND_FEATUREGROUPS, Level.FINE,
        ", Online feature serving is only supported for featuregroups of type: "
          + FeaturegroupType.CACHED_FEATURE_GROUP + ", and the user requested to enable feature serving on a " +
          "featuregroup with type:" + FeaturegroupType.ON_DEMAND_FEATURE_GROUP);
    }
    cachedFeaturegroupController.enableFeaturegroupOnline(featurestore, featuregroup, project, user);
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
      throws FeaturestoreException, SQLException {
    if(featuregroup.getFeaturegroupType() == FeaturegroupType.ON_DEMAND_FEATURE_GROUP) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.ONLINE_FEATURE_SERVING_NOT_SUPPORTED_FOR_ON_DEMAND_FEATUREGROUPS, Level.FINE,
        ", Online feature serving is only supported for featuregroups of type: "
          + FeaturegroupType.CACHED_FEATURE_GROUP + ", and the user requested to a feature serving operation on a " +
          "featuregroup with type:" + FeaturegroupType.ON_DEMAND_FEATURE_GROUP);
    }
    cachedFeaturegroupController.disableFeaturegroupOnline(featuregroup, project, user);
    return convertFeaturegrouptoDTO(featuregroup, project, user);
  }

  /**
   * Updates statistics settings for a featuregroup
   *
   * @param featurestore    the featurestore where the featuregroup resides
   * @param featuregroupDTO a DTO containing the updated featuregroup stats
   * @return DTO of the updated feature group
   * @throws FeaturestoreException
   */
  public FeaturegroupDTO updateFeaturegroupStatsSettings(Featurestore featurestore, FeaturegroupDTO featuregroupDTO,
    Project project, Users user) throws FeaturestoreException {
    Featuregroup featuregroup = getFeaturegroupById(featurestore, featuregroupDTO.getId());
    if (featuregroupDTO.isDescStatsEnabled() != null) {
      // if setting is changed, check with new setting
      verifyFeaturegroupStatsSettings(featuregroupDTO.isDescStatsEnabled(), featuregroupDTO.isFeatCorrEnabled(),
        featuregroupDTO.isFeatHistEnabled());
      featuregroup.setDescStatsEnabled(featuregroupDTO.isDescStatsEnabled());
    } else {
      // check with old setting, assuming previous state was valid
      verifyFeaturegroupStatsSettings(featuregroup.isDescStatsEnabled(), featuregroupDTO.isFeatCorrEnabled(),
        featuregroupDTO.isFeatHistEnabled());
    }
    if (featuregroupDTO.isFeatHistEnabled() != null) {
      featuregroup.setFeatHistEnabled(featuregroupDTO.isFeatHistEnabled());
    }
    if (featuregroupDTO.isFeatCorrEnabled() != null) {
      featuregroup.setFeatCorrEnabled(featuregroupDTO.isFeatCorrEnabled());
    }
    // compare against schema from database, as client doesn't need to send schema in update request
    statisticColumnController.verifyStatisticColumnsExist(
      featuregroupDTO, convertFeaturegrouptoDTO(featuregroup, project, user).getFeatures());
    featuregroupFacade.updateFeaturegroupMetadata(featuregroup);
    statisticColumnController.persistStatisticColumns(featuregroup, featuregroupDTO.getStatisticColumns());
    // get feature group again with persisted columns - this trip to the database can be saved
    featuregroup = getFeaturegroupById(featurestore, featuregroupDTO.getId());
    return convertFeaturegrouptoDTO(featuregroup, project, user);
  }
  
  /**
   * Verifies if statistics settings were provided and else sets it to the default or keeps the current settings
   *
   * @param generalStatistics
   * @param correlations
   * @param histograms
   */
  public void verifyFeaturegroupStatsSettings(Boolean generalStatistics, Boolean correlations, Boolean histograms)
    throws FeaturestoreException {
    if (generalStatistics != null && !generalStatistics) {
      // if general statistics is null we assume it defaults to true for new feature groups
      if ((correlations != null && correlations) || (histograms != null && histograms)) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STATISTICS_CONFIG, Level.FINE,
          "correlations and histograms can only be enabled with statistics generally enabled.");
      }
    }
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
    throws SQLException, FeaturestoreException, ServiceException, IOException {
    switch (featuregroup.getFeaturegroupType()) {
      case CACHED_FEATURE_GROUP:
        // Statistics files need to be deleted explicitly
        statisticsController.deleteStatistics(project, user, featuregroup);
        //Delete hive_table will cascade to cached_featuregroup_table which will cascade to feature_group table
        cachedFeaturegroupController.dropHiveFeaturegroup(featuregroup, project, user);
        //Delete mysql table and metadata
        cachedFeaturegroupController.dropMySQLFeaturegroup(featuregroup, project, user);
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

    activityFacade.persistActivity(ActivityFacade.DELETED_FEATUREGROUP + featuregroup.getName(),
        project, user, ActivityFlag.SERVICE);
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
   * Synchronizes an already created Hive table with the Feature Store metadata
   *
   * @param featurestore the featurestore of the feature group
   * @param featuregroupDTO the feature group DTO
   * @param user the Hopsworks user making the request
   * @return a DTO of the created feature group
   * @throws FeaturestoreException
   */
  public FeaturegroupDTO syncHiveTableWithFeaturestore(Featurestore featurestore, FeaturegroupDTO featuregroupDTO,
    Users user) throws FeaturestoreException {

    if (featuregroupDTO instanceof OnDemandFeaturegroupDTO) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_TYPE.getMessage()
          + ", Only cached feature groups can be synced from an existing Hive table, not on-demand feature groups.");
    }
    verifyFeatureGroupInput(featuregroupDTO);

    CachedFeaturegroup cachedFeaturegroup = cachedFeaturegroupController
            .syncHiveTableWithFeaturestore(featurestore, (CachedFeaturegroupDTO) featuregroupDTO);

    //Persist basic feature group metadata
    Featuregroup featuregroup = persistFeaturegroupMetadata(featurestore, user, featuregroupDTO,
      cachedFeaturegroup, null);
  
    //Get jobs
    List<Jobs> jobs = getJobs(featuregroupDTO.getJobs(), featurestore.getProject());
  
    //Store jobs
    featurestoreJobFacade.insertJobs(featuregroup, jobs);
    
    return featuregroupDTO;
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
                                                   OnDemandFeaturegroup onDemandFeaturegroup) {
    Featuregroup featuregroup = new Featuregroup();
    featuregroup.setName(featuregroupDTO.getName());
    featuregroup.setFeaturestore(featurestore);
    featuregroup.setCreated(new Date());
    featuregroup.setCreator(user);
    featuregroup.setVersion(featuregroupDTO.getVersion());

    featuregroup.setFeaturegroupType(
      featuregroupDTO instanceof CachedFeaturegroupDTO ?
        FeaturegroupType.CACHED_FEATURE_GROUP :
        FeaturegroupType.ON_DEMAND_FEATURE_GROUP);

    featuregroup.setCachedFeaturegroup(cachedFeaturegroup);
    featuregroup.setOnDemandFeaturegroup(onDemandFeaturegroup);
    if (featuregroupDTO.isDescStatsEnabled() != null) {
      featuregroup.setDescStatsEnabled(featuregroupDTO.isDescStatsEnabled());
      featuregroup.setFeatHistEnabled(
        featuregroupDTO.isFeatHistEnabled() != null && featuregroupDTO.isFeatHistEnabled() &&
          featuregroupDTO.isDescStatsEnabled());
      featuregroup.setFeatCorrEnabled(
        featuregroupDTO.isFeatCorrEnabled() != null && featuregroupDTO.isFeatCorrEnabled() &&
          featuregroupDTO.isDescStatsEnabled());
    }

    featuregroupFacade.persist(featuregroup);
    return featuregroup;
  }

  public List<FeatureGroupFeatureDTO> getFeatures(Featuregroup featuregroup, Project project, Users user)
    throws FeaturestoreException {
    switch (featuregroup.getFeaturegroupType()) {
      case CACHED_FEATURE_GROUP:
        return cachedFeaturegroupController.getFeaturesDTO(
          featuregroup.getCachedFeaturegroup().getHiveTbls(), featuregroup.getFeaturestore(), project, user);
      case ON_DEMAND_FEATURE_GROUP:
        return featuregroup.getOnDemandFeaturegroup().getFeatures().stream()
          .map(f -> new FeatureGroupFeatureDTO(f.getName(), f.getType(), f.getPrimary(), null))
          .collect(Collectors.toList());
    }
    return new ArrayList<>();
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
    verifyFeaturegroupStatsSettings(featureGroupDTO.isDescStatsEnabled(), featureGroupDTO.isFeatCorrEnabled(),
      featureGroupDTO.isFeatHistEnabled());
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
