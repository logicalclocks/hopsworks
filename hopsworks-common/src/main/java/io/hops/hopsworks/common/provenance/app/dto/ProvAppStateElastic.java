/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.provenance.app.dto;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.annotation.XmlRootElement;

import io.hops.hopsworks.common.provenance.app.ProvAParser;
import io.hops.hopsworks.common.provenance.core.elastic.BasicElasticHit;
import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.common.provenance.util.ProvHelper;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.restutils.RESTCodes;

/**
 * Represents a JSONifiable version of the elastic hit object
 */
@XmlRootElement
public class ProvAppStateElastic implements Comparator<ProvAppStateElastic> {

  private static final Logger LOG = Logger.getLogger(ProvAppStateElastic.class.getName());

  private String id;
  private float score;
  private Map<String, Object> map;

  private String appId;
  private Provenance.AppState appState = null;
  private long appStateTimestamp;
  private String readableTimestamp;
  private String appName;
  private String appUser;

  public ProvAppStateElastic() {
  }

  public static ProvAppStateElastic instance(BasicElasticHit hit) throws ProvenanceException {
    ProvAppStateElastic result = new ProvAppStateElastic();
    result.id = hit.getId();
    result.score = hit.getScore();
    result.map = hit.getSource();
  
    Map<String, Object> map = new HashMap<>(result.map);
    try {
      //Even though we define in the elastic mapping a Long,
      //if the value is small, it seems to be retrieved as an Integer
      //so we need to do the complicated castings lambda
      result.appId = ProvHelper.extractElasticField(map, ProvAParser.Field.APP_ID);
      result.appState = ProvHelper.extractElasticField(map, ProvAParser.Field.APP_STATE);
      result.appStateTimestamp = ProvHelper.extractElasticField(map, ProvAParser.Field.TIMESTAMP);
      result.appName = ProvHelper.extractElasticField(map, ProvAParser.Field.APP_NAME);
      result.appUser = ProvHelper.extractElasticField(map, ProvAParser.Field.APP_USER);
      result.readableTimestamp = ProvHelper.extractElasticField(map, ProvAParser.Field.R_TIMESTAMP);
  
      for (Map.Entry<String, Object> entry : map.entrySet()) {
        throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.INTERNAL_ERROR, Level.INFO,
          "field:" + entry.getKey() + "not managed in file state return");
      }
    }catch(ClassCastException e) {
      String msg = "mismatch between DTO class and ProvAParser field types (elastic)";
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.INTERNAL_ERROR, Level.WARNING, msg, msg, e);
    }
    return result;
  }

  @Override
  public int compare(ProvAppStateElastic o1, ProvAppStateElastic o2) {
    return Float.compare(o2.getScore(), o1.getScore());
  }

  public float getScore() {
    return score;
  }

  public void setScore(float score) {
    this.score = score;
  }

  public void setMap(Map<String, Object> source) {
    this.map = new HashMap<>(source);
  }

  public Map<String, String> getMap() {
    //flatten hits (remove nested json objects) to make it more readable
    Map<String, String> refined = new HashMap<>();

    if (this.map != null) {
      for (Map.Entry<String, Object> entry : this.map.entrySet()) {
        //convert value to string
        String value = (entry.getValue() == null) ? "null" : entry.getValue().toString();
        refined.put(entry.getKey(), value);
      }
    }

    return refined;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public Provenance.AppState getAppState() {
    return appState;
  }

  public void setAppState(Provenance.AppState appState) {
    this.appState = appState;
  }

  public Long getAppStateTimestamp() {
    return appStateTimestamp;
  }

  public void setAppStateTimestamp(long appStateTimestamp) {
    this.appStateTimestamp = appStateTimestamp;
  }

  public String getAppName() {
    return appName;
  }

  public void setAppName(String appName) {
    this.appName = appName;
  }

  public String getAppUser() {
    return appUser;
  }

  public void setAppUser(String appUser) {
    this.appUser = appUser;
  }
  
  public String getReadableTimestamp() {
    return readableTimestamp;
  }
  
  public void setReadableTimestamp(String readableTimestamp) {
    this.readableTimestamp = readableTimestamp;
  }
}
