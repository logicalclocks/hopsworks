/*
 *Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.featurestore.tags;

import io.hops.hopsworks.common.commands.featurestore.search.SearchFSCommandLogger;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewController;
import io.hops.hopsworks.common.featurestore.tag.FeatureStoreTagControllerIface;
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
  private TagsController tagsController;
  @EJB
  private SearchFSCommandLogger searchCommandLogger;

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
          throws DatasetException, MetadataException, FeaturestoreException {
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
      throw SchematizedTagException.tagsNotSupported(trainingDataset.getTrainingDatasetType().toString());
    }
  
    return tagsController.getAll(accessProject, user, trainingDataset.getTagPath());
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
          throws DatasetException, MetadataException, SchematizedTagException, FeaturestoreException {
  
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
      throw SchematizedTagException.tagsNotSupported(trainingDataset.getTrainingDatasetType().toString());
    }
  
    return tagsController.get(accessProject, user, trainingDataset.getTagPath(), name);
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
          throws MetadataException, SchematizedTagException, FeaturestoreException {
  
    String path = featuregroupController.getFeatureGroupLocation(featureGroup);
    AttachTagResult result = tagsController.upsert(project, user, path, name, value);
    searchCommandLogger.updateTags(featureGroup);
    return result;
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
    throws MetadataException, SchematizedTagException, FeaturestoreException {
  
    if(!trainingDataset.getTrainingDatasetType().equals(TrainingDatasetType.HOPSFS_TRAINING_DATASET)) {
      throw SchematizedTagException.tagsNotSupported(trainingDataset.getTrainingDatasetType().toString());
    }
  
    String path = trainingDataset.getTagPath();
    AttachTagResult result = tagsController.upsert(project, user, path, name, value);
    searchCommandLogger.updateTags(trainingDataset);
    return result;
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
    AttachTagResult result = tagsController.upsert(project, user, path, name, value);
    searchCommandLogger.updateTags(featureView);
    return result;
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
          throws MetadataException, SchematizedTagException, FeaturestoreException {
  
    String path = featuregroupController.getFeatureGroupLocation(featureGroup);
    AttachTagResult result = tagsController.upsert(project, user, path, newTags);
    searchCommandLogger.updateTags(featureGroup);
    return result;
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
    throws MetadataException, SchematizedTagException, FeaturestoreException {
  
    if(!trainingDataset.getTrainingDatasetType().equals(TrainingDatasetType.HOPSFS_TRAINING_DATASET)) {
      throw SchematizedTagException.tagsNotSupported(trainingDataset.getTrainingDatasetType().toString());
    }
  
    String path = trainingDataset.getTagPath();
    AttachTagResult result = tagsController.upsert(project, user, path, newTags);
    searchCommandLogger.updateTags(trainingDataset);
    return result;
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
    AttachTagResult result = tagsController.upsert(project, user, path, newTags);
    searchCommandLogger.updateTags(featureView);
    return result;
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
    throws MetadataException, DatasetException, FeaturestoreException {
  
    String path = featuregroupController.getFeatureGroupLocation(featureGroup);
    tagsController.deleteAll(accessProject, user, path);
    searchCommandLogger.updateTags(featureGroup);
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
    throws MetadataException, DatasetException, SchematizedTagException, FeaturestoreException {
  
    if(!trainingDataset.getTrainingDatasetType().equals(TrainingDatasetType.HOPSFS_TRAINING_DATASET)) {
      throw new SchematizedTagException(RESTCodes.SchematizedTagErrorCode.TAG_NOT_ALLOWED,
        Level.FINE, "Tags are only supported for " + TrainingDatasetType.HOPSFS_TRAINING_DATASET);
    }
  
    String path = trainingDataset.getTagPath();
    tagsController.deleteAll(accessProject, user, path);
    searchCommandLogger.updateTags(trainingDataset);
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
      throws DatasetException, MetadataException, FeaturestoreException {
    String path = featureViewController.getLocation(featureView);
    tagsController.deleteAll(accessProject, user, path);
    searchCommandLogger.updateTags(featureView);
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
          throws MetadataException, DatasetException, FeaturestoreException {
  
    String path = featuregroupController.getFeatureGroupLocation(featureGroup);
    tagsController.delete(accessProject, user, path, name);
    searchCommandLogger.updateTags(featureGroup);
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
    throws MetadataException, DatasetException, SchematizedTagException, FeaturestoreException {
  
    if(!trainingDataset.getTrainingDatasetType().equals(TrainingDatasetType.HOPSFS_TRAINING_DATASET)) {
      throw new SchematizedTagException(RESTCodes.SchematizedTagErrorCode.TAG_NOT_ALLOWED,
        Level.FINE, "Tags are only supported for " + TrainingDatasetType.HOPSFS_TRAINING_DATASET);
    }
  
    String path = trainingDataset.getTagPath();
    tagsController.delete(accessProject, user, path, name);
    searchCommandLogger.updateTags(trainingDataset);
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
    searchCommandLogger.updateTags(featureView);
  }
}
