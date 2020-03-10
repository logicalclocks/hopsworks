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


import io.hops.hopsworks.common.featurestore.feature.FeatureDTO;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;

import java.util.List;

/**
 * Internal representation of a Query. It's a superset of QueryDTO.java as it contains
 * cached reference to feature gropus and list of available features for each feature group.
 * This is to avoid hitting the db too many times.
 */
public class Query {
  private List<FeatureDTO> features;
  private String featureStore;
  private Featuregroup featuregroup;
  private String as;
  private List<FeatureDTO> availableFeatures;

  private List<Join> joins;

  // For testing
  public Query() {
  }

  // Constructor for testing
  public Query(String featureStore, Featuregroup featuregroup) {
    this.featureStore = featureStore;
    this.featuregroup = featuregroup;
  }

  public Query(String featureStore, Featuregroup featuregroup, String as,
               List<FeatureDTO> features, List<FeatureDTO> availableFeatures) {
    this.featureStore = featureStore;
    this.featuregroup = featuregroup;
    this.as = as;
    this.features = features;
    this.availableFeatures = availableFeatures;
  }

  public String getFeatureStore() {
    return featureStore;
  }

  public void setFeatureStore(String featureStore) {
    this.featureStore = featureStore;
  }

  public List<FeatureDTO> getFeatures() {
    return features;
  }

  public void setFeatures(List<FeatureDTO> features) {
    this.features = features;
  }

  public Featuregroup getFeaturegroup() {
    return featuregroup;
  }

  public void setFeaturegroup(Featuregroup featuregroup) {
    this.featuregroup = featuregroup;
  }

  public List<FeatureDTO> getAvailableFeatures() {
    return availableFeatures;
  }

  public void setAvailableFeatures(List<FeatureDTO> availableFeatures) {
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
}
