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

public class Filter {

  private Feature feature;
  private SqlCondition condition;
  private String value;
  
  public Filter(Feature feature, SqlCondition condition, String value) {
    this.feature = feature;
    this.condition = condition;
    this.value = value;
  }

  public Feature getFeature() {
    return feature;
  }
  
  public void setFeature(Feature feature) {
    this.feature = feature;
  }
  
  public SqlCondition getCondition() {
    return condition;
  }
  
  public void setCondition(SqlCondition condition) {
    this.condition = condition;
  }
  
  public String getValue() {
    return value;
  }
  
  public void setValue(String value) {
    this.value = value;
  }
}

