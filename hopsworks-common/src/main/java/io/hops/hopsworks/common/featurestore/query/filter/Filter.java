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

package io.hops.hopsworks.common.featurestore.query.filter;

import io.hops.hopsworks.common.featurestore.query.Feature;
import io.hops.hopsworks.common.featurestore.query.SqlCondition;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Filter {

  private List<Feature> features;
  private SqlCondition condition;
  private FilterValue value;
  
  public Filter(List<Feature> features, SqlCondition condition, String value) {
    this.features = features;
    this.condition = condition;
    this.value = new FilterValue(value);
  }

  public Filter(Feature feature, SqlCondition condition, FilterValue value) {
    this.features = Arrays.asList(feature);
    this.condition = condition;
    this.value = value;
  }

  public Filter(List<Feature> features, SqlCondition condition, FilterValue value) {
    this.features = features;
    this.condition = condition;
    this.value = value;
  }

  public Filter(Feature feature, SqlCondition condition, String value) {
    this.features = Arrays.asList(feature);
    this.condition = condition;
    this.value = new FilterValue(value);
  }

  public List<Feature> getFeatures() {
    return features;
  }

  public void setFeatures(List<Feature> features) {
    this.features = features;
  }
  
  public SqlCondition getCondition() {
    return condition;
  }
  
  public void setCondition(SqlCondition condition) {
    this.condition = condition;
  }
  
  public FilterValue getValue() {
    return value;
  }
  
  public void setValue(FilterValue value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Filter filter = (Filter) o;
    return Objects.equals(features, filter.features) && condition == filter.condition &&
        Objects.equals(value, filter.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(features, condition, value);
  }
}

