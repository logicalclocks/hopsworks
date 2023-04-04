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

package io.hops.hopsworks.common.featurestore.featuregroup.ondemand;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.featurestore.FeaturestoreFacade;
import io.hops.hopsworks.common.featurestore.activity.FeaturestoreActivityFacade;
import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeatureGroupInputValidation;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupFacade;
import io.hops.hopsworks.common.featurestore.featuregroup.online.OnlineFeaturegroupController;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreConnectorFacade;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
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
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.ondemand.OnDemandFeature;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.ondemand.OnDemandFeaturegroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.ondemand.OnDemandOption;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnectorType;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.fs.Path;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Class controlling the interaction with the on_demand_feature_group table and required business logic
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class OnDemandFeaturegroupController {
  @EJB
  private OnDemandFeaturegroupFacade onDemandFeaturegroupFacade;
  @EJB
  private FeaturestoreConnectorFacade featurestoreConnectorFacade;
  @EJB
  private FeaturestoreFacade featurestoreFacade;
  @EJB
  private DistributedFsService distributedFsService;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private InodeController inodeController;
  @EJB
  private OnlineFeaturegroupController onlineFeatureGroupController;
  @EJB
  private FeatureGroupInputValidation featureGroupInputValidation;
  @EJB
  private FeaturegroupFacade featureGroupFacade;
  @EJB
  private FeaturestoreActivityFacade fsActivityFacade;

  /**
   * Persists an on demand feature group
   *
   *
   * @param project
   * @param user
   * @param featurestore
   * @param onDemandFeaturegroupDTO the user input data to use when creating the feature group
   * @return the created entity
   */
  public OnDemandFeaturegroup createOnDemandFeaturegroup(Featurestore featurestore,
                                                         OnDemandFeaturegroupDTO onDemandFeaturegroupDTO,
                                                         Project project, Users user)
      throws FeaturestoreException {
    //Verify User Input specific for on demand feature groups
    FeaturestoreConnector connector = getStorageConnector(onDemandFeaturegroupDTO.getStorageConnector().getId());

    // We allow users to read an entire S3 bucket for instance and they don't need to provide us with a query
    // However if you are running against a JDBC database, you need to provide a query
    boolean isJDBCType = (connector.getConnectorType() == FeaturestoreConnectorType.JDBC ||
        connector.getConnectorType() == FeaturestoreConnectorType.REDSHIFT ||
        connector.getConnectorType() == FeaturestoreConnectorType.SNOWFLAKE ||
        connector.getConnectorType() == FeaturestoreConnectorType.BIGQUERY);
    if (connector.getConnectorType() == FeaturestoreConnectorType.KAFKA) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_CREATE_ON_DEMAND_FEATUREGROUP,
        Level.FINE, connector.getConnectorType() + " storage connectors are not supported as source for on demand " +
        "feature groups");
    } else if (Strings.isNullOrEmpty(onDemandFeaturegroupDTO.getQuery()) && isJDBCType) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.INVALID_SQL_QUERY, Level.FINE,
          "SQL Query cannot be empty");
    } else if (!Strings.isNullOrEmpty(onDemandFeaturegroupDTO.getQuery()) && !isJDBCType) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.INVALID_SQL_QUERY, Level.FINE,
          "SQL query not supported when specifying " + connector.getConnectorType() + " storage connectors");
    } else if (onDemandFeaturegroupDTO.getDataFormat() == null && !isJDBCType) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_ON_DEMAND_DATA_FORMAT, Level.FINE,
          "Data format required when specifying " + connector.getConnectorType() + " storage connectors");
    }

    //Persist on-demand featuregroup
    OnDemandFeaturegroup onDemandFeaturegroup = new OnDemandFeaturegroup();
    onDemandFeaturegroup.setDescription(onDemandFeaturegroupDTO.getDescription());
    onDemandFeaturegroup.setFeaturestoreConnector(connector);
    onDemandFeaturegroup.setQuery(onDemandFeaturegroupDTO.getQuery());
    onDemandFeaturegroup.setFeatures(convertOnDemandFeatures(onDemandFeaturegroupDTO, onDemandFeaturegroup));
    onDemandFeaturegroup.setInode(createFile(project, user, featurestore, onDemandFeaturegroupDTO));
    onDemandFeaturegroup.setDataFormat(onDemandFeaturegroupDTO.getDataFormat());
    onDemandFeaturegroup.setPath(onDemandFeaturegroupDTO.getPath());

    if (onDemandFeaturegroupDTO.getOptions() != null) {
      onDemandFeaturegroup.setOptions(
          onDemandFeaturegroupDTO.getOptions().stream()
              .map(o -> new OnDemandOption(onDemandFeaturegroup, o.getName(), o.getValue()))
              .collect(Collectors.toList()));
    }


    onDemandFeaturegroupFacade.persist(onDemandFeaturegroup);
    return onDemandFeaturegroup;
  }

  public OnDemandFeaturegroupDTO convertOnDemandFeatureGroupToDTO(String featureStoreName, Featuregroup featureGroup,
    FeaturestoreStorageConnectorDTO storageConnectorDTO) throws FeaturestoreException {
    List<FeatureGroupFeatureDTO> features = getFeaturesDTO(featureGroup);
    String onlineTopicName = onlineFeatureGroupController.onlineFeatureGroupTopicName(
      featureGroup.getFeaturestore().getProject().getId(),
      featureGroup.getId(), Utils.getFeaturegroupName(featureGroup));
    return new OnDemandFeaturegroupDTO(featureStoreName, featureGroup, storageConnectorDTO, features, onlineTopicName);
  }

  private List<FeatureGroupFeatureDTO> getFeaturesDTO(Featuregroup featureGroup)
    throws FeaturestoreException {
    List<FeatureGroupFeatureDTO> features = featureGroup.getOnDemandFeaturegroup().getFeatures().stream()
      .sorted(Comparator.comparing(OnDemandFeature::getIdx))
      .map(fgFeature ->
        new FeatureGroupFeatureDTO(fgFeature.getName(), fgFeature.getType(), fgFeature.getDescription(),
          fgFeature.getPrimary(), fgFeature.getDefaultValue(), featureGroup.getId())).collect(Collectors.toList());
    return onlineFeatureGroupController.getFeaturegroupFeatures(featureGroup, features);
  }

  /**
   * Updates metadata of an on demand feature group in the feature store
   *
   * @param featuregroup the on-demand feature group to update
   * @param onDemandFeaturegroupDTO the metadata DTO
   */
  public void updateOnDemandFeaturegroupMetadata(Project project, Users user, Featuregroup featuregroup,
                                                 OnDemandFeaturegroupDTO onDemandFeaturegroupDTO)
    throws FeaturestoreException, SchemaException, SQLException, KafkaException {
    OnDemandFeaturegroup onDemandFeaturegroup = featuregroup.getOnDemandFeaturegroup();
    List<FeatureGroupFeatureDTO> previousSchema = getFeaturesDTO(featuregroup);
    
    // verify previous schema unchanged and valid
    verifySchemaUnchangedAndValid(onDemandFeaturegroup.getFeatures(), onDemandFeaturegroupDTO.getFeatures());
    
    List<FeatureGroupFeatureDTO> newFeatures = featureGroupInputValidation.verifyAndGetNewFeatures(previousSchema,
      onDemandFeaturegroupDTO.getFeatures());
  
    // Update metadata in entity
    if (onDemandFeaturegroupDTO.getDescription() != null) {
      onDemandFeaturegroup.setDescription(onDemandFeaturegroupDTO.getDescription());
    }
    
    // append new features and update existing ones
    updateOnDemandFeatures(onDemandFeaturegroup, onDemandFeaturegroupDTO.getFeatures());
  
    // alter table for new additional features
    if (!newFeatures.isEmpty()) {
      if (featuregroup.isOnlineEnabled()) {
        onlineFeatureGroupController.alterOnlineFeatureGroupSchema(
          featuregroup, newFeatures, onDemandFeaturegroupDTO.getFeatures(), project, user);
      }
    
      // Log schema change
      String newFeaturesStr = "New features: " + newFeatures.stream().map(FeatureGroupFeatureDTO::getName)
        .collect(Collectors.joining(","));
      fsActivityFacade.logMetadataActivity(user, featuregroup, FeaturestoreActivityMeta.FG_ALTERED, newFeaturesStr);
    }
    // finally merge in database
    onDemandFeaturegroupFacade.updateMetadata(onDemandFeaturegroup);
  
  
  }
  
  private void updateOnDemandFeatures(OnDemandFeaturegroup onDemandFeaturegroup,
                                      List<FeatureGroupFeatureDTO> featureGroupFeatureDTOs) {
    for (FeatureGroupFeatureDTO feature : featureGroupFeatureDTOs) {
      Optional<OnDemandFeature> previousOnDemandFeature = getOnDemandFeature(onDemandFeaturegroup.getFeatures(),
        feature.getName());
  
      if (previousOnDemandFeature.isPresent()) {
        previousOnDemandFeature.get().setDescription(feature.getDescription());
      } else {
        onDemandFeaturegroup.getFeatures().add(new OnDemandFeature(onDemandFeaturegroup, feature.getName(),
          feature.getType(), feature.getDescription(), feature.getPrimary(),
          onDemandFeaturegroup.getFeatures().size(), feature.getDefaultValue()));
      }
    }
  }
  
  private Optional<OnDemandFeature> getOnDemandFeature(Collection<OnDemandFeature> onDemandFeatures,
                                                       String featureName) {
    return onDemandFeatures.stream().filter(feature -> feature.getName().equalsIgnoreCase(featureName)).findAny();
  }
  
  public void verifySchemaUnchangedAndValid(Collection<OnDemandFeature> previousSchema,
    List<FeatureGroupFeatureDTO> newSchema) throws FeaturestoreException {
    for (OnDemandFeature feature : previousSchema) {
      FeatureGroupFeatureDTO newFeature =
        newSchema.stream().filter(newFeat -> feature.getName().equals(newFeat.getName())).findAny().orElseThrow(() ->
          new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_UPDATE, Level.FINE,
            "Feature " + feature.getName() + " was not found in new schema. It is only possible to append features."));
      if ( newFeature.getPrimary() != feature.getPrimary() ||
          !newFeature.getType().equalsIgnoreCase(feature.getType())) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_UPDATE, Level.FINE,
          "Primary key or type information of feature " + feature.getName() + " changed. Primary key" +
            " and type cannot be changed when updating features.");
      }
      if (newFeature.getDefaultValue() != null) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_UPDATE, Level.FINE,
          "Default values for features appended to external feature groups are not supported. Feature: " +
            feature.getName() + ", provided default value: " + newFeature.getDefaultValue() + " must be null instead.");
      }
    }
  }

  /**
   * Removes a on demand feature group
   * @return the deleted entity
   */
  public void removeOnDemandFeaturegroup(Featurestore featurestore, Featuregroup featuregroup,
                                         Project project, Users user) throws FeaturestoreException {
    String username = hdfsUsersController.getHdfsUserName(project, user);
    DistributedFileSystemOps udfso = null;

    // this is here for old feature groups that don't have a file
    onDemandFeaturegroupFacade.remove(featuregroup.getOnDemandFeaturegroup());

    try {
      udfso = distributedFsService.getDfsOps(username);
      udfso.rm(getFilePath(featurestore, featuregroup.getName(), featuregroup.getVersion()), false);
    } catch (IOException | URISyntaxException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_DELETE_ON_DEMAND_FEATUREGROUP,
          Level.SEVERE, "Error removing the placeholder file", e.getMessage(), e);
    } finally {
      distributedFsService.closeDfsClient(udfso);
    }
  }

  private List<OnDemandFeature> convertOnDemandFeatures(OnDemandFeaturegroupDTO onDemandFeaturegroupDTO,
                                                        OnDemandFeaturegroup onDemandFeaturegroup) {
    if (onDemandFeaturegroupDTO.getFeatures().isEmpty()) {
      throw new IllegalArgumentException("No features were provided for on demand feature group");
    }
    int i = 0;
    List<OnDemandFeature> features = new ArrayList<>();
    for (FeatureGroupFeatureDTO f : onDemandFeaturegroupDTO.getFeatures()) {
      features.add(new OnDemandFeature(onDemandFeaturegroup, f.getName(), f.getType(), f.getDescription(),
        f.getPrimary(), i++, f.getDefaultValue()));
    }
    return features;
  }

  private Inode createFile(Project project, Users user, Featurestore featurestore,
                           OnDemandFeaturegroupDTO onDemandFeaturegroupDTO) throws FeaturestoreException {
    String username = hdfsUsersController.getHdfsUserName(project, user);

    Path path = null;
    DistributedFileSystemOps udfso = null;
    try {
      path = getFilePath(featurestore, onDemandFeaturegroupDTO.getName(), onDemandFeaturegroupDTO.getVersion());

      udfso = distributedFsService.getDfsOps(username);
      udfso.touchz(path);
    } catch (IOException | URISyntaxException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_CREATE_ON_DEMAND_FEATUREGROUP,
          Level.SEVERE, "Error creating the placeholder file", e.getMessage(), e);
    } finally {
      distributedFsService.closeDfsClient(udfso);
    }

    return inodeController.getInodeAtPath(path.toString());
  }

  private Path getFilePath(Featurestore featurestore, String name, Integer version) throws URISyntaxException {
    // for compatibility reason here we need to remove the authority
    return new Path (new URI(featurestoreFacade.getHiveDbHdfsPath(featurestore.getHiveDbId())).getPath(),
        name + "_" + version);
  }

  private FeaturestoreConnector getStorageConnector(Integer connectorId) throws FeaturestoreException {
    if(connectorId == null){
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.CONNECTOR_ID_NOT_PROVIDED.getMessage());
    }

    return featurestoreConnectorFacade.findById(connectorId)
        .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.CONNECTOR_NOT_FOUND, Level.FINE,
        "Connector with id: " + connectorId + " was not found"));
  }

  /**
   * Update an external feature group that currently does not support online feature serving, to support it.
   *
   * @param featurestore the featurestore where the featuregroup resides
   * @param featuregroup the featuregroup entity to update
   * @param user the user making the request
   * @return a DTO of the updated featuregroup
   * @throws FeaturestoreException
   * @throws SQLException
   */
  public void enableFeatureGroupOnline(Featurestore featurestore, Featuregroup featuregroup,
    Project project, Users user)
    throws FeaturestoreException, SQLException, ServiceException, KafkaException, SchemaException, ProjectException,
    UserException, IOException, HopsSecurityException {
    List<FeatureGroupFeatureDTO> features = getFeaturesDTO(featuregroup);
    if(!featuregroup.isOnlineEnabled()) {
      onlineFeatureGroupController.setupOnlineFeatureGroup(featurestore, featuregroup, features, project, user);
    }
    featuregroup.setOnlineEnabled(true);
    featureGroupFacade.updateFeaturegroupMetadata(featuregroup);
  }
}
