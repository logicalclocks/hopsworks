/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.tags;

import io.hops.hopsworks.common.dataset.util.DatasetPath;
import io.hops.hopsworks.common.integrations.EnterpriseStereotype;
import io.hops.hopsworks.common.tags.AttachTagResult;
import io.hops.hopsworks.common.tags.DatasetTagsControllerIface;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.SchematizedTagException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Map;

@Stateless
@EnterpriseStereotype
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class DatasetTagsEEController implements DatasetTagsControllerIface {
  @EJB
  private TagsController tagsController;
  
  /**
   * Get all tags associated with a dataset or file
   * @param user
   * @param datasetPath
   * @return {@link java.util.Map} containing tags and their associated value
   * @throws DatasetException
   * @throws MetadataException
   */
  @Override
  public Map<String, String> getAll(Users user, DatasetPath datasetPath)
    throws DatasetException, MetadataException {
    return tagsController.getAll(datasetPath.getAccessProject(), user, datasetPath.getFullPath().toString());
  }
  
  /**
   * Get single tag by name attached to a dataset or file
   * @param user
   * @param datasetPath
   * @param name
   * @return {@link java.util.Map} containing tag and its associated value
   * @throws DatasetException
   * @throws MetadataException
   * @throws SchematizedTagException
   */
  @Override
  public String get(Users user, DatasetPath datasetPath, String name)
    throws DatasetException, MetadataException, SchematizedTagException {
    String path = datasetPath.getFullPath().toString();
    return tagsController.get(datasetPath.getAccessProject(), user, path, name);
  }
  
  /**
   * Create a new tag or update an existing one if it already exists for a specific dataset or file
   * @param user
   * @param datasetPath
   * @param name
   * @param value
   * @return attached all attached tags and created or updated
   * @throws MetadataException
   * @throws SchematizedTagException
   */
  @Override
  public AttachTagResult upsert(Users user, DatasetPath datasetPath, String name, String value)
    throws MetadataException, SchematizedTagException {
    Project accessProject = datasetPath.getAccessProject();
    String path = datasetPath.getFullPath().toString();
    return tagsController.upsert(accessProject, user, path, name, value);
  }
  
  /**
   * Bulk creates new tags or updates existing ones if they already exist for a specific dataset or file
   * @param user
   * @param datasetPath
   * @param newTags
   * @return attached all attached tags and created or updated
   * @throws MetadataException
   * @throws SchematizedTagException
   */
  @Override
  public AttachTagResult upsert(Users user, DatasetPath datasetPath, Map<String, String> newTags)
    throws MetadataException, SchematizedTagException {
    Project accessProject = datasetPath.getAccessProject();
    String path = datasetPath.getFullPath().toString();
    return tagsController.upsert(accessProject, user, path, newTags);
  }
  
  /**
   * Delete all tags attached to a dataset or file
   * @param user
   * @param datasetPath
   * @throws MetadataException
   * @throws DatasetException
   */
  @Override
  public void deleteAll(Users user, DatasetPath datasetPath)
    throws MetadataException, DatasetException {
    String path = datasetPath.getFullPath().toString();
    tagsController.deleteAll(datasetPath.getAccessProject(), user, path);
  }
  
  /**
   * Delete a single tag attached to a dataset or file
   * @param user
   * @param datasetPath
   * @param name
   * @throws MetadataException
   * @throws DatasetException
   */
  @Override
  public void delete(Users user, DatasetPath datasetPath, String name)
    throws MetadataException, DatasetException {
    String path = datasetPath.getFullPath().toString();
    tagsController.delete(datasetPath.getAccessProject(), user, path, name);
  }
}
