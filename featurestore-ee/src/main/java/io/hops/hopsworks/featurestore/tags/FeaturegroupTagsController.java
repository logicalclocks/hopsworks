/*
 *Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.featurestore.tags;

import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupFacade;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.CachedFeaturegroupFacade;
import io.hops.hopsworks.common.featurestore.tag.FeatureStoreTagController;
import io.hops.hopsworks.common.featurestore.tag.FeaturegroupTagControllerIface;
import io.hops.hopsworks.common.featurestore.xattr.dto.FeaturestoreXAttrsConstants;
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
  private FeaturegroupFacade featuregroupFacade;
  @EJB
  private CachedFeaturegroupFacade cachedFeaturegroupFacade;
  @EJB
  private XAttrsController xAttrsController;
  @EJB
  private FeatureStoreTagController featureStoreTagController;

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

    Featuregroup featuregroup = validateFeaturegroup(featuregroupId, featurestore);

    String path = getFeaturegroupLocation(featuregroup);

    Map<String, String> xattrsMap = xAttrsController.getXAttrs(project, user, path, FeaturestoreXAttrsConstants.TAGS);

    JSONObject tags = featureStoreTagController.convertToExternalTags(xattrsMap.get(FeaturestoreXAttrsConstants.TAGS));
    Map<String, String> tagsMap = new HashMap<>();
    if(tags != null) {
      for(String key: tags.keySet()) {
        tagsMap.put(key, tags.getString(key));
      }
    }
    return tagsMap;
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

    Featuregroup featuregroup = validateFeaturegroup(featuregroupId, featurestore);

    String path = getFeaturegroupLocation(featuregroup);

    Map<String, String> xattrsMap = xAttrsController.getXAttrs(project, user, path, FeaturestoreXAttrsConstants.TAGS);

    JSONObject tags = featureStoreTagController.convertToExternalTags(xattrsMap.get(FeaturestoreXAttrsConstants.TAGS));
    Map<String, String> tagsMap = new HashMap<>();
    if (tags != null && tags.has(tagName)) {
      String tagValue = tags.getString(tagName);
      tagsMap.put(tagName, tagValue);
    } else {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TAG_NOT_FOUND, Level.FINE);
    }

    return tagsMap;
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

    Featuregroup featuregroup = validateFeaturegroup(featuregroupId, featurestore);

    String path = getFeaturegroupLocation(featuregroup);

    String tagsJson = new JSONObject().put(tag, value).toString();

    featureStoreTagController.validateTags(tagsJson);

    Map<String, String> xattrsMap = xAttrsController.getXAttrs(project, user, path, FeaturestoreXAttrsConstants.TAGS);

    JSONObject newTags = new JSONObject();
    if(xattrsMap != null && xattrsMap.size() > 0) {
      JSONObject existingTags = featureStoreTagController.convertToExternalTags(
        xattrsMap.get(FeaturestoreXAttrsConstants.TAGS));
      if (existingTags != null) {
        for (String tagName : existingTags.keySet()) {
          newTags.put(tagName, existingTags.get(tagName));
        }
      }
    }
    if(value != null) {
      newTags.put(tag, value);
    } else {
      newTags.put(tag, "");
    }
    JSONArray jsonTagsArr = featureStoreTagController.convertToInternalTags(newTags.toString());
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

    Featuregroup featuregroup = validateFeaturegroup(featuregroupId, featurestore);

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

    Featuregroup featuregroup = validateFeaturegroup(featuregroupId, featurestore);

    String path = getFeaturegroupLocation(featuregroup);

    Map<String, String> xattrsMap = xAttrsController.getXAttrs(project, user, path, FeaturestoreXAttrsConstants.TAGS);

    JSONObject tags = featureStoreTagController.convertToExternalTags(xattrsMap.get(FeaturestoreXAttrsConstants.TAGS));
    JSONObject newTags = new JSONObject();
    if (tags != null) {
      if (tags.has(tagName)) {
        tags.remove(tagName);
      }
      for (String attachedTag : tags.keySet()) {
        newTags.put(attachedTag, tags.get(attachedTag));
      }
    }

    JSONArray jsonTagsArr = featureStoreTagController.convertToInternalTags(newTags.toString());
    xAttrsController.addXAttr(project, user, path, FeaturestoreXAttrsConstants.TAGS, jsonTagsArr.toString());
  }

  /**
   * Validate that the featuregroup supports XAttrs
   * @param featuregroupId
   * @param featurestore
   * @return
   * @throws FeaturestoreException
   */
  private Featuregroup validateFeaturegroup(int featuregroupId, Featurestore featurestore)
      throws FeaturestoreException {
    Featuregroup featuregroup = featuregroupFacade.findByIdAndFeaturestore(featuregroupId, featurestore);

    if(!featuregroup.getFeaturegroupType().equals(FeaturegroupType.CACHED_FEATURE_GROUP)) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TAG_NOT_ALLOWED,
          Level.FINE, "Tags is only supported for " + FeaturegroupType.CACHED_FEATURE_GROUP);
    }
    return featuregroup;
  }

  /**
   * Get path to where featuregroup is stored
   * @param featuregroup
   * @return
   */
  private String getFeaturegroupLocation(Featuregroup featuregroup) {
    return featuregroup.getCachedFeaturegroup().getHiveTbls()
        .getSdId()
        .getLocation();
  }
}
