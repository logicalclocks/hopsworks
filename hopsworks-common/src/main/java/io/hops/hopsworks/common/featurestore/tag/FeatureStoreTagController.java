/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
import java.util.List;
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
   * @param tagsJson
   * @return
   */
  public JSONArray convertToInternalTags(String tagsJson) {

    JSONObject tagsObject = new JSONObject(tagsJson);
    List<JSONObject> tagsList = new ArrayList<>();
    for(String key: tagsObject.keySet()) {
      JSONObject tag = new JSONObject();
      tag.put("key", key);
      if(!Strings.isNullOrEmpty(tagsObject.getString(key))) {
        tag.put("value", tagsObject.getString(key));
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
  public JSONObject convertToExternalTags(String tagsJson) {

    JSONObject tagsObject = null;
    if(!Strings.isNullOrEmpty(tagsJson)) {
      tagsObject = new JSONObject();
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
