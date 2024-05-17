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

import io.hops.hopsworks.common.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.featurestore.activity.FeaturestoreActivityFacade;
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
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.stream.StreamFeatureGroup;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
  private FeatureGroupInputValidation featureGroupInputValidation;
  @EJB
  private FeaturestoreController featurestoreController;

  /**
   * Converts a StreamFeatureGroup entity into a DTO representation
   *
   * @param featuregroup the entity to convert
   * @return the converted DTO representation
   */
  public StreamFeatureGroupDTO convertStreamFeatureGroupToDTO(Featuregroup featuregroup)
    throws ServiceException {
    StreamFeatureGroupDTO streamFeatureGroupDTO = new StreamFeatureGroupDTO(featuregroup);
    streamFeatureGroupDTO.setOnlineTopicName(Utils.getFeatureGroupTopicName(featuregroup));
    streamFeatureGroupDTO.setName(featuregroup.getName());
    streamFeatureGroupDTO.setDescription(featuregroup.getDescription());
    streamFeatureGroupDTO.setOnlineEnabled(featuregroup.isOnlineEnabled());
    
    streamFeatureGroupDTO.setLocation(
            featurestoreUtils.resolveLocation(featuregroupController.getFeatureGroupLocation(featuregroup)));
    return streamFeatureGroupDTO;
  }

  public List<FeatureGroupFeatureDTO> getFeaturesDTO(Featuregroup featuregroup,
                                                     Project project, Users user) throws FeaturestoreException {
    Set<String> primaryKeys = getPrimaryKeys(featuregroup);

    Set<String> precombineKeys = featuregroup.getStreamFeatureGroup().getFeaturesExtraConstraints().stream()
        .filter(CachedFeatureExtraConstraints::getHudiPrecombineKey)
        .map(CachedFeatureExtraConstraints::getName)
        .collect(Collectors.toSet());

    Map<String, String> featureDescription = featuregroup.getStreamFeatureGroup().getCachedFeatures().stream()
        .collect(Collectors.toMap(CachedFeature::getName, CachedFeature::getDescription));

    List<FeatureGroupFeatureDTO> featureGroupFeatures = offlineFeatureGroupController.getSchema(
        featuregroup.getFeaturestore(),
        featuregroupController.getTblName(featuregroup),
        project, user);

    for (FeatureGroupFeatureDTO feature : featureGroupFeatures) {
      feature.setPrimary(primaryKeys.contains(feature.getName()));
      feature.setHudiPrecombineKey(precombineKeys.contains(feature.getName()));
      feature.setDescription(featureDescription.get(feature.getName()));
      feature.setFeatureGroupId(featuregroup.getId());
    }

    featureGroupFeatures = cachedFeaturegroupController.dropHudiSpecFeatureGroupFeature(featureGroupFeatures);

    return featureGroupFeatures;
  }

  public Set<String> getPrimaryKeys(Featuregroup featuregroup) {
    return featuregroup.getStreamFeatureGroup().getFeaturesExtraConstraints().stream()
        .filter(CachedFeatureExtraConstraints::getPrimary)
        .map(CachedFeatureExtraConstraints::getName)
        .collect(Collectors.toSet());
  }

  public List<FeatureGroupFeatureDTO> getFeaturesDTOOnlineChecked(Featuregroup featuregroup,
                                                                  Project project, Users user)
      throws FeaturestoreException {
    List<FeatureGroupFeatureDTO> featureGroupFeatureDTOS = getFeaturesDTO(featuregroup, project, user);
    if (featuregroup.isOnlineEnabled()) {
      featureGroupFeatureDTOS = onlineFeaturegroupController.getFeaturegroupFeatures(featuregroup,
          featureGroupFeatureDTOS);
    }
    return featureGroupFeatureDTOS;
  }

  /**
   * Drop a feature group
   * @param featuregroup
   * @param project
   * @param user
   * @throws FeaturestoreException
   * @throws IOException
   * @throws ServiceException
   */
  public void deleteFeatureGroup(Featuregroup featuregroup, Project project, Users user)
      throws FeaturestoreException, IOException, ServiceException {
    // Drop the table from Hive
    String db = featurestoreController.getOfflineFeaturestoreDbName(featuregroup.getFeaturestore().getProject());
    String tableName = featuregroupController.getTblName(featuregroup.getName(), featuregroup.getVersion());
    offlineFeatureGroupController.dropFeatureGroup(db, tableName, project, user);

    // remove the metadata from the Hopswroks schema
    streamFeatureGroupFacade.remove(featuregroup.getStreamFeatureGroup());
  }

  /**
   * Persists metadata of a new stream feature group in the stream_feature_group table
   *
   * @param featureGroupFeatureDTOS the list of the feature group feature DTOs
   * @return Entity of the created cached feature group
   */
  private StreamFeatureGroup persistStreamFeatureGroupMetadata(List<FeatureGroupFeatureDTO> featureGroupFeatureDTOS) {
    StreamFeatureGroup streamFeatureGroup = new StreamFeatureGroup();
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
    offlineFeatureGroupController.createHiveTable(featurestore, tableName,
        cachedFeaturegroupController.addHudiSpecFeatures(streamFeatureGroupDTO.getFeatures()),
        project, user, OfflineFeatureGroupController.Formats.HUDI);

    //Persist stream feature group
    return persistStreamFeatureGroupMetadata(streamFeatureGroupDTO.getFeatures());
  }

  public void updateMetadata(Project project, Users user, Featuregroup featuregroup,
    FeaturegroupDTO featuregroupDTO)
    throws FeaturestoreException, SQLException, SchemaException, KafkaException {

    List<FeatureGroupFeatureDTO> previousSchema = getFeaturesDTO(featuregroup, project, user);

    String tableName = featuregroupController.getTblName(featuregroup.getName(), featuregroup.getVersion());

    // verify user input specific for cached feature groups - if any
    List<FeatureGroupFeatureDTO> newFeatures = new ArrayList<>();
    if (featuregroupDTO.getFeatures() != null) {
      cachedFeaturegroupController.verifyPreviousSchemaUnchanged(previousSchema, featuregroupDTO.getFeatures());
      newFeatures = featureGroupInputValidation.verifyAndGetNewFeatures(previousSchema, featuregroupDTO.getFeatures());
      featureGroupInputValidation.verifyVectorDatabaseIndexMappingLimit(project, featuregroupDTO, newFeatures.size());
      featureGroupInputValidation.verifyVectorDatabaseSupportedDataType(newFeatures);

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
        onlineFeaturegroupController.alterFeatureGroupSchema(featuregroup, featuregroupDTO.getFeatures(), project);
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
}
