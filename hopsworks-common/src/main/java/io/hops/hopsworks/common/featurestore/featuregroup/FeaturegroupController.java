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

import io.hops.hopsworks.common.dao.featurestore.Featurestore;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.FeaturegroupType;
import io.hops.hopsworks.common.featurestore.FeaturestoreFacade;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.cached.CachedFeaturegroup;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.CachedFeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.CachedFeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.FeaturegroupPreview;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.RowValueQueryResult;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.ondemand.OnDemandFeaturegroup;
import io.hops.hopsworks.common.featurestore.featuregroup.ondemand.OnDemandFeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.ondemand.OnDemandFeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.jobs.FeaturestoreJobController;
import io.hops.hopsworks.common.featurestore.jobs.FeaturestoreJobDTO;
import io.hops.hopsworks.common.featurestore.statistics.columns.StatisticColumnController;
import io.hops.hopsworks.common.featurestore.statistics.columns.StatisticColumnFacade;
import io.hops.hopsworks.common.featurestore.statistics.FeaturestoreStatisticController;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.jobs.description.JobFacade;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.featurestore.FeaturestoreConstants;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.parquet.Strings;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Class controlling the interaction with the feature_group table and required business logic
 */
@Stateless
public class FeaturegroupController {
  @EJB
  private FeaturegroupFacade featuregroupFacade;
  @EJB
  private HdfsUsersFacade hdfsUsersFacade;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private FeaturestoreStatisticController featurestoreStatisticController;
  @EJB
  private CachedFeaturegroupController cachedFeaturegroupController;
  @EJB
  private OnDemandFeaturegroupController onDemandFeaturegroupController;
  @EJB
  private FeaturestoreFacade featurestoreFacade;
  @EJB
  private FeaturestoreJobController featurestoreJobController;
  @EJB
  private JobFacade jobFacade;
  @EJB
  private StatisticColumnController statisticColumnController;
  @EJB
  private StatisticColumnFacade statisticColumnFacade;

