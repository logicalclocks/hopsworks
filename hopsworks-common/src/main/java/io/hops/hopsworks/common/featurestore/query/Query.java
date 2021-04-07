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

package io.hops.hopsworks.common.featurestore.query;

import io.hops.hopsworks.common.featurestore.query.filter.FilterLogic;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;

import java.util.List;

/**
 * Internal representation of a Query. It's a superset of QueryDTO.java as it contains
 * cached reference to feature gropus and list of available features for each feature group.
 * This is to avoid hitting the db too many times.
 */
public class Query {

  private List<Feature> features;
  // feature store from which the feature group comes from
  // to build the FROM part of the offline query
  private String featureStore;
  // project from which the feature group comes from
  // to build the FROM part of the online query
  private String project;
  private Featuregroup featuregroup;
  private String leftFeatureGroupStartTime;
  private String leftFeatureGroupEndTime;
  private Long leftFeatureGroupStartTimestamp;
  private Long leftFeatureGroupEndTimestamp;
  private Long leftFeatureGroupEndCommitId;
  private String as;
  private List<Feature> availableFeatures;
  private Boolean hiveEngine = false;

  private List<Join> joins;
  private FilterLogic filter;

  // For testing
  public Query() {
  }

  // Constructor for testing
  public Query(String featureStore, Featuregroup featuregroup) {
    this.featureStore = featureStore;
    this.featuregroup = featuregroup;
  }

  // for testing
  public Query(String featureStore, String project, Featuregroup featuregroup, String as,
    List<Feature> features, List<Feature> availableFeatures) {
    this.featureStore = featureStore;
    this.project = project;
    this.featuregroup = featuregroup;
    this.as = as;
    this.features = features;
    this.availableFeatures = availableFeatures;
  }

  public Query(String featureStore, String project, Featuregroup featuregroup, String as,
    List<Feature> features, List<Feature> availableFeatures, Boolean hiveEngine) {
    this.featureStore = featureStore;
    this.project = project;
    this.featuregroup = featuregroup;
    this.as = as;
    this.features = features;
    this.availableFeatures = availableFeatures;
    this.hiveEngine = hiveEngine;
  }

  public Query(String featureStore, String project, Featuregroup featuregroup, String as) {
    this.featureStore = featureStore;
    this.project = project;
    this.featuregroup = featuregroup;
    this.as = as;
  }

  public String getFeatureStore() {
    return featureStore;
  }

  public void setFeatureStore(String featureStore) {
    this.featureStore = featureStore;
  }

  public List<Feature> getFeatures() {
    return features;
  }

  public void setFeatures(List<Feature> features) {
    this.features = features;
  }

  public Featuregroup getFeaturegroup() {
    return featuregroup;
  }

  public String getProject() {
    return project;
  }

  public void setProject(String project) {
    this.project = project;
  }

  public void setFeaturegroup(Featuregroup featuregroup) {
    this.featuregroup = featuregroup;
  }

  public String getLeftFeatureGroupStartTime() {
    return leftFeatureGroupStartTime;
  }

  public void setLeftFeatureGroupStartTime(String leftFeatureGroupStartTime) {
    this.leftFeatureGroupStartTime = leftFeatureGroupStartTime;
  }

  public String getLeftFeatureGroupEndTime() {
    return leftFeatureGroupEndTime;
  }

  public void setLeftFeatureGroupEndTime(String leftFeatureGroupEndTime) {
    this.leftFeatureGroupEndTime = leftFeatureGroupEndTime;
  }

  public Long getLeftFeatureGroupStartTimestamp() {
    return leftFeatureGroupStartTimestamp;
  }

  public void setLeftFeatureGroupStartTimestamp(Long leftFeatureGroupStartTimestamp) {
    this.leftFeatureGroupStartTimestamp = leftFeatureGroupStartTimestamp;
  }

  public Long getLeftFeatureGroupEndTimestamp() {
    return leftFeatureGroupEndTimestamp;
  }

  public void setLeftFeatureGroupEndTimestamp(Long leftFeatureGroupEndTimestamp) {
    this.leftFeatureGroupEndTimestamp = leftFeatureGroupEndTimestamp;
  }


  public Long getLeftFeatureGroupEndCommitId() {
    return leftFeatureGroupEndCommitId;
  }

  public void setLeftFeatureGroupEndCommitId(Long leftFeatureGroupEndCommitId) {
    this.leftFeatureGroupEndCommitId = leftFeatureGroupEndCommitId;
  }

  public List<Feature> getAvailableFeatures() {
    return availableFeatures;
  }

  public void setAvailableFeatures(List<Feature> availableFeatures) {
    this.availableFeatures = availableFeatures;
  }

  public String getAs() {
    return as;
  }

  public void setAs(String as) {
    this.as = as;
  }

  public List<Join> getJoins() {
    return joins;
  }

  public void setJoins(List<Join> joins) {
    this.joins = joins;
  }
  
  public FilterLogic getFilter() {
    return filter;
  }
  
  public void setFilter(FilterLogic filter) {
    this.filter = filter;
  }

  public Boolean getHiveEngine() {
    return hiveEngine;
  }

  public void setHiveEngine(Boolean hiveEngine) {
    this.hiveEngine = hiveEngine;
  }
}
