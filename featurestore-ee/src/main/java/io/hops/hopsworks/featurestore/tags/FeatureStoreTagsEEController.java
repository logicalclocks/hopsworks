/*
 *Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.featurestore.tags;

import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewController;
import io.hops.hopsworks.common.featurestore.tag.FeatureStoreTagControllerIface;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.integrations.EnterpriseStereotype;
import io.hops.hopsworks.common.tags.AttachTagResult;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.SchematizedTagException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetType;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.tags.TagsController;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Map;
import java.util.logging.Level;

@Stateless
@EnterpriseStereotype
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class FeatureStoreTagsEEController implements FeatureStoreTagControllerIface {

  @EJB
  private FeaturegroupController featuregroupController;
  @EJB
  private FeatureViewController featureViewController;
  @EJB
  private InodeController inodeController;
  @EJB
  private TagsController tagsController;

  /**
   * Get all tags associated with a featuregroup
   * @param accessProject
   * @param user
   * @param featureStore
   * @param featureGroup
   * @return {@link java.util.Map} containing tags and their associated value
   * @throws DatasetException
   * @throws MetadataException
   */
  @Override
  public Map<String, String> getAll(Project accessProject, Users user, Featurestore featureStore,
                                    Featuregroup featureGroup)
    throws DatasetException, MetadataException {
    String path = featuregroupController.getFeatureGroupLocation(featureGroup);
    return tagsController.getAll(accessProject, user, path);
  }
  
  /**
   * Get all tags associated with a trainingDataset
   * @param accessProject
   * @param user
   * @param featureStore
   * @param trainingDataset
   * @return {@link java.util.Map} containing tags and their associated value
   * @throws SchematizedTagException
   * @throws DatasetException
   * @throws MetadataException
   */
  @Override
  public Map<String, String> getAll(Project accessProject, Users user, Featurestore featureStore,
                                    TrainingDataset trainingDataset)
    throws DatasetException, MetadataException, SchematizedTagException {
    
    if(!trainingDataset.getTrainingDatasetType().equals(TrainingDatasetType.HOPSFS_TRAINING_DATASET)) {
      throw new SchematizedTagException(RESTCodes.SchematizedTagErrorCode.TAG_NOT_ALLOWED,
        Level.FINE, "Tags are only supported for " + TrainingDatasetType.HOPSFS_TRAINING_DATASET);
    }
  
    String path = inodeController.getPath(trainingDataset.getHopsfsTrainingDataset().getInode());
    return tagsController.getAll(accessProject, user, path);
  }

  /**
   * Get all tags associated with a featureView
   * @param accessProject
   * @param user
   * @param featureStore
   * @param featureView
   * @return {@link java.util.Map} containing tags and their associated value
   * @throws SchematizedTagException
   * @throws DatasetException
   * @throws MetadataException
   */
  @Override
  public Map<String, String> getAll(Project accessProject, Users user, Featurestore featureStore,
                                    FeatureView featureView)
      throws DatasetException, MetadataException, SchematizedTagException, FeaturestoreException {
    String path = featureViewController.getLocation(featureView);
    return tagsController.getAll(accessProject, user, path);
  }

  /**
   * Get single tag by name attached to featuregroup
   * @param accessProject
   * @param user
   * @param featureStore
   * @param featureGroup
   * @param name
   * @return {@link java.util.Map} containing tag and its associated value
   * @throws SchematizedTagException
   * @throws DatasetException
   * @throws MetadataException
   */
  @Override
  public String get(Project accessProject, Users user, Featurestore featureStore, Featuregroup featureGroup,
                    String name)
    throws DatasetException, MetadataException, SchematizedTagException {
  
    String path = featuregroupController.getFeatureGroupLocation(featureGroup);
    return tagsController.get(accessProject, user, path, name);
  }
  
  /**
   * Get single tag by name attached to trainingDataset
   * @param accessProject
   * @param user
   * @param featureStore
   * @param trainingDataset
   * @param name
   * @return {@link java.util.Map} containing tag and its associated value
   * @throws SchematizedTagException
   * @throws DatasetException
   * @throws MetadataException
   */
  @Override
  public String get(Project accessProject, Users user, Featurestore featureStore, TrainingDataset trainingDataset,
                    String name)
    throws DatasetException, MetadataException, SchematizedTagException {
  
    if(!trainingDataset.getTrainingDatasetType().equals(TrainingDatasetType.HOPSFS_TRAINING_DATASET)) {
      throw new SchematizedTagException(RESTCodes.SchematizedTagErrorCode.TAG_NOT_ALLOWED,
        Level.FINE, "Tags are only supported for " + TrainingDatasetType.HOPSFS_TRAINING_DATASET);
    }
  
    String path = inodeController.getPath(trainingDataset.getHopsfsTrainingDataset().getInode());
    return tagsController.get(accessProject, user, path, name);
  }

  /**
   * Get single tag by name attached to featureView
   * @param accessProject
   * @param user
   * @param featureStore
   * @param featureView
   * @param name
   * @return {@link java.util.Map} containing tag and its associated value
   * @throws SchematizedTagException
   * @throws DatasetException
   * @throws MetadataException
   */
  @Override
  public String get(Project accessProject, Users user, Featurestore featureStore, FeatureView featureView, String name)
      throws DatasetException, MetadataException, SchematizedTagException, FeaturestoreException {
    String path = featureViewController.getLocation(featureView);
    return tagsController.get(accessProject, user, path, name);
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
   * @throws SchematizedTagException
   * @throws MetadataException
   */
  @Override
  public AttachTagResult upsert(Project project, Users user, Featurestore featureStore, Featuregroup featureGroup,
                                String name, String value)
    throws MetadataException, SchematizedTagException {
  
    String path = featuregroupController.getFeatureGroupLocation(featureGroup);
    return tagsController.upsert(project, user, path, name, value);
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
   * @throws SchematizedTagException
   * @throws MetadataException
   */
  @Override
  public AttachTagResult upsert(Project project, Users user, Featurestore featureStore, TrainingDataset trainingDataset,
                                String name, String value)
    throws MetadataException, SchematizedTagException {
  
    if(!trainingDataset.getTrainingDatasetType().equals(TrainingDatasetType.HOPSFS_TRAINING_DATASET)) {
      throw new SchematizedTagException(RESTCodes.SchematizedTagErrorCode.TAG_NOT_ALLOWED,
        Level.FINE, "Tags are only supported for " + TrainingDatasetType.HOPSFS_TRAINING_DATASET);
    }
  
    String path = inodeController.getPath(trainingDataset.getHopsfsTrainingDataset().getInode());
    return tagsController.upsert(project, user, path, name, value);
  }

  /**
   * Creates a new tag or updates an existing one if it already exists
   * @param project
   * @param user
   * @param featureStore
   * @param featureView
   * @param name
   * @param value
   * @return attached all attached tags and created or updated
   * @throws SchematizedTagException
   * @throws MetadataException
   */
  @Override
  public AttachTagResult upsert(Project project, Users user, Featurestore featureStore, FeatureView featureView,
                                String name, String value)
      throws MetadataException, SchematizedTagException, FeaturestoreException {
    String path = featureViewController.getLocation(featureView);
    return tagsController.upsert(project, user, path, name, value);
  }
  
  /**
   * Creates a new tag or updates an existing one if it already exists
   * @param project
   * @param user
   * @param featureStore
   * @param featureGroup
   * @param newTags
   * @return attached all attached tags and created or updated
   * @throws SchematizedTagException
   * @throws MetadataException
   */
  @Override
  public AttachTagResult upsert(Project project, Users user, Featurestore featureStore, Featuregroup featureGroup,
                                Map<String, String> newTags)
    throws MetadataException, SchematizedTagException {
  
    String path = featuregroupController.getFeatureGroupLocation(featureGroup);
    return tagsController.upsert(project, user, path, newTags);
  }
  
  /**
   * Creates a new tag or updates an existing one if it already exists
   * @param project
   * @param user
   * @param featureStore
   * @param trainingDataset
   * @param newTags
   * @return attached all attached tags and created or updated
   * @throws SchematizedTagException
   * @throws MetadataException
   */
  @Override
  public AttachTagResult upsert(Project project, Users user, Featurestore featureStore,
                                TrainingDataset trainingDataset, Map<String, String> newTags)
    throws MetadataException, SchematizedTagException {
  
    if(!trainingDataset.getTrainingDatasetType().equals(TrainingDatasetType.HOPSFS_TRAINING_DATASET)) {
      throw new SchematizedTagException(RESTCodes.SchematizedTagErrorCode.TAG_NOT_ALLOWED,
        Level.FINE, "Tags are only supported for " + TrainingDatasetType.HOPSFS_TRAINING_DATASET);
    }
  
    String path = inodeController.getPath(trainingDataset.getHopsfsTrainingDataset().getInode());
    return tagsController.upsert(project, user, path, newTags);
  }

  /**
   * Creates a new tag or updates an existing one if it already exists
   * @param project
   * @param user
   * @param featureStore
   * @param featureView
   * @param newTags
   * @return attached all attached tags and created or updated
   * @throws SchematizedTagException
   * @throws MetadataException
   */
  @Override
  public AttachTagResult upsert(Project project, Users user, Featurestore featureStore, FeatureView featureView,
                                Map<String, String> newTags)
      throws MetadataException, SchematizedTagException, FeaturestoreException {
    String path = featureViewController.getLocation(featureView);
    return tagsController.upsert(project, user, path, newTags);
  }

  /**
   * Delete all tags attached to a featuregroup
   * @param accessProject
   * @param user
   * @param featureStore
   * @param featureGroup
   * @throws MetadataException
   * @throws DatasetException
   */
  @Override
  public void deleteAll(Project accessProject, Users user, Featurestore featureStore, Featuregroup featureGroup)
    throws MetadataException, DatasetException {
  
    String path = featuregroupController.getFeatureGroupLocation(featureGroup);
    tagsController.deleteAll(accessProject, user, path);
  }
  
  /**
   * Delete all tags attached to a trainingDataset
   * @param accessProject
   * @param user
   * @param featureStore
   * @param trainingDataset
   * @throws MetadataException
   * @throws DatasetException
   * @throws SchematizedTagException
   */
  @Override
  public void deleteAll(Project accessProject, Users user, Featurestore featureStore, TrainingDataset trainingDataset)
    throws MetadataException, DatasetException, SchematizedTagException {
  
    if(!trainingDataset.getTrainingDatasetType().equals(TrainingDatasetType.HOPSFS_TRAINING_DATASET)) {
      throw new SchematizedTagException(RESTCodes.SchematizedTagErrorCode.TAG_NOT_ALLOWED,
        Level.FINE, "Tags are only supported for " + TrainingDatasetType.HOPSFS_TRAINING_DATASET);
    }
  
    String path = inodeController.getPath(trainingDataset.getHopsfsTrainingDataset().getInode());
    tagsController.deleteAll(accessProject, user, path);
  }

  /**
   * Delete all tags attached to a featureView
   * @param accessProject
   * @param user
   * @param featureStore
   * @param featureView
   * @throws MetadataException
   * @throws DatasetException
   * @throws SchematizedTagException
   */
  @Override
  public void deleteAll(Project accessProject, Users user, Featurestore featureStore, FeatureView featureView)
      throws DatasetException, MetadataException, SchematizedTagException, FeaturestoreException {
    String path = featureViewController.getLocation(featureView);
    tagsController.deleteAll(accessProject, user, path);
  }

  /**
   * Delete a single tag attached to a featuregroup
   * @param accessProject
   * @param user
   * @param featureStore
   * @param featureGroup
   * @param name
   * @throws MetadataException
   * @throws DatasetException
   */
  @Override
  public void delete(Project accessProject, Users user, Featurestore featureStore, Featuregroup featureGroup,
                     String name)
    throws MetadataException, DatasetException {
  
    String path = featuregroupController.getFeatureGroupLocation(featureGroup);
    tagsController.delete(accessProject, user, path, name);
  }
  
  /**
   * Delete a single tag attached to a trainingDataset
   * @param accessProject
   * @param user
   * @param featureStore
   * @param trainingDataset
   * @param name
   * @throws MetadataException
   * @throws DatasetException
   * @throws SchematizedTagException
   */
  @Override
  public void delete(Project accessProject, Users user, Featurestore featureStore, TrainingDataset trainingDataset,
                     String name)
    throws MetadataException, DatasetException, SchematizedTagException {
  
    if(!trainingDataset.getTrainingDatasetType().equals(TrainingDatasetType.HOPSFS_TRAINING_DATASET)) {
      throw new SchematizedTagException(RESTCodes.SchematizedTagErrorCode.TAG_NOT_ALLOWED,
        Level.FINE, "Tags are only supported for " + TrainingDatasetType.HOPSFS_TRAINING_DATASET);
    }
  
    String path = inodeController.getPath(trainingDataset.getHopsfsTrainingDataset().getInode());
    tagsController.delete(accessProject, user, path, name);
  }

  /**
   * Delete a single tag attached to a featureView
   * @param accessProject
   * @param user
   * @param featureStore
   * @param featureView
   * @param name
   * @throws MetadataException
   * @throws DatasetException
   * @throws SchematizedTagException
   */
  @Override
  public void delete(Project accessProject, Users user, Featurestore featureStore, FeatureView featureView, String name)
      throws DatasetException, MetadataException, SchematizedTagException, FeaturestoreException {
    String path = featureViewController.getLocation(featureView);
    tagsController.delete(accessProject, user, path, name);
  }
}
