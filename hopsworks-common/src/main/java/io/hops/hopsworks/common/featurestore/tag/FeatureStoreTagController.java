/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.common.featurestore.tag;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.featurestore.tag.FeatureStoreTagFacade;
import io.hops.hopsworks.exceptions.FeatureStoreTagException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.tag.FeatureStoreTag;
import io.hops.hopsworks.persistence.entity.featurestore.tag.TagType;
import io.hops.hopsworks.restutils.RESTCodes;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureStoreTagController {

  @EJB
  private FeatureStoreTagFacade featureStoreTagFacade;

  private void validateName(String name) throws FeatureStoreTagException {
    if (name == null || name.trim().isEmpty() || name.trim().contains(" ")) {
      throw new FeatureStoreTagException(RESTCodes.FeatureStoreTagErrorCode.INVALID_TAG_NAME, Level.FINE);
    }
  }

  public List<FeatureStoreTag> get() {
    List<FeatureStoreTag> featureStoreTagList = featureStoreTagFacade.findAll();
    if (featureStoreTagList == null) {
      featureStoreTagList = new ArrayList<>();
    }
    return featureStoreTagList;
  }

  public void update(Integer id, String newName, TagType type) throws FeatureStoreTagException {
    FeatureStoreTag tag = featureStoreTagFacade.find(id);
    update(tag, newName, type);
  }

  public void update(String name, String newName, TagType type) throws FeatureStoreTagException {
    FeatureStoreTag tag = featureStoreTagFacade.findByName(name);
    update(tag, newName, type);
  }

  private void update(FeatureStoreTag tag, String newName, TagType type) throws FeatureStoreTagException {
    if (tag == null) {
      throw new FeatureStoreTagException(RESTCodes.FeatureStoreTagErrorCode.TAG_NOT_FOUND, Level.FINE);
    }
    validateName(newName);
    FeatureStoreTag newTag = featureStoreTagFacade.findByName(newName);
    if (newTag != null) {
      throw new FeatureStoreTagException(RESTCodes.FeatureStoreTagErrorCode.TAG_ALREADY_EXISTS, Level.FINE);
    }
    tag.setName(newName);
    tag.setType(type);
    featureStoreTagFacade.update(tag);
  }

  public void create(String name, TagType type) throws FeatureStoreTagException {
    validateName(name);
    FeatureStoreTag tag = featureStoreTagFacade.findByName(name);
    if (tag != null) {
      throw new FeatureStoreTagException(RESTCodes.FeatureStoreTagErrorCode.TAG_ALREADY_EXISTS, Level.FINE);
    }
    tag = new FeatureStoreTag(name, type);
    featureStoreTagFacade.save(tag);
  }

  public void delete(String name) {
    FeatureStoreTag tag = featureStoreTagFacade.findByName(name);
    delete(tag);
  }

  public void delete(FeatureStoreTag tag) {
    if (tag != null) {
      featureStoreTagFacade.remove(tag);
    }
  }

  /**
   * Validates that tags being attached are previously defined
   *
   * @param tagsJson json containing tags on the form:
   *                {'updated': 'daily', 'jobtype': 'batch'}
   * @throws FeaturestoreException
   */
  public void validateTags(String tagsJson) throws FeaturestoreException {
    List<FeatureStoreTag> validTags = this.get();
    if(validTags == null || validTags.isEmpty()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TAG_NOT_ALLOWED,
          Level.FINE, "No keys have been defined");
    }
    Set<String> validKeysSet = validTags.stream().map(FeatureStoreTag::getName).collect(Collectors.toSet());
    if(!Strings.isNullOrEmpty(tagsJson)) {
      JSONObject tagsObj = new JSONObject(tagsJson);
      for (String tag : tagsObj.keySet()) {
        if (!validKeysSet.contains(tag)) {
          throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TAG_NOT_ALLOWED,
              Level.FINE, tag + " is not a valid tag. Allowed tags include "
              + validKeysSet.toString());
        }
      }
    }
  }

  /**
   * Convert {'updated': 'daily', 'jobtype': 'batch'}
   * to [{'key': 'updated', 'value': 'daily'}, {'key': 'jobtype', 'value': 'batch'}]
   *
   * @param externalTags
   * @return
   */
  public JSONArray convertToInternalTags(Map<String, String> externalTags) {
    List<JSONObject> tagsList = new ArrayList<>();
    for(Map.Entry<String, String> entry : externalTags.entrySet()) {
      JSONObject tag = new JSONObject();
      tag.put("key", entry.getKey());
      if(!Strings.isNullOrEmpty(entry.getValue())) {
        tag.put("value", entry.getValue());
      }
      tagsList.add(tag);
    }
    return new JSONArray(tagsList);
  }

  /**
   *
   * Convert [{'key': 'updated', 'value': 'daily'}, {'key': 'jobtype', 'value': 'batch'}]
   * to {'updated': 'daily', 'jobtype': 'batch'}
   *
   * @param tagsJson
   * @return
   */
  public Map<String, String> convertToExternalTags(String tagsJson) {
    Map<String, String> tagsObject = new HashMap<>();

    if(!Strings.isNullOrEmpty(tagsJson)) {
      JSONArray tagsArr = new JSONArray(tagsJson);
      for (int i = 0; i < tagsArr.length(); i++) {
        JSONObject currentTag = tagsArr.getJSONObject(i);
        if(currentTag.has("value")) {
          tagsObject.put(currentTag.getString("key"), currentTag.getString("value"));
        } else {
          tagsObject.put(currentTag.getString("key"), "");
        }
      }
    }
    return tagsObject;
  }
}
