/*
 *Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.featurestore.tags;

import io.hops.hopsworks.common.dao.featurestore.tag.FeatureStoreTagFacade;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.tag.AttachTagResult;
import io.hops.hopsworks.common.featurestore.tag.FeatureStoreTagControllerIface;
import io.hops.hopsworks.common.featurestore.xattr.dto.FeaturestoreXAttrsConstants;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.hdfs.xattrs.XAttrsController;
import io.hops.hopsworks.common.integrations.EnterpriseStereotype;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeatureStoreTagException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.tag.FeatureStoreTag;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetType;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.json.JSONArray;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

@Stateless
@EnterpriseStereotype
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class FeatureStoreTagsController implements FeatureStoreTagControllerIface {

  @EJB
  private FeaturegroupController featuregroupController;
  @EJB
  private XAttrsController xAttrsController;
  @EJB
  private FeatureStoreTagFacade featureStoreTagFacade;
  @EJB
  private InodeController inodeController;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private DistributedFsService dfs;

  /**
   * Get all tags associated with a featuregroup
   * @param project
   * @param user
   * @param featureStore
   * @param featureGroup
   * @return {@link java.util.Map} containing tags and their associated value
   * @throws FeaturestoreException
   * @throws DatasetException
   * @throws MetadataException
   */
  @Override
  public Map<String, String> getAll(Project project, Users user, Featurestore featureStore, Featuregroup featureGroup)
    throws DatasetException, MetadataException {
  
    String path = featuregroupController.getFeatureGroupLocation(featureGroup);
    String xAttrStr = xAttrsController.getXAttr(project, user, path, FeaturestoreXAttrsConstants.TAGS);
    return FeatureStoreTagsHelper.convertToExternalTags(xAttrStr);
  }
  
  /**
   * Get all tags associated with a featuregroup
   * @param project
   * @param user
   * @param featureStore
   * @param trainingDataset
   * @return {@link java.util.Map} containing tags and their associated value
   * @throws FeaturestoreException
   * @throws DatasetException
   * @throws MetadataException
   */
  @Override
  public Map<String, String> getAll(Project project, Users user, Featurestore featureStore,
                                    TrainingDataset trainingDataset)
    throws DatasetException, MetadataException, FeaturestoreException {
    
    if(!trainingDataset.getTrainingDatasetType().equals(TrainingDatasetType.HOPSFS_TRAINING_DATASET)) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TAG_NOT_ALLOWED,
        Level.FINE, "Tags are only supported for " + TrainingDatasetType.HOPSFS_TRAINING_DATASET);
    }
  
    String path = inodeController.getPath(trainingDataset.getHopsfsTrainingDataset().getInode());
    String xAttrStr = xAttrsController.getXAttr(project, user, path, FeaturestoreXAttrsConstants.TAGS);
    return FeatureStoreTagsHelper.convertToExternalTags(xAttrStr);
  }

  /**
   * Get single tag by name attached to featuregroup
   * @param project
   * @param user
   * @param featureStore
   * @param featureGroup
   * @param name
   * @return {@link java.util.Map} containing tag and its associated value
   * @throws FeaturestoreException
   * @throws DatasetException
   * @throws MetadataException
   */
  @Override
  public Map<String, String> get(Project project, Users user, Featurestore featureStore, Featuregroup featureGroup,
                                 String name)
    throws FeaturestoreException, DatasetException, MetadataException {
  
    String path = featuregroupController.getFeatureGroupLocation(featureGroup);
    return get(project, user, path, name);
  }
  
  /**
   * Get single tag by name attached to featuregroup
   * @param project
   * @param user
   * @param featureStore
   * @param trainingDataset
   * @param name
   * @return {@link java.util.Map} containing tag and its associated value
   * @throws FeaturestoreException
   * @throws DatasetException
   * @throws MetadataException
   */
  @Override
  public Map<String, String> get(Project project, Users user, Featurestore featureStore,
                                 TrainingDataset trainingDataset, String name)
    throws FeaturestoreException, DatasetException, MetadataException {
  
    if(!trainingDataset.getTrainingDatasetType().equals(TrainingDatasetType.HOPSFS_TRAINING_DATASET)) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TAG_NOT_ALLOWED,
        Level.FINE, "Tags are only supported for " + TrainingDatasetType.HOPSFS_TRAINING_DATASET);
    }
  
    String path = inodeController.getPath(trainingDataset.getHopsfsTrainingDataset().getInode());
    return get(project, user, path, name);
  }
  
  private Map<String, String> get(Project project, Users user, String path, String name)
    throws FeaturestoreException, DatasetException, MetadataException {
    String xAttrStr = xAttrsController.getXAttr(project, user, path, FeaturestoreXAttrsConstants.TAGS);
    Map<String, String> tags = FeatureStoreTagsHelper.convertToExternalTags(xAttrStr);
  
    Map<String, String> results = new HashMap<>();
    if (tags != null && tags.containsKey(name)) {
      results.put(name, tags.get(name));
    } else {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TAG_NOT_FOUND, Level.FINE);
    }
    return results;
  }

  /**
   * Creates a new tag or updates an existing one if it already exists
   * @param project
   * @param user
   * @param featureStore
   * @param featureGroup
   * @param name
   * @param value
   * @return attached all attached tags and created or updated
   * @throws FeaturestoreException
   * @throws DatasetException
   * @throws MetadataException
   */
  @Override
  public AttachTagResult upsert(Project project, Users user, Featurestore featureStore, Featuregroup featureGroup,
                                String name, String value)
    throws FeaturestoreException, MetadataException, FeatureStoreTagException {
  
    String path = featuregroupController.getFeatureGroupLocation(featureGroup);
    return upsert(project, user, path, name, value);
  }
  
  /**
   * Creates a new tag or updates an existing one if it already exists
   * @param project
   * @param user
   * @param featureStore
   * @param trainingDataset
   * @param name
   * @param value
   * @return attached all attached tags and created or updated
   * @throws FeaturestoreException
   * @throws DatasetException
   * @throws MetadataException
   */
  @Override
  public AttachTagResult upsert(Project project, Users user, Featurestore featureStore, TrainingDataset trainingDataset,
                                String name, String value)
    throws FeaturestoreException, MetadataException, FeatureStoreTagException {
  
    if(!trainingDataset.getTrainingDatasetType().equals(TrainingDatasetType.HOPSFS_TRAINING_DATASET)) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TAG_NOT_ALLOWED,
        Level.FINE, "Tags are only supported for " + TrainingDatasetType.HOPSFS_TRAINING_DATASET);
    }
  
    String path = inodeController.getPath(trainingDataset.getHopsfsTrainingDataset().getInode());
    return upsert(project, user, path, name, value);
  }
  
  private AttachTagResult upsert(Project project, Users user, String path, String name, String value)
    throws MetadataException, FeaturestoreException, FeatureStoreTagException {
    String hdfsUserName = hdfsUsersController.getHdfsUserName(project, user);
    DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUserName);
    try {
      String xAttrsStr = xAttrsController.getXAttr(path, FeaturestoreXAttrsConstants.TAGS, udfso);
      Map<String, String> tags = FeatureStoreTagsHelper.convertToExternalTags(xAttrsStr);
      boolean created = addNewTag(tags, name, value);
      JSONArray jsonTagsArr = FeatureStoreTagsHelper.convertToInternalTags(tags);
      xAttrsController.addStrXAttr(path, FeaturestoreXAttrsConstants.TAGS, jsonTagsArr.toString(), udfso);
      return new AttachTagResult(tags, created);
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }
  
  /**
   * Creates a new tag or updates an existing one if it already exists
   * @param project
   * @param user
   * @param featureStore
   * @param featureGroup
   * @param newTags
   * @return attached all attached tags and created or updated
   * @throws FeaturestoreException
   * @throws DatasetException
   * @throws MetadataException
   */
  @Override
  public AttachTagResult upsert(Project project, Users user, Featurestore featureStore, Featuregroup featureGroup,
                                Map<String, String> newTags)
    throws FeaturestoreException, MetadataException, FeatureStoreTagException {
  
    String path = featuregroupController.getFeatureGroupLocation(featureGroup);
    return upsert(project, user, path, newTags);
  }
  
  /**
   * Creates a new tag or updates an existing one if it already exists
   * @param project
   * @param user
   * @param featureStore
   * @param trainingDataset
   * @param newTags
   * @return attached all attached tags and created or updated
   * @throws FeaturestoreException
   * @throws DatasetException
   * @throws MetadataException
   */
  @Override
  public AttachTagResult upsert(Project project, Users user, Featurestore featureStore,
                                TrainingDataset trainingDataset, Map<String, String> newTags)
    throws FeaturestoreException, MetadataException, FeatureStoreTagException {
  
    if(!trainingDataset.getTrainingDatasetType().equals(TrainingDatasetType.HOPSFS_TRAINING_DATASET)) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TAG_NOT_ALLOWED,
        Level.FINE, "Tags are only supported for " + TrainingDatasetType.HOPSFS_TRAINING_DATASET);
    }
  
    String path = inodeController.getPath(trainingDataset.getHopsfsTrainingDataset().getInode());
    return upsert(project, user, path, newTags);
  }
  
  private AttachTagResult upsert(Project project, Users user, String path, Map<String, String> newTags)
    throws MetadataException, FeaturestoreException, FeatureStoreTagException {
    String hdfsUserName = hdfsUsersController.getHdfsUserName(project, user);
    DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUserName);
    try {
      String xAttrsStr = xAttrsController.getXAttr(path, FeaturestoreXAttrsConstants.TAGS, udfso);
      Map<String, String> tags = FeatureStoreTagsHelper.convertToExternalTags(xAttrsStr);
    
      boolean created = false;
      for(Map.Entry<String, String> tag : newTags.entrySet()) {
        created = created || addNewTag(tags, tag.getKey(), tag.getValue());
      }
    
      JSONArray jsonTagsArr = FeatureStoreTagsHelper.convertToInternalTags(tags);
      xAttrsController.addStrXAttr(path, FeaturestoreXAttrsConstants.TAGS, jsonTagsArr.toString(), udfso);
      return new AttachTagResult(tags, created);
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }
  
  /**
   *
   * @param existingTags
   * @param name
   * @param value
   * @return whether the tag is created or updated
   * @throws FeaturestoreException
   * @throws FeatureStoreTagException
   */
  private boolean addNewTag(Map<String, String> existingTags, String name, String value)
    throws FeaturestoreException, FeatureStoreTagException {
    
    FeatureStoreTag tagSchema = featureStoreTagFacade.findByName(name);
    if (tagSchema == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TAG_NOT_ALLOWED,
        Level.FINE, name + " is not a valid tag.");
    }
    SchematizedTagHelper.validateTag(tagSchema.getSchema(), value);
    return existingTags.put(name, value) == null;
  }

  /**
   * Delete all tags attached to a featuregroup
   * @param project
   * @param user
   * @param featureStore
   * @param featureGroup
   * @throws FeaturestoreException
   * @throws MetadataException
   * @throws DatasetException
   */
  @Override
  public void deleteAll(Project project, Users user, Featurestore featureStore, Featuregroup featureGroup)
    throws MetadataException, DatasetException {
  
    String path = featuregroupController.getFeatureGroupLocation(featureGroup);
    xAttrsController.removeXAttr(project, user, path, FeaturestoreXAttrsConstants.TAGS);
  }
  
  /**
   * Delete all tags attached to a featuregroup
   * @param project
   * @param user
   * @param featureStore
   * @param trainingDataset
   * @throws FeaturestoreException
   * @throws MetadataException
   * @throws DatasetException
   */
  @Override
  public void deleteAll(Project project, Users user, Featurestore featureStore, TrainingDataset trainingDataset)
    throws MetadataException, DatasetException, FeaturestoreException {
  
    if(!trainingDataset.getTrainingDatasetType().equals(TrainingDatasetType.HOPSFS_TRAINING_DATASET)) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TAG_NOT_ALLOWED,
        Level.FINE, "Tags are only supported for " + TrainingDatasetType.HOPSFS_TRAINING_DATASET);
    }
  
    String path = inodeController.getPath(trainingDataset.getHopsfsTrainingDataset().getInode());
    xAttrsController.removeXAttr(project, user, path, FeaturestoreXAttrsConstants.TAGS);
  }

  /**
   * Delete a single tag attached to a featuregroup
   * @param project
   * @param user
   * @param featureStore
   * @param featureGroup
   * @param name
   * @throws FeaturestoreException
   * @throws MetadataException
   * @throws DatasetException
   */
  @Override
  public void delete(Project project, Users user, Featurestore featureStore, Featuregroup featureGroup, String name)
    throws MetadataException, DatasetException {
  
    String path = featuregroupController.getFeatureGroupLocation(featureGroup);
    delete(project, user, path, name);
  }
  
  /**
   * Delete a single tag attached to a featuregroup
   * @param project
   * @param user
   * @param featureStore
   * @param trainingDataset
   * @param name
   * @throws FeaturestoreException
   * @throws MetadataException
   * @throws DatasetException
   */
  @Override
  public void delete(Project project, Users user, Featurestore featureStore, TrainingDataset trainingDataset,
                     String name)
    throws MetadataException, DatasetException, FeaturestoreException {
  
    if(!trainingDataset.getTrainingDatasetType().equals(TrainingDatasetType.HOPSFS_TRAINING_DATASET)) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TAG_NOT_ALLOWED,
        Level.FINE, "Tags are only supported for " + TrainingDatasetType.HOPSFS_TRAINING_DATASET);
    }
  
    String path = inodeController.getPath(trainingDataset.getHopsfsTrainingDataset().getInode());
    delete(project, user, path, name);
  }
  
  private void delete(Project project, Users user, String path, String name)
    throws DatasetException, MetadataException {
    String xAttrStr = xAttrsController.getXAttr(project, user, path, FeaturestoreXAttrsConstants.TAGS);
    Map<String, String> tags = FeatureStoreTagsHelper.convertToExternalTags(xAttrStr);
    tags.remove(name);
  
    JSONArray jsonTagsArr = FeatureStoreTagsHelper.convertToInternalTags(tags);
    xAttrsController.addXAttr(project, user, path, FeaturestoreXAttrsConstants.TAGS, jsonTagsArr.toString());
  }
}
