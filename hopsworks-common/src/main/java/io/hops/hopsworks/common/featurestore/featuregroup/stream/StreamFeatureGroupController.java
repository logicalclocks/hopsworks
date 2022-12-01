/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 *  Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 *  If not, see <https://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.featurestore.featuregroup.stream;

import io.hops.hopsworks.common.featurestore.activity.FeaturestoreActivityFacade;
import io.hops.hopsworks.common.featurestore.datavalidationv2.suites.ExpectationSuiteController;
import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.CachedFeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.FeaturegroupPreview;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.OfflineFeatureGroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.online.OnlineFeaturegroupController;
import io.hops.hopsworks.common.featurestore.utils.FeaturestoreUtils;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.SchemaException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.activity.FeaturestoreActivityMeta;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.CachedFeature;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.hive.HiveTableParams;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.hive.HiveTbls;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.TimeTravelFormat;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.stream.StreamFeatureGroup;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Class controlling the interaction with the stream_feature_group table and required business logic
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class StreamFeatureGroupController {
  
  @EJB
  private StreamFeatureGroupFacade streamFeatureGroupFacade;
  @EJB
  private OnlineFeaturegroupController onlineFeaturegroupController;
  @EJB
  private OfflineFeatureGroupController offlineFeatureGroupController;
  @EJB
  private FeaturegroupController featuregroupController;
  @EJB
  private FeaturestoreUtils featurestoreUtils;
  @EJB
  private CachedFeaturegroupController cachedFeaturegroupController;
  @EJB
  private FeaturestoreActivityFacade fsActivityFacade;
  @EJB
  private ExpectationSuiteController expectationSuiteController;
  
  /**
   * Converts a StreamFeatureGroup entity into a DTO representation
   *
   * @param featuregroup the entity to convert
   * @return the converted DTO representation
   */
  public StreamFeatureGroupDTO convertStreamFeatureGroupToDTO(Featuregroup featuregroup, Project project, Users user)
    throws FeaturestoreException, ServiceException {
    List<FeatureGroupFeatureDTO> featureGroupFeatureDTOS;
    if (featuregroup.getExpectationSuite() != null) {
      featuregroup.setExpectationSuite(
        expectationSuiteController.addAllExpectationIdToMetaField(featuregroup.getExpectationSuite()));
    }
    StreamFeatureGroupDTO streamFeatureGroupDTO = new StreamFeatureGroupDTO(featuregroup);
  
    if (featuregroup.getStreamFeatureGroup().isOnlineEnabled()) {
      streamFeatureGroupDTO.setOnlineTopicName(onlineFeaturegroupController
        .onlineFeatureGroupTopicName(project.getId(), featuregroup.getId(),
          Utils.getFeaturegroupName(featuregroup)));
      featureGroupFeatureDTOS = onlineFeaturegroupController.getFeaturegroupFeatures(featuregroup,
        cachedFeaturegroupController.getFeaturesDTO(featuregroup.getStreamFeatureGroup(), featuregroup.getId(),
            featuregroup.getFeaturestore(), project, user));
    } else {
      streamFeatureGroupDTO.setOnlineTopicName(offlineStreamFeatureGroupTopicName(project.getId(),
        featuregroup.getId(), Utils.getFeaturegroupName(featuregroup)));
      featureGroupFeatureDTOS = cachedFeaturegroupController.getFeaturesDTO(
        featuregroup.getStreamFeatureGroup(), featuregroup.getId(), featuregroup.getFeaturestore(), project, user);
    }
  
    streamFeatureGroupDTO.setFeatures(featureGroupFeatureDTOS);
    streamFeatureGroupDTO.setName(featuregroup.getName());
    streamFeatureGroupDTO.setDescription(
      featuregroup.getStreamFeatureGroup().getHiveTbls().getHiveTableParamsCollection().stream()
        .filter(p -> p.getHiveTableParamsPK().getParamKey().equalsIgnoreCase("COMMENT"))
        .map(HiveTableParams::getParamValue)
        .findFirst()
        .orElse("")
    );
    streamFeatureGroupDTO.setOnlineEnabled(featuregroup.getStreamFeatureGroup().isOnlineEnabled());
    
    streamFeatureGroupDTO.setLocation(featurestoreUtils.resolveLocationURI(
      featuregroup.getStreamFeatureGroup().getHiveTbls().getSdId().getLocation()));
    return streamFeatureGroupDTO;
  }
  
  /**
   * Persists metadata of a new stream feature group in the stream_feature_group table
   *
   * @param hiveTable the id of the Hive table in the Hive metastore
   * @param featureGroupFeatureDTOS the list of the feature group feature DTOs
   * @param onlineEnabled
   * @return Entity of the created cached feature group
   */
  private StreamFeatureGroup persistStreamFeatureGroupMetadata(HiveTbls hiveTable,
    List<FeatureGroupFeatureDTO> featureGroupFeatureDTOS, Boolean onlineEnabled) {
    StreamFeatureGroup streamFeatureGroup = new StreamFeatureGroup();
    streamFeatureGroup.setHiveTbls(hiveTable);
    streamFeatureGroup.setCachedFeatures(featureGroupFeatureDTOS.stream()
      // right now we are filtering and saving only features with description separately, if we add more
      // information to the cached_feature table in the future, we might want to change this and persist a row for
      // every feature
      .filter(feature -> feature.getDescription() != null)
      .map(feature -> new CachedFeature(streamFeatureGroup, feature.getName(), feature.getDescription()))
      .collect(Collectors.toList()));
    streamFeatureGroup.setFeaturesExtraConstraints(
      cachedFeaturegroupController.buildFeatureExtraConstrains(featureGroupFeatureDTOS, null, streamFeatureGroup ));
    streamFeatureGroup.setOnlineEnabled(onlineEnabled);
  
    streamFeatureGroupFacade.persist(streamFeatureGroup);
    return streamFeatureGroup;
  }
  
  /**
   * Persists a stream feature group
   *
   * @param featurestore the featurestore of the feature group
   * @param streamFeatureGroupDTO the user input data to use when creating the stream feature group
   * @param project the project where feature group is created
   * @param user the user making the request
   * @return the created entity
   */
  public StreamFeatureGroup createStreamFeatureGroup(Featurestore featurestore,
    StreamFeatureGroupDTO streamFeatureGroupDTO, Project project, Users user) throws FeaturestoreException {
    cachedFeaturegroupController.verifyPrimaryKey(streamFeatureGroupDTO.getFeatures(), TimeTravelFormat.HUDI);
    
    //Prepare DDL statement
    String tableName = featuregroupController.getTblName(streamFeatureGroupDTO.getName(),
      streamFeatureGroupDTO.getVersion());
    offlineFeatureGroupController.createHiveTable(featurestore, tableName, streamFeatureGroupDTO.getDescription(),
      cachedFeaturegroupController.addHudiSpecFeatures(streamFeatureGroupDTO.getFeatures()),
      project, user, OfflineFeatureGroupController.Formats.HUDI);
    
    //Get HiveTblId of the newly created table from the metastore
    HiveTbls hiveTbls = streamFeatureGroupFacade.getHiveTableByNameAndDB(tableName, featurestore.getHiveDbId())
      .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_CREATE_FEATUREGROUP,
        Level.WARNING, "Table created correctly but not in the metastore"));
    
    //Persist stream feature group
    return persistStreamFeatureGroupMetadata(
      hiveTbls, streamFeatureGroupDTO.getFeatures(), streamFeatureGroupDTO.getOnlineEnabled());
  }
  
  /**
   * Previews a given featuregroup by doing a SELECT LIMIT query on the Hive Table (offline feature data)
   * and the MySQL table (online feature data)
   *
   * @param featuregroup    of the featuregroup to preview
   * @param project         the project the user is operating from, in case of shared feature store
   * @param user            the user making the request
   * @param partition       the selected partition if any as represented in the PARTITIONS_METASTORE
   * @param online          whether to show preview from the online feature store
   * @param limit           the number of rows to visualize
   * @return A DTO with the first 20 feature rows of the online and offline tables.
   * @throws SQLException
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  public FeaturegroupPreview getFeaturegroupPreview(Featuregroup featuregroup, Project project,
    Users user, String partition, boolean online, int limit)
    throws SQLException, FeaturestoreException, HopsSecurityException {
    if (online && featuregroup.getStreamFeatureGroup().isOnlineEnabled()) {
      return onlineFeaturegroupController.getFeaturegroupPreview(featuregroup, project, user, limit);
    } else if (online) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_NOT_ONLINE, Level.FINE);
    } else {
      return cachedFeaturegroupController.getOfflineFeaturegroupPreview(featuregroup, project, user, partition, limit);
    }
  }
  
  
  public void updateMetadata(Project project, Users user, Featuregroup featuregroup,
    FeaturegroupDTO featuregroupDTO)
    throws FeaturestoreException, SQLException, SchemaException, KafkaException {
    
    List<FeatureGroupFeatureDTO> previousSchema =
      cachedFeaturegroupController.getFeaturesDTO(featuregroup.getStreamFeatureGroup(),
      featuregroup.getId(), featuregroup.getFeaturestore(), project,
      user);
    
    String tableName = featuregroupController.getTblName(featuregroup.getName(), featuregroup.getVersion());
    
    // verify user input specific for cached feature groups - if any
    List<FeatureGroupFeatureDTO> newFeatures = new ArrayList<>();
    if (featuregroupDTO.getFeatures() != null) {
      cachedFeaturegroupController.verifyPreviousSchemaUnchanged(previousSchema, featuregroupDTO.getFeatures());
      newFeatures = cachedFeaturegroupController.verifyAndGetNewFeatures(previousSchema, featuregroupDTO.getFeatures());
    }
    
    // change table description
    if (featuregroupDTO.getDescription() != null) {
      offlineFeatureGroupController.alterHiveTableDescription(
        featuregroup.getFeaturestore(), tableName, featuregroupDTO.getDescription(), project, user);
    }
    
    // change feature descriptions
    updateCachedDescriptions(featuregroup.getStreamFeatureGroup(), featuregroupDTO.getFeatures());
    
    // alter table for new additional features
    if (!newFeatures.isEmpty()) {
      offlineFeatureGroupController.alterHiveTableFeatures(
        featuregroup.getFeaturestore(), tableName, newFeatures, project, user);
      if (featuregroup.getStreamFeatureGroup().isOnlineEnabled()) {
        onlineFeaturegroupController.alterOnlineFeatureGroupSchema(
          featuregroup, newFeatures, featuregroupDTO.getFeatures(), project, user);
      } else {
        alterOfflineStreamFeatureGroupSchema(featuregroup, featuregroupDTO.getFeatures(), project);
      }

      // Log schema change
      String newFeaturesStr = "New features: " + newFeatures.stream().map(FeatureGroupFeatureDTO::getName)
        .collect(Collectors.joining(","));
      fsActivityFacade.logMetadataActivity(user, featuregroup, FeaturestoreActivityMeta.FG_ALTERED, newFeaturesStr);
    }
  }
  
  private void updateCachedDescriptions(StreamFeatureGroup streamFeatureGroup,
    List<FeatureGroupFeatureDTO> featureGroupFeatureDTOs) {
    for (FeatureGroupFeatureDTO feature : featureGroupFeatureDTOs) {
      Optional<CachedFeature> previousCachedFeature =
        cachedFeaturegroupController.getCachedFeature(streamFeatureGroup.getCachedFeatures(), feature.getName());
      if (feature.getDescription() != null) {
        if (previousCachedFeature.isPresent()) {
          previousCachedFeature.get().setDescription(feature.getDescription());
        } else {
          streamFeatureGroup.getCachedFeatures().add(new CachedFeature(streamFeatureGroup, feature.getName(),
            feature.getDescription()));
        }
      }
    }
    streamFeatureGroupFacade.updateMetadata(streamFeatureGroup);
  }
  
  public void deleteOfflineStreamFeatureGroupTopic(Project project, Featuregroup featureGroup)
      throws SchemaException, KafkaException {
    String topicName = offlineStreamFeatureGroupTopicName(
      project.getId(), featureGroup.getId(), Utils.getFeaturegroupName(featureGroup));
    onlineFeaturegroupController.deleteFeatureGroupKafkaTopic(project, topicName);
  }
  
  private void alterOfflineStreamFeatureGroupSchema(Featuregroup featureGroup,
                                                    List<FeatureGroupFeatureDTO> fullNewSchema, Project project)
      throws SchemaException, KafkaException, FeaturestoreException {
    String topicName = offlineStreamFeatureGroupTopicName(
      project.getId(), featureGroup.getId(), Utils.getFeaturegroupName(featureGroup));
    onlineFeaturegroupController.alterFeatureGroupSchema(featureGroup, fullNewSchema, topicName, project);
  }
  
  public void setupOfflineStreamFeatureGroup(Project project, Featuregroup featureGroup,
                                             List<FeatureGroupFeatureDTO> features)
      throws ProjectException, SchemaException, KafkaException, UserException, FeaturestoreException {
    String featureGroupEntityName = Utils.getFeaturegroupName(featureGroup);
    String topicName = offlineStreamFeatureGroupTopicName(
      project.getId(), featureGroup.getId(), featureGroupEntityName);
    
    onlineFeaturegroupController.createFeatureGroupKafkaTopic(project, featureGroupEntityName, topicName, features);
  }
  
  public String offlineStreamFeatureGroupTopicName(Integer projectId, Integer featureGroupId,
                                                   String featureGroupEntityName) {
    return projectId.toString() + "_" + featureGroupId.toString() + "_" + featureGroupEntityName;
  }
}
