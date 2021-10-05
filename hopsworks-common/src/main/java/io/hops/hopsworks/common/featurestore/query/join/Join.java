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

package io.hops.hopsworks.common.featurestore.query.join;

import io.hops.hopsworks.common.featurestore.query.Feature;
import io.hops.hopsworks.common.featurestore.query.Query;
import io.hops.hopsworks.common.featurestore.query.SqlCondition;
import org.apache.calcite.sql.JoinType;

import java.util.List;

public class Join {
  private Query leftQuery;
  private Query rightQuery;

  private List<Feature> leftOn;
  private List<Feature> rightOn;
  private JoinType joinType;
  private List<SqlCondition> joinOperator;

  private String prefix;

  public Join(Query leftQuery, Query rightQuery, List<Feature> leftOn, List<Feature> rightOn, JoinType joinType,
              String prefix, List<SqlCondition> joinOperator) {
    this.leftQuery = leftQuery;
    this.rightQuery = rightQuery;
    this.leftOn = leftOn;
    this.rightOn = rightOn;
    this.joinType = joinType;
    this.prefix = prefix;
    this.joinOperator = joinOperator;
  }

  public Query getLeftQuery() {
    return leftQuery;
  }

  public void setLeftQuery(Query leftQuery) {
    this.leftQuery = leftQuery;
  }

  public Query getRightQuery() {
    return rightQuery;
  }

  public void setRightQuery(Query rightQuery) {
    this.rightQuery = rightQuery;
  }

  public void setLeftOn(List<Feature> leftOn) {
    this.leftOn = leftOn;
  }

  public void setRightOn(List<Feature> rightOn) {
    this.rightOn = rightOn;
  }

  public JoinType getJoinType() {
    return joinType;
  }

  public void setJoinType(JoinType joinType) {
    this.joinType = joinType;
  }

  public List<Feature> getLeftOn() {
    return leftOn;
  }

  public List<Feature> getRightOn() {
    return rightOn;
  }

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }
  
  public List<SqlCondition> getJoinOperator() {
    return joinOperator;
  }
  
  public void setJoinOperator(List<SqlCondition> joinOperator) {
    this.joinOperator = joinOperator;
  }
}
