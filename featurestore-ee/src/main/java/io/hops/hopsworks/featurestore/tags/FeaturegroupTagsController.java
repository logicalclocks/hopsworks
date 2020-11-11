/*
 *Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.featurestore.tags;

import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.tag.FeatureStoreTagController;
import io.hops.hopsworks.common.featurestore.tag.FeaturegroupTagControllerIface;
import io.hops.hopsworks.common.featurestore.xattr.dto.FeaturestoreXAttrsConstants;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.hdfs.xattrs.XAttrsController;
import io.hops.hopsworks.common.integrations.EnterpriseStereotype;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.FeaturegroupType;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.json.JSONArray;
import org.json.JSONObject;

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
public class FeaturegroupTagsController implements FeaturegroupTagControllerIface {

  @EJB
  private FeaturegroupController featuregroupController;
  @EJB
  private XAttrsController xAttrsController;
  @EJB
  private FeatureStoreTagController featureStoreTagController;
  @EJB
  private InodeController inodeController;

  /**
   * Get all tags associated with a featuregroup
   * @param project
   * @param user
   * @param featurestore
   * @param featuregroupId
   * @return {@link java.util.Map} containing tags and their associated value
   * @throws FeaturestoreException
   * @throws DatasetException
   * @throws MetadataException
   */
  public Map<String, String> getAll(Project project, Users user, Featurestore featurestore, int featuregroupId)
      throws FeaturestoreException, DatasetException, MetadataException {

    Featuregroup featuregroup = featuregroupController.getFeaturegroupById(featurestore, featuregroupId);
    String path = getFeaturegroupLocation(featuregroup);
    Map<String, String> xattrsMap = xAttrsController.getXAttrs(project, user, path, FeaturestoreXAttrsConstants.TAGS);
    return featureStoreTagController.convertToExternalTags(xattrsMap.get(FeaturestoreXAttrsConstants.TAGS));
  }

  /**
   * Get single tag by name attached to featuregroup
   * @param project
   * @param user
   * @param featurestore
   * @param featuregroupId
   * @param tagName
   * @return {@link java.util.Map} containing tag and its associated value
   * @throws FeaturestoreException
   * @throws DatasetException
   * @throws MetadataException
   */
  public Map<String, String> getSingle(Project project, Users user, Featurestore featurestore, int featuregroupId,
                                       String tagName)
      throws FeaturestoreException, DatasetException, MetadataException {

    Featuregroup featuregroup = featuregroupController.getFeaturegroupById(featurestore, featuregroupId);
    String path = getFeaturegroupLocation(featuregroup);
    Map<String, String> xattrsMap = xAttrsController.getXAttrs(project, user, path, FeaturestoreXAttrsConstants.TAGS);
    Map<String, String> tags =
        featureStoreTagController.convertToExternalTags(xattrsMap.get(FeaturestoreXAttrsConstants.TAGS));

    Map<String, String> results = new HashMap<>();
    if (tags != null && tags.containsKey(tagName)) {
      results.put(tagName, tags.get(tagName));
    } else {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TAG_NOT_FOUND, Level.FINE);
    }

    return results;
  }

  /**
   * Creates a new tag or updates an existing one if it already exists
   * @param project
   * @param user
   * @param featurestore
   * @param featuregroupId
   * @param tag
   * @param value
   * @return true if tag was created, false if updated
   * @throws FeaturestoreException
   * @throws DatasetException
   * @throws MetadataException
   */
  public boolean createOrUpdateSingleTag(Project project, Users user, Featurestore featurestore,
                                int featuregroupId, String tag, String value)
      throws FeaturestoreException, DatasetException, MetadataException {

    Featuregroup featuregroup = featuregroupController.getFeaturegroupById(featurestore, featuregroupId);
    String path = getFeaturegroupLocation(featuregroup);

    String tagsJson = new JSONObject().put(tag, value).toString();
    featureStoreTagController.validateTags(tagsJson);

    Map<String, String> xattrsMap = xAttrsController.getXAttrs(project, user, path, FeaturestoreXAttrsConstants.TAGS);
    Map<String, String> tags =
        featureStoreTagController.convertToExternalTags(xattrsMap.get(FeaturestoreXAttrsConstants.TAGS));
    tags.put(tag, value != null ? value : "");

    JSONArray jsonTagsArr = featureStoreTagController.convertToInternalTags(tags);
    return xAttrsController.addXAttr(project, user, path, FeaturestoreXAttrsConstants.TAGS, jsonTagsArr.toString());
  }

  /**
   * Delete all tags attached to a featuregroup
   * @param project
   * @param user
   * @param featurestore
   * @param featuregroupId
   * @throws FeaturestoreException
   * @throws MetadataException
   * @throws DatasetException
   */
  public void deleteAll(Project project, Users user, Featurestore featurestore, int featuregroupId)
      throws FeaturestoreException, MetadataException, DatasetException {
    Featuregroup featuregroup = featuregroupController.getFeaturegroupById(featurestore, featuregroupId);
    String path = getFeaturegroupLocation(featuregroup);
    xAttrsController.removeXAttr(project, user, path, FeaturestoreXAttrsConstants.TAGS);
  }

  /**
   * Delete a single tag attached to a featuregroup
   * @param project
   * @param user
   * @param featurestore
   * @param featuregroupId
   * @param tagName
   * @throws FeaturestoreException
   * @throws MetadataException
   * @throws DatasetException
   */
  public void deleteSingle(Project project, Users user, Featurestore featurestore, int featuregroupId, String tagName)
      throws FeaturestoreException, MetadataException, DatasetException {

    Featuregroup featuregroup = featuregroupController.getFeaturegroupById(featurestore, featuregroupId);
    String path = getFeaturegroupLocation(featuregroup);

    Map<String, String> xattrsMap = xAttrsController.getXAttrs(project, user, path, FeaturestoreXAttrsConstants.TAGS);
    Map<String, String> tags =
        featureStoreTagController.convertToExternalTags(xattrsMap.get(FeaturestoreXAttrsConstants.TAGS));
    tags.remove(tagName);

    JSONArray jsonTagsArr = featureStoreTagController.convertToInternalTags(tags);
    xAttrsController.addXAttr(project, user, path, FeaturestoreXAttrsConstants.TAGS, jsonTagsArr.toString());
  }

  /**
   * Get path to where featuregroup is stored
   * @param featuregroup
   * @return
   */
  private String getFeaturegroupLocation(Featuregroup featuregroup) {
    if (featuregroup.getFeaturegroupType() == FeaturegroupType.CACHED_FEATURE_GROUP) {
      return featuregroup.getCachedFeaturegroup().getHiveTbls()
          .getSdId()
          .getLocation();
    } else {
      return inodeController.getPath(featuregroup.getOnDemandFeaturegroup().getInode());
    }
  }
}
