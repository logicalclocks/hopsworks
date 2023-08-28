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
import io.hops.hopsworks.common.featurestore.featuregroup.FeatureGroupInputValidation;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.CachedFeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.OfflineFeatureGroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.online.OnlineFeaturegroupController;
import io.hops.hopsworks.common.featurestore.utils.FeaturestoreUtils;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.exceptions.SchemaException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.activity.FeaturestoreActivityMeta;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.CachedFeature;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.CachedFeatureExtraConstraints;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.TimeTravelFormat;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.hive.HiveTableParams;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.hive.HiveTbls;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.stream.StreamFeatureGroup;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.hive.metastore.api.SQLDefaultConstraint;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
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
  @EJB
  private FeatureGroupInputValidation featureGroupInputValidation;

  /**
   * Converts a StreamFeatureGroup entity into a DTO representation
   *
   * @param featuregroup the entity to convert
   * @return the converted DTO representation
   */
  public StreamFeatureGroupDTO convertStreamFeatureGroupToDTO(Featuregroup featuregroup, Project project, Users user)
    throws FeaturestoreException, ServiceException {
    StreamFeatureGroupDTO streamFeatureGroupDTO = new StreamFeatureGroupDTO(featuregroup);

    if (featuregroup.isOnlineEnabled()) {
      streamFeatureGroupDTO.setOnlineTopicName(onlineFeaturegroupController
        .onlineFeatureGroupTopicName(project.getId(), featuregroup.getId(),
          Utils.getFeaturegroupName(featuregroup)));
    } else {
      streamFeatureGroupDTO.setOnlineTopicName(offlineStreamFeatureGroupTopicName(project.getId(),
        featuregroup.getId(), Utils.getFeaturegroupName(featuregroup)));
    }

    streamFeatureGroupDTO.setName(featuregroup.getName());
    streamFeatureGroupDTO.setDescription(
      featuregroup.getStreamFeatureGroup().getHiveTbls().getHiveTableParamsCollection().stream()
        .filter(p -> p.getHiveTableParamsPK().getParamKey().equalsIgnoreCase("COMMENT"))
        .map(HiveTableParams::getParamValue)
        .findFirst()
        .orElse("")
    );
    streamFeatureGroupDTO.setOnlineEnabled(featuregroup.isOnlineEnabled());

    streamFeatureGroupDTO.setLocation(featurestoreUtils.resolveLocationURI(
      featuregroup.getStreamFeatureGroup().getHiveTbls().getSdId().getLocation()));
    return streamFeatureGroupDTO;
  }

  public List<FeatureGroupFeatureDTO> getFeaturesDTO(StreamFeatureGroup streamFeatureGroup,
      Integer featureGroupId, Featurestore featurestore,
      Project project, Users user) throws FeaturestoreException {
    Collection<CachedFeatureExtraConstraints> featureExtraConstraints =
        streamFeatureGroup.getFeaturesExtraConstraints();

    HiveTbls hiveTable = streamFeatureGroup.getHiveTbls();

    List<SQLDefaultConstraint> defaultConstraints =
        offlineFeatureGroupController.getDefaultConstraints(featurestore, hiveTable.getTblName(), project, user);
    Collection<CachedFeature> cachedFeatures = streamFeatureGroup.getCachedFeatures();

    return cachedFeaturegroupController.getFeatureGroupFeatureDTOS(featureExtraConstraints, defaultConstraints,
        hiveTable, cachedFeatures,
        featureGroupId, true);
  }

  public List<FeatureGroupFeatureDTO> getFeaturesDTOOnlineChecked(Featuregroup featuregroup,
      StreamFeatureGroup streamFeatureGroup, Integer featureGroupId,
      Featurestore featurestore, Project project, Users user) throws FeaturestoreException {
    List<FeatureGroupFeatureDTO> featureGroupFeatureDTOS =
        getFeaturesDTO(streamFeatureGroup, featureGroupId, featurestore, project, user);
    if (featuregroup.isOnlineEnabled()) {
      featureGroupFeatureDTOS = onlineFeaturegroupController.getFeaturegroupFeatures(featuregroup,
          featureGroupFeatureDTOS);
    }
    return featureGroupFeatureDTOS;
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
    cachedFeaturegroupController.verifyPrimaryKey(streamFeatureGroupDTO, TimeTravelFormat.HUDI);

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

  public void updateMetadata(Project project, Users user, Featuregroup featuregroup,
    FeaturegroupDTO featuregroupDTO)
    throws FeaturestoreException, SQLException, SchemaException, KafkaException {

    List<FeatureGroupFeatureDTO> previousSchema =
      getFeaturesDTO(featuregroup.getStreamFeatureGroup(),
      featuregroup.getId(), featuregroup.getFeaturestore(), project,
      user);

    String tableName = featuregroupController.getTblName(featuregroup.getName(), featuregroup.getVersion());

    // verify user input specific for cached feature groups - if any
    List<FeatureGroupFeatureDTO> newFeatures = new ArrayList<>();
    if (featuregroupDTO.getFeatures() != null) {
      cachedFeaturegroupController.verifyPreviousSchemaUnchanged(previousSchema, featuregroupDTO.getFeatures());
      newFeatures = featureGroupInputValidation.verifyAndGetNewFeatures(previousSchema, featuregroupDTO.getFeatures());
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
      if (featuregroup.isOnlineEnabled()) {
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
      throws SchemaException, KafkaException, FeaturestoreException {
    String featureGroupEntityName = Utils.getFeaturegroupName(featureGroup);
    String topicName = offlineStreamFeatureGroupTopicName(
      project.getId(), featureGroup.getId(), featureGroupEntityName);
    
    onlineFeaturegroupController.createFeatureGroupKafkaTopic(project, featureGroupEntityName, topicName,
            features);
  }

  public String offlineStreamFeatureGroupTopicName(Integer projectId, Integer featureGroupId,
                                                   String featureGroupEntityName) {
    return projectId.toString() + "_" + featureGroupId.toString() + "_" + featureGroupEntityName;
  }
}