  /**
   * Gets all featuregroups for a particular featurestore and project, using the userCerts to query Hive
   *
   * @param featurestore featurestore to query featuregroups for
   * @return list of XML/JSON DTOs of the featuregroups
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public List<FeaturegroupDTO> getFeaturegroupsForFeaturestore(Featurestore featurestore) {
    List<Featuregroup> featuregroups = featuregroupFacade.findByFeaturestore(featurestore);
    return featuregroups.stream().map(fg -> convertFeaturegrouptoDTO(fg)).collect(Collectors.toList());
  }

  /**
   * Clears the contents of a feature group (obviously only works for cached feature groups)
   *
   * @param featurestore    the featurestore of the feature group
   * @param featuregroupDTO data about the featuregroup to clear
   * @param user            the user making the request
   * @return a DTO representation of the cleared feature group
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   * @throws SQLException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public FeaturegroupDTO clearFeaturegroup(Featurestore featurestore, FeaturegroupDTO featuregroupDTO, Users user)
    throws FeaturestoreException, HopsSecurityException, SQLException, ProvenanceException {
    switch (featuregroupDTO.getFeaturegroupType()) {
      case CACHED_FEATURE_GROUP:
        deleteFeaturegroupIfExists(featurestore, featuregroupDTO, user);
        return createFeaturegroup(featurestore, featuregroupDTO, user);
      case ON_DEMAND_FEATURE_GROUP:
        throw new FeaturestoreException(
            RESTCodes.FeaturestoreErrorCode.CLEAR_OPERATION_NOT_SUPPORTED_FOR_ON_DEMAND_FEATUREGROUPS,
            Level.FINE, "featuregroupId: " + featuregroupDTO.getId());
      default:
        throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_TYPE.getMessage()
            + ", Recognized Feature group types are: " + FeaturegroupType.ON_DEMAND_FEATURE_GROUP + ", and: " +
            FeaturegroupType.CACHED_FEATURE_GROUP + ". The provided feature group type was not recognized: "
            + featuregroupDTO.getFeaturegroupType());
    }
  }

  /**
   * Creates a new feature group in a featurestore
   *
   * @param featurestore    the featurestore where the new feature group will be created
   * @param featuregroupDTO input data about the feature group to create
   * @param user            the user making the request
   * @return a DTO representation of the created feature group
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   * @throws SQLException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public FeaturegroupDTO createFeaturegroup(Featurestore featurestore, FeaturegroupDTO featuregroupDTO, Users user)
    throws FeaturestoreException, HopsSecurityException, SQLException, ProvenanceException {
    
    //Verify basic feature group input information
    verifyFeaturegroupUserInput(featuregroupDTO, featurestore);
    
    //Verify statistics input (more detailed input verification is delegated to lower level controllers)
    verifyStatisticsInput(featuregroupDTO);
    
    //Extract metadata
    String hdfsUsername = hdfsUsersController.getHdfsUserName(featurestore.getProject(), user);
    HdfsUsers hdfsUser = hdfsUsersFacade.findByName(hdfsUsername);
  
    //Persist specific feature group metadata (cached fg or on-demand fg)
    OnDemandFeaturegroup onDemandFeaturegroup = null;
    CachedFeaturegroup cachedFeaturegroup = null;
    switch (featuregroupDTO.getFeaturegroupType()) {
      case CACHED_FEATURE_GROUP:
        cachedFeaturegroup = cachedFeaturegroupController.createCachedFeaturegroup(featurestore,
          (CachedFeaturegroupDTO) featuregroupDTO, user);
        break;
      case ON_DEMAND_FEATURE_GROUP:
        onDemandFeaturegroup =
          onDemandFeaturegroupController.createOnDemandFeaturegroup((OnDemandFeaturegroupDTO) featuregroupDTO);
        break;
      default:
        throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_TYPE.getMessage()
          + ", Recognized Feature group types are: " + FeaturegroupType.ON_DEMAND_FEATURE_GROUP + ", and: " +
          FeaturegroupType.CACHED_FEATURE_GROUP + ". The provided feature group type was not recognized: "
          + featuregroupDTO.getFeaturegroupType());
    }
    
    //Persist basic feature group metadata
    Featuregroup featuregroup = persistFeaturegroupMetadata(featurestore, hdfsUser, user, featuregroupDTO,
      cachedFeaturegroup, onDemandFeaturegroup);
    
    //Store statistic columns setting
    statisticColumnController.persistStatisticColumns(featuregroup, featuregroupDTO.getStatisticColumns());
    featuregroup.setStatisticColumns(statisticColumnFacade.findByFeaturegroup(featuregroup));

    // Store statistics
    featurestoreStatisticController.updateFeaturestoreStatistics(featuregroup, null,
        featuregroupDTO.getFeatureCorrelationMatrix(), featuregroupDTO.getDescriptiveStatistics(),
        featuregroupDTO.getFeaturesHistogram(), featuregroupDTO.getClusterAnalysis());
  
    //Get jobs
    List<Jobs> jobs = getJobs(featuregroupDTO.getJobs(), featurestore.getProject());
    
    //Store jobs
    featurestoreJobController.insertJobs(featuregroup, jobs);
    
    return convertFeaturegrouptoDTO(featuregroup);
  }
  
  /**
   * Lookup jobs by list of jobNames
   *
   * @param jobDTOs the DTOs with the job names
   * @param project the project that owns the jobs
   * @return a list of job entities
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
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
  @TransactionAttribute(TransactionAttributeType.NEVER)
  private FeaturegroupDTO convertFeaturegrouptoDTO(Featuregroup featuregroup) {
    String featurestoreName = featurestoreFacade.getHiveDbName(featuregroup.getFeaturestore().getHiveDbId());
    switch (featuregroup.getFeaturegroupType()) {
      case CACHED_FEATURE_GROUP:
        CachedFeaturegroupDTO cachedFeaturegroupDTO =
          cachedFeaturegroupController.convertCachedFeaturegroupToDTO(featuregroup);
        cachedFeaturegroupDTO.setFeaturestoreName(featurestoreName);
        return cachedFeaturegroupDTO;
      case ON_DEMAND_FEATURE_GROUP:
        OnDemandFeaturegroupDTO onDemandFeaturegroupDTO = new
          OnDemandFeaturegroupDTO(featuregroup);
        onDemandFeaturegroupDTO.setFeaturestoreName(featurestoreName);
        return onDemandFeaturegroupDTO;
      default:
        throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_TYPE.getMessage()
            + ", Recognized Feature group types are: " + FeaturegroupType.ON_DEMAND_FEATURE_GROUP + ", and: " +
            FeaturegroupType.CACHED_FEATURE_GROUP + ". The provided feature group type was not recognized: "
            + featuregroup.getFeaturegroupType());
    }
  }

  /**
   * Retrieves a featuregroup with a particular id from a particular featurestore
   *
   * @param id           id of the featuregroup
   * @param featurestore the featurestore that the featuregroup belongs to
   * @return XML/JSON DTO of the featuregroup
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public FeaturegroupDTO getFeaturegroupWithIdAndFeaturestore(Featurestore featurestore, Integer id) {
    Featuregroup featuregroup = verifyFeaturegroupId(id, featurestore);
    return convertFeaturegrouptoDTO(featuregroup);
  }

  /**
   * Updates metadata for a featuregroup
   *
   * @param featurestore    the featurestore where the featuregroup resides
   * @param featuregroupDTO the updated featuregroup metadata
   * @return DTO of the updated feature group
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public FeaturegroupDTO updateFeaturegroupMetadata(
      Featurestore featurestore, FeaturegroupDTO featuregroupDTO) throws FeaturestoreException {
    Featuregroup featuregroup = verifyFeaturegroupId(featuregroupDTO.getId(), featurestore);
    //Get jobs
    List<Jobs> jobs = getJobs(featuregroupDTO.getJobs(), featurestore.getProject());
    //Store jobs
    featurestoreJobController.insertJobs(featuregroup, jobs);
    //Update on-demand feature group metadata
    if (featuregroup.getFeaturegroupType() == FeaturegroupType.ON_DEMAND_FEATURE_GROUP) {
      onDemandFeaturegroupController.updateOnDemandFeaturegroupMetadata(featuregroup.getOnDemandFeaturegroup(),
        (OnDemandFeaturegroupDTO) featuregroupDTO);
    }
    return convertFeaturegrouptoDTO(featuregroup);
  }
  
  /**
   * Enable online feature serving of a feature group that is currently only offline
   *
   * @param featurestore    the featurestore where the featuregroup resides
   * @param featuregroupDTO the updated featuregroup metadata
   * @return DTO of the updated feature group
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public FeaturegroupDTO enableFeaturegroupOnline(Featurestore featurestore, FeaturegroupDTO featuregroupDTO,
    Users user) throws FeaturestoreException, SQLException {
    Featuregroup featuregroup = verifyFeaturegroupId(featuregroupDTO.getId(), featurestore);
    if(featuregroup.getFeaturegroupType() == FeaturegroupType.ON_DEMAND_FEATURE_GROUP){
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.ONLINE_FEATURE_SERVING_NOT_SUPPORTED_FOR_ON_DEMAND_FEATUREGROUPS, Level.FINE,
        ", Online feature serving is only supported for featuregroups of type: "
          + FeaturegroupType.CACHED_FEATURE_GROUP + ", and the user requested to enable feature serving on a " +
          "featuregroup with type:" + FeaturegroupType.ON_DEMAND_FEATURE_GROUP);
    }
    cachedFeaturegroupController.enableFeaturegroupOnline(featurestore, ((CachedFeaturegroupDTO) featuregroupDTO),
      featuregroup, user);
    return convertFeaturegrouptoDTO(featuregroup);
  }
  
  /**
   * Disable online feature serving of a feature group
   *
   * @param featurestore    the featurestore where the featuregroup resides
   * @param featuregroupDTO the updated featuregroup metadata
   * @return DTO of the updated feature group
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public FeaturegroupDTO disableFeaturegroupOnline(Featurestore featurestore, FeaturegroupDTO featuregroupDTO,
    Users user) throws FeaturestoreException, SQLException {
    Featuregroup featuregroup = verifyFeaturegroupId(featuregroupDTO.getId(), featurestore);
    if(featuregroup.getFeaturegroupType() == FeaturegroupType.ON_DEMAND_FEATURE_GROUP) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.ONLINE_FEATURE_SERVING_NOT_SUPPORTED_FOR_ON_DEMAND_FEATUREGROUPS, Level.FINE,
        ", Online feature serving is only supported for featuregroups of type: "
          + FeaturegroupType.CACHED_FEATURE_GROUP + ", and the user requested to a feature serving operation on a " +
          "featuregroup with type:" + FeaturegroupType.ON_DEMAND_FEATURE_GROUP);
    }
    cachedFeaturegroupController.disableFeaturegroupOnline(featurestore, featuregroup, user);
    return convertFeaturegrouptoDTO(featuregroup);
  }

  /**
   * Updates stats for a featuregroup
   *
   * @param featurestore    the featurestore where the featuregroup resides
   * @param featuregroupDTO a DTO containing the updated featuregroup stats
   * @return DTO of the updated feature group
   * @throws FeaturestoreException
   */

