/*
 *Copyright (C) 2022, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.tags;

import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.integrations.EnterpriseStereotype;
import io.hops.hopsworks.common.models.tags.ModelTagControllerIface;
import io.hops.hopsworks.common.tags.AttachTagResult;
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
public class ModelTagsEEController implements ModelTagControllerIface {

  @EJB
  private FeaturegroupController featuregroupController;
  @EJB
  private InodeController inodeController;
  @EJB
  private TagsController tagsController;

  /**
   * Get single tag by name attached to a model.
   * @param accessProject
   * @param user
   * @param path
   * @param name
   * @return
   * @throws DatasetException
   * @throws MetadataException
   * @throws SchematizedTagException
   */
  @Override
  public String get(Project accessProject, Users user, String path, String name)
      throws DatasetException, MetadataException, SchematizedTagException {
    return tagsController.get(accessProject, user, path, name);
  }

  /**
   * Get all tags associated with a model.
   * @param accessProject
   * @param user
   * @param path
   * @return
   * @throws DatasetException
   * @throws MetadataException
   */
  @Override
  public Map<String, String> getAll(Project accessProject, Users user, String path)
      throws DatasetException, MetadataException {
    return tagsController.getAll(accessProject, user, path);
  }

  /**
   * Creates a new tag or updates an existing one if it already exists.
   * @param accessProject
   * @param user
   * @param path
   * @param name
   * @param value
   * @return
   * @throws MetadataException
   * @throws SchematizedTagException
   */
  @Override
  public AttachTagResult upsert(Project accessProject, Users user, String path,
                                String name, String value)
      throws MetadataException, SchematizedTagException {
    return tagsController.upsert(accessProject, user, path, name, value);
  }

  /**
   * Bulk creation of new tag and updates existing tags.
   * @param accessProject
   * @param user
   * @param path
   * @param newTags
   * @return
   * @throws MetadataException
   * @throws SchematizedTagException
   */
  @Override
  public AttachTagResult upsertAll(Project accessProject, Users user, String path,
                                Map<String, String> newTags)
      throws MetadataException, SchematizedTagException {
    return tagsController.upsert(accessProject, user, path, newTags);
  }

  /**
   * Delete tag by name that is attached to a model.
   * @param accessProject
   * @param user
   * @param path
   * @param name
   * @throws MetadataException
   * @throws DatasetException
   */
  @Override
  public void delete(Project accessProject, Users user, String path, String name)
      throws MetadataException, DatasetException {
    tagsController.delete(accessProject, user, path, name);
  }

  /**
   * Delete all tags attached to a model.
   * @param accessProject
   * @param user
   * @param path
   * @throws MetadataException
   * @throws DatasetException
   */
  @Override
  public void deleteAll(Project accessProject, Users user, String path)
      throws MetadataException, DatasetException {
    tagsController.deleteAll(accessProject, user, path);
  }
}
