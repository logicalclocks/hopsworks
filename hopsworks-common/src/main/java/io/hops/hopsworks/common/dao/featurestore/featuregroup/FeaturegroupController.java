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

package io.hops.hopsworks.common.dao.featurestore.featuregroup;

import io.hops.hopsworks.common.dao.featurestore.Featurestore;
import io.hops.hopsworks.common.dao.featurestore.FeaturestoreFacade;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.cached_featuregroup.CachedFeaturegroup;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.cached_featuregroup.CachedFeaturegroupController;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.cached_featuregroup.CachedFeaturegroupDTO;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.cached_featuregroup.RowValueQueryResult;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.on_demand_featuregroup.OnDemandFeaturegroup;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.on_demand_featuregroup.OnDemandFeaturegroupController;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.on_demand_featuregroup.OnDemandFeaturegroupDTO;
import io.hops.hopsworks.common.dao.featurestore.settings.FeaturestoreClientSettingsDTO;
import io.hops.hopsworks.common.dao.featurestore.stats.FeaturestoreStatisticController;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
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

  /**
   * Gets all featuregroups for a particular featurestore and project, using the userCerts to query Hive
   *
   * @param featurestore featurestore to query featuregroups for
   * @return list of XML/JSON DTOs of the featuregroups
   */
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
      throws FeaturestoreException, HopsSecurityException, SQLException {
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
      throws FeaturestoreException, HopsSecurityException, SQLException {
    
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
    Featuregroup featuregroup = new Featuregroup();
    featuregroup.setFeaturestore(featurestore);
    featuregroup.setHdfsUserId(hdfsUser.getId());
    featuregroup.setJob(null);
    featuregroup.setCreated(new Date());
    featuregroup.setCreator(user);
    featuregroup.setVersion(featuregroupDTO.getVersion());
    featuregroup.setFeaturegroupType(featuregroupDTO.getFeaturegroupType());
    featuregroup.setCachedFeaturegroup(cachedFeaturegroup);
    featuregroup.setOnDemandFeaturegroup(onDemandFeaturegroup);
    featuregroupFacade.persist(featuregroup);

    // Store statistics
    featurestoreStatisticController.updateFeaturestoreStatistics(featuregroup, null,
        featuregroupDTO.getFeatureCorrelationMatrix(), featuregroupDTO.getDescriptiveStatistics(),
        featuregroupDTO.getFeaturesHistogram(), featuregroupDTO.getClusterAnalysis());
    
    return convertFeaturegrouptoDTO(featuregroup);
  }

  /**
   * Convert a featuregroup entity to a DTO representation
   *
   * @param featuregroup the entity to convert
   * @return a DTO representation of the entity
   */
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
   * @param id           if of the featuregroup
   * @param featurestore the featurestore that the featuregroup belongs to
   * @return XML/JSON DTO of the featuregroup
   */
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
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public FeaturegroupDTO updateFeaturegroupMetadata(
      Featurestore featurestore, FeaturegroupDTO featuregroupDTO) {
    Featuregroup featuregroup = verifyFeaturegroupId(featuregroupDTO.getId(), featurestore);
    if(featuregroup.getFeaturegroupType() == FeaturegroupType.ON_DEMAND_FEATURE_GROUP){
      onDemandFeaturegroupController.updateOnDemandFeaturergroupMetadata(featuregroup.getOnDemandFeaturegroup(),
        (OnDemandFeaturegroupDTO) featuregroupDTO);
    }
    return  convertFeaturegrouptoDTO(featuregroup);
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
   * Verifies statistics user input for a feature group
   *
   * @param featuregroupDTO DTO containing the feature group statistics
   */
  public void verifyStatisticsInput(FeaturegroupDTO featuregroupDTO) {
    if (featuregroupDTO.getFeatureCorrelationMatrix() != null &&
        featuregroupDTO.getFeatureCorrelationMatrix().getFeatureCorrelations().size() >
            FeaturestoreClientSettingsDTO.FEATURESTORE_STATISTICS_MAX_CORRELATIONS) {
      throw new IllegalArgumentException(
          RESTCodes.FeaturestoreErrorCode.CORRELATION_MATRIX_EXCEED_MAX_SIZE.getMessage());
    }
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
  public FeaturegroupDTO deleteFeaturegroupIfExists(
      Featurestore featurestore, FeaturegroupDTO featuregroupDTO, Users user)
      throws SQLException, FeaturestoreException, HopsSecurityException {
    Featuregroup featuregroup = null;
    if (featuregroupDTO.getId() != null) {
      featuregroup = verifyFeaturegroupId(featuregroupDTO.getId(), null);
    } else {
      if (featuregroupDTO.getId() == null) {
        List<Featuregroup> featuregroups = featuregroupFacade.findByFeaturestore(featurestore);
        featuregroups = featuregroups.stream().filter(fg -> {
          FeaturegroupDTO convertedFeaturegroupDTO = convertFeaturegrouptoDTO(fg);
          return convertedFeaturegroupDTO.getName().equals(featuregroupDTO.getName()) &&
              convertedFeaturegroupDTO.getVersion() == featuregroupDTO.getVersion();
        }).collect(Collectors.toList());
        if (!featuregroups.isEmpty())
          featuregroup = featuregroups.get(0);
      } else {
        featuregroup = featuregroupFacade.findById(featuregroupDTO.getId());
      }
    }

    if (featuregroup != null) {
      FeaturegroupDTO convertedFeaturegroupDTO = convertFeaturegrouptoDTO(featuregroup);
      switch (featuregroup.getFeaturegroupType()) {
        case CACHED_FEATURE_GROUP:
          //Delete hive_table will cascade to cached_featuregroup_table which will cascade to feature_group table
          cachedFeaturegroupController.dropHiveFeaturegroup(convertedFeaturegroupDTO, featurestore, user);
          break;
        case ON_DEMAND_FEATURE_GROUP:
          //Delete on_demand_feature_group will cascade will cascade to feature_group table
          onDemandFeaturegroupController.removeOnDemandFeaturegroup(featuregroup.getOnDemandFeaturegroup());
          break;
        default:
          throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_TYPE.getMessage()
              + ", Recognized Feature group types are: " + FeaturegroupType.ON_DEMAND_FEATURE_GROUP + ", and: " +
              FeaturegroupType.CACHED_FEATURE_GROUP + ". The provided feature group type was not recognized: "
              + featuregroup.getFeaturegroupType());
      }
      return convertedFeaturegroupDTO;
    } else {
      return null;
    }
  }

  /**
   * Previews a given featuregroup by doing a SELECT LIMIT query on the Hive Table
   *
   * @param featuregroupDTO DTO of the featuregroup to preview
   * @param featurestore    the feature store where the feature group resides
   * @param user            the user making the request
   * @return list of feature-rows from the Hive table where the featuregroup is stored
   * @throws SQLException
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public List<RowValueQueryResult> getFeaturegroupPreview(
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
        throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_TYPE.getMessage()
            + ", Recognized Feature group types are: " + FeaturegroupType.ON_DEMAND_FEATURE_GROUP + ", and: " +
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
        throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_TYPE.getMessage()
            + ", Recognized Feature group types are: " + FeaturegroupType.ON_DEMAND_FEATURE_GROUP + ", and: " +
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
   */
  public void verifyFeaturegroupUserInput(FeaturegroupDTO featuregroupDTO, Featurestore featurestore) {
    if (featurestore == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_NOT_FOUND.getMessage());
    }
    if (featuregroupDTO.getFeaturegroupType() != FeaturegroupType.CACHED_FEATURE_GROUP &&
        featuregroupDTO.getFeaturegroupType() != FeaturegroupType.ON_DEMAND_FEATURE_GROUP) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_TYPE.getMessage()
          + ", Recognized Feature group types are: " + FeaturegroupType.ON_DEMAND_FEATURE_GROUP + ", and: " +
          FeaturegroupType.CACHED_FEATURE_GROUP + ". The provided feature group type was not recognized: "
          + featuregroupDTO.getFeaturegroupType());
    }
    if (featuregroupDTO.getVersion() == null) {
      throw new IllegalArgumentException(
          RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_VERSION_NOT_PROVIDED.getMessage());
    }
  }
}