  @TransactionAttribute(TransactionAttributeType.NEVER)
  public FeaturegroupDTO updateFeaturegroupStats(
      Featurestore featurestore, FeaturegroupDTO featuregroupDTO) {
    Featuregroup featuregroup = verifyFeaturegroupId(featuregroupDTO.getId(), featurestore);
    verifyStatisticsInput(featuregroupDTO);
    featurestoreStatisticController.updateFeaturestoreStatistics(featuregroup, null,
      featuregroupDTO.getFeatureCorrelationMatrix(), featuregroupDTO.getDescriptiveStatistics(),
      featuregroupDTO.getFeaturesHistogram(), featuregroupDTO.getClusterAnalysis());
    return convertFeaturegrouptoDTO(featuregroup);
  }
  
  /**
   * Updates statistics settings for a featuregroup
   *
   * @param featurestore    the featurestore where the featuregroup resides
   * @param featuregroupDTO a DTO containing the updated featuregroup stats
   * @return DTO of the updated feature group
   * @throws FeaturestoreException
   */
  
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public FeaturegroupDTO updateFeaturegroupStatsSettings(
    Featurestore featurestore, FeaturegroupDTO featuregroupDTO) {
    Featuregroup featuregroup = verifyFeaturegroupId(featuregroupDTO.getId(), featurestore);
    statisticColumnController.persistStatisticColumns(featuregroup, featuregroupDTO.getStatisticColumns());
    featuregroup = verifyFeaturegroupId(featuregroupDTO.getId(), featurestore);
    // update the settings and persist, if setting not define keep previous value
    verifyAndSetFeaturegroupStatsSettings(featuregroupDTO, featuregroup);
    featuregroupFacade.updateFeaturegroupMetadata(featuregroup);
    return convertFeaturegrouptoDTO(featuregroup);
  }
  
