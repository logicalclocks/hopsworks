/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.featurestore.tags;

import com.google.common.base.Strings;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeatureStoreTagsHelper {
  /**
   * Convert {'updated': 'daily', 'jobtype': 'batch'}
   * to [{'key': 'updated', 'value': 'daily'}, {'key': 'jobtype', 'value': 'batch'}]
   *
   * @param externalTags
   * @return
   */
  public static JSONArray convertToInternalTags(Map<String, String> externalTags) {
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
  public static Map<String, String> convertToExternalTags(String tagsJson) {
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