  /**
   * Verifies if statistics settings were provided and else sets it to the default or keeps the current settings
   *
   * @param featuregroupDTO
   * @param featuregroup
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void verifyAndSetFeaturegroupStatsSettings(FeaturegroupDTO featuregroupDTO, Featuregroup featuregroup) {
    if(featuregroupDTO.isDescStatsEnabled() != null) {
      featuregroup.setDescStatsEnabled(featuregroupDTO.isDescStatsEnabled());
    }
    if(featuregroupDTO.isFeatCorrEnabled() != null) {
      featuregroup.setFeatCorrEnabled(featuregroupDTO.isFeatCorrEnabled());
    }
    if(featuregroupDTO.isFeatHistEnabled() != null) {
      featuregroup.setFeatHistEnabled(featuregroupDTO.isFeatHistEnabled());
    }
    if(featuregroupDTO.isClusterAnalysisEnabled() != null) {
      featuregroup.setClusterAnalysisEnabled(featuregroupDTO.isClusterAnalysisEnabled());
    }
    if(featuregroupDTO.getNumBins() != null) {
      featuregroup.setNumBins(featuregroupDTO.getNumBins());
    }
    if(featuregroupDTO.getNumClusters() != null) {
      featuregroup.setNumClusters(featuregroupDTO.getNumClusters());
    }
    if(featuregroupDTO.getCorrMethod() != null) {
      featuregroup.setCorrMethod(featuregroupDTO.getCorrMethod());
    }
  }
  
  /**
   * Verifies statistics user input for a feature group
   *
   * @param featuregroupDTO DTO containing the feature group statistics
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void verifyStatisticsInput(FeaturegroupDTO featuregroupDTO) {
    if (featuregroupDTO.getFeatureCorrelationMatrix() != null &&
        featuregroupDTO.getFeatureCorrelationMatrix().getFeatureCorrelations().size() >
          FeaturestoreConstants.FEATURESTORE_STATISTICS_MAX_CORRELATIONS) {
      throw new IllegalArgumentException(
          RESTCodes.FeaturestoreErrorCode.CORRELATION_MATRIX_EXCEED_MAX_SIZE.getMessage());
    }
  }

  /**
   * Check if the feature group described by the DTO exists
   *
   * @param featurestore    the featurestore that the featuregroup belongs to
   * @param featuregroupDTO DTO representation of the feature group
   * @return
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public boolean featuregroupExists(Featurestore featurestore, FeaturegroupDTO featuregroupDTO) {
    return getFeaturegroupByDTO(featurestore, featuregroupDTO).isPresent();
  }

  /**
   * Get the feature group represented by the DTO
   *
   * @param featurestore    the featurestore that the featuregroup belongs to
   * @param featuregroupDTO DTO representation of the feature group
   * @return Optional containing the feature group if found
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public Optional<Featuregroup> getFeaturegroupByDTO(Featurestore featurestore, FeaturegroupDTO featuregroupDTO) {
    if (featuregroupDTO.getId() != null) {
      return Optional.of(verifyFeaturegroupId(featuregroupDTO.getId(), featurestore));
    }

    List<Featuregroup> featuregroups = featuregroupFacade.findByFeaturestore(featurestore);
    return featuregroups.stream().filter(fg -> {
      FeaturegroupDTO convertedFeaturegroupDTO = convertFeaturegrouptoDTO(fg);
      return convertedFeaturegroupDTO.getName().equalsIgnoreCase(featuregroupDTO.getName()) &&
        convertedFeaturegroupDTO.getVersion().equals(featuregroupDTO.getVersion());
    }).findFirst();
  }

  /**
   * Deletes a featuregroup with a particular id or name from a featurestore
   *
   * @param featurestore    the featurestore that the featuregroup belongs to
   * @param featuregroupDTO DTO representation of the feature group to delete
   * @param user            the user making the request
   * @return JSON/XML DTO of the deleted featuregroup
   * @throws SQLException
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public Optional<FeaturegroupDTO> deleteFeaturegroupIfExists(
      Featurestore featurestore, FeaturegroupDTO featuregroupDTO, Users user)
          throws SQLException, FeaturestoreException, HopsSecurityException {
    Optional<Featuregroup> featuregroup = getFeaturegroupByDTO(featurestore, featuregroupDTO);
    if (featuregroup.isPresent()) {
      return Optional.of(deleteFeaturegroup(featurestore, featuregroup.get(), user));
    }
    return Optional.empty();
  }

  /**
   * Deletes a featuregroup with a particular id or name from a featurestore
   *
   * @param featurestore    the featurestore that the featuregroup belongs to
   * @param user            the user making the request
   * @return JSON/XML DTO of the deleted featuregroup
   * @throws SQLException
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public FeaturegroupDTO deleteFeaturegroup(Featurestore featurestore, Featuregroup featuregroup, Users user)
      throws SQLException, FeaturestoreException, HopsSecurityException {
    FeaturegroupDTO convertedFeaturegroupDTO = convertFeaturegrouptoDTO(featuregroup);
    switch (featuregroup.getFeaturegroupType()) {
      case CACHED_FEATURE_GROUP:
        //Delete hive_table will cascade to cached_featuregroup_table which will cascade to feature_group table
        cachedFeaturegroupController.dropHiveFeaturegroup(convertedFeaturegroupDTO, featurestore, user);
        //Delete mysql table and metadata
        cachedFeaturegroupController.dropMySQLFeaturegroup(featuregroup.getCachedFeaturegroup(), featurestore, user);
        break;
      case ON_DEMAND_FEATURE_GROUP:
        //Delete on_demand_feature_group will cascade will cascade to feature_group table
        onDemandFeaturegroupController.removeOnDemandFeaturegroup(featuregroup.getOnDemandFeaturegroup());
        break;
      default:
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_TYPE, Level.FINE,
          ", Recognized Feature group types are: " + FeaturegroupType.ON_DEMAND_FEATURE_GROUP + ", and: " +
            FeaturegroupType.CACHED_FEATURE_GROUP + ". The provided feature group type was not recognized: "
            + featuregroup.getFeaturegroupType());
    }
    return convertedFeaturegroupDTO;
  }

  /**
   * Previews a given featuregroup by doing a SELECT LIMIT query on the Hive and MySQL Tables
   *
   * @param featuregroupDTO DTO of the featuregroup to preview
   * @param featurestore    the feature store where the feature group resides
   * @param user            the user making the request
   * @return A DTO object with the first 20 rows of the offline and online feature tables
   * @throws SQLException
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public FeaturegroupPreview getFeaturegroupPreview(
      FeaturegroupDTO featuregroupDTO, Featurestore featurestore, Users user) throws SQLException,
      FeaturestoreException, HopsSecurityException {
    switch (featuregroupDTO.getFeaturegroupType()) {
      case CACHED_FEATURE_GROUP:
        return cachedFeaturegroupController.getFeaturegroupPreview(featuregroupDTO, featurestore, user);
      case ON_DEMAND_FEATURE_GROUP:
        throw new FeaturestoreException(
            RESTCodes.FeaturestoreErrorCode.PREVIEW_NOT_SUPPORTED_FOR_ON_DEMAND_FEATUREGROUPS,
            Level.FINE, "featuregroupId: " + featuregroupDTO.getId());
      default:
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_TYPE, Level.FINE,
            ", Recognized Feature group types are: " + FeaturegroupType.ON_DEMAND_FEATURE_GROUP + ", and: " +
            FeaturegroupType.CACHED_FEATURE_GROUP + ". The provided feature group type was not recognized: "
            + featuregroupDTO.getFeaturegroupType());
    }
  }

  /**
   * Executes "SHOW CREATE TABLE" on the hive table of the featuregroup formats it as a string and returns it
   *
   * @param featuregroupDTO the featuregroup to get the schema for
   * @param user            the user making the request
   * @param featurestore    the featurestore where the featuregroup resides
   * @return JSON/XML DTO with the schema
   * @throws SQLException
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public RowValueQueryResult getDDLSchema(FeaturegroupDTO featuregroupDTO, Users user, Featurestore featurestore)
      throws SQLException, FeaturestoreException, HopsSecurityException {
    switch (featuregroupDTO.getFeaturegroupType()) {
      case CACHED_FEATURE_GROUP:
        return cachedFeaturegroupController.getDDLSchema(featuregroupDTO, user, featurestore);
      case ON_DEMAND_FEATURE_GROUP:
        throw new FeaturestoreException(
            RESTCodes.FeaturestoreErrorCode.CANNOT_FETCH_HIVE_SCHEMA_FOR_ON_DEMAND_FEATUREGROUPS,
            Level.FINE, "featuregroupId: " + featuregroupDTO.getId());
      default:
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_TYPE, Level.FINE,
            ", Recognized Feature group types are: " + FeaturegroupType.ON_DEMAND_FEATURE_GROUP + ", and: " +
            FeaturegroupType.CACHED_FEATURE_GROUP + ". The provided feature group type was not recognized: "
            + featuregroupDTO.getFeaturegroupType());
    }
  }
  
  /**
   * Verifies the id of a feature group
   *
   * @param featuregroupId the id of the feature group
   * @param featurestore the featurestore to query
   * @return the featuregroup with the id if it passed the validation
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  private Featuregroup verifyFeaturegroupId(Integer featuregroupId, Featurestore featurestore) {
    Featuregroup featuregroup = null;
    if(featurestore != null){
      featuregroup = featuregroupFacade.findByIdAndFeaturestore(featuregroupId, featurestore);
    } else {
      featuregroup = featuregroupFacade.findById(featuregroupId);
    }
    if (featuregroup == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_NOT_FOUND +
        "featuregroupId: " + featuregroupId);
    }
    return featuregroup;
  }

  /**
   * Verify user input
   *
   * @param featuregroupDTO the provided user input
   * @param featurestore    the feature store to perform the operation against
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  private void verifyFeaturegroupUserInput(FeaturegroupDTO featuregroupDTO, Featurestore featurestore)
    throws FeaturestoreException {
    if (featurestore == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_NOT_FOUND.getMessage());
    }
    if (featuregroupDTO.getFeaturegroupType() != FeaturegroupType.CACHED_FEATURE_GROUP &&
        featuregroupDTO.getFeaturegroupType() != FeaturegroupType.ON_DEMAND_FEATURE_GROUP) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_TYPE, Level.FINE,
          ", Recognized Feature group types are: " + FeaturegroupType.ON_DEMAND_FEATURE_GROUP + ", and: " +
          FeaturegroupType.CACHED_FEATURE_GROUP + ". The provided feature group type was not recognized: "
          + featuregroupDTO.getFeaturegroupType());
    }
    if (featuregroupDTO.getVersion() == null) {
      throw new IllegalArgumentException(
          RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_VERSION_NOT_PROVIDED.getMessage());
    }
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
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public FeaturegroupDTO syncHiveTableWithFeaturestore(Featurestore featurestore, FeaturegroupDTO featuregroupDTO,
    Users user) throws FeaturestoreException {
    //Verify basic feature group input information
    verifyFeaturegroupUserInput(featuregroupDTO, featurestore);
  
    //Verify statistics input (more detailed input verification is delegated to lower level controllers)
    verifyStatisticsInput(featuregroupDTO);
  
    //Extract metadata
    String hdfsUsername = hdfsUsersController.getHdfsUserName(featurestore.getProject(), user);
    HdfsUsers hdfsUser = hdfsUsersFacade.findByName(hdfsUsername);
  
    CachedFeaturegroup cachedFeaturegroup = null;
    switch (featuregroupDTO.getFeaturegroupType()) {
      case CACHED_FEATURE_GROUP:
        cachedFeaturegroup = cachedFeaturegroupController.syncHiveTableWithFeaturestore(featurestore,
          (CachedFeaturegroupDTO) featuregroupDTO);
        break;
      case ON_DEMAND_FEATURE_GROUP:
        throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_TYPE.getMessage()
          + ", Only cached feature groups can be synced from an existing Hive table, not on-demand feature groups.");
      default:
        throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_TYPE.getMessage()
          + ", Recognized Feature group types are: " + FeaturegroupType.ON_DEMAND_FEATURE_GROUP + ", and: " +
          FeaturegroupType.CACHED_FEATURE_GROUP + ". The provided feature group type was not recognized: "
          + featuregroupDTO.getFeaturegroupType());
    }
  
    //Persist basic feature group metadata
    Featuregroup featuregroup = persistFeaturegroupMetadata(featurestore, hdfsUser, user, featuregroupDTO,
      cachedFeaturegroup, null);
  
    // Store statistics
    featurestoreStatisticController.updateFeaturestoreStatistics(featuregroup, null,
      featuregroupDTO.getFeatureCorrelationMatrix(), featuregroupDTO.getDescriptiveStatistics(),
      featuregroupDTO.getFeaturesHistogram(), featuregroupDTO.getClusterAnalysis());
  
    //Get jobs
    List<Jobs> jobs = getJobs(featuregroupDTO.getJobs(), featurestore.getProject());
  
    //Store jobs
    featurestoreJobController.insertJobs(featuregroup, jobs);
    
    return featuregroupDTO;
  }
  
  /**
   * Persists metadata of a new feature group in the feature_group table
   *
   * @param featurestore the featurestore of the feature group
   * @param hdfsUser the HDFS user making the request
   * @param user the Hopsworks user making the request
   * @param featuregroupDTO DTO of the feature group
   * @param cachedFeaturegroup the cached feature group that the feature group is linked to (if any)
   * @param onDemandFeaturegroup the on-demand feature group that the feature group is linked to (if any)
   * @return the created entity
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  private Featuregroup persistFeaturegroupMetadata(Featurestore featurestore, HdfsUsers hdfsUser, Users user,
    FeaturegroupDTO featuregroupDTO, CachedFeaturegroup cachedFeaturegroup, OnDemandFeaturegroup onDemandFeaturegroup) {
    Featuregroup featuregroup = new Featuregroup();
    featuregroup.setFeaturestore(featurestore);
    featuregroup.setHdfsUserId(hdfsUser.getId());
    featuregroup.setCreated(new Date());
    featuregroup.setCreator(user);
    featuregroup.setVersion(featuregroupDTO.getVersion());
    featuregroup.setFeaturegroupType(featuregroupDTO.getFeaturegroupType());
    featuregroup.setCachedFeaturegroup(cachedFeaturegroup);
    featuregroup.setOnDemandFeaturegroup(onDemandFeaturegroup);
    // check if null to handle old clients and use entity defaults
    verifyAndSetFeaturegroupStatsSettings(featuregroupDTO, featuregroup);
    featuregroupFacade.persist(featuregroup);
    return featuregroup;
  }
  
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public FeaturegroupDTO getCachedFeaturegroupDTO(Featurestore featurestore,
      Integer featuregroupId) throws FeaturestoreException {
    FeaturegroupDTO featuregroupDTO = getFeaturegroupWithIdAndFeaturestore(featurestore, featuregroupId);
  
    if(featuregroupDTO.getFeaturegroupType() != FeaturegroupType.CACHED_FEATURE_GROUP)
      throw new FeaturestoreException(
          RESTCodes.FeaturestoreErrorCode.XATTRS_OPERATIONS_ONLY_SUPPORTED_FOR_CACHED_FEATUREGROUPS,
          Level.FINE);
    
    return featuregroupDTO;
  }
}
