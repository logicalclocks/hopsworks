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

package io.hops.hopsworks.common.featurestore.query;

import io.hops.hopsworks.common.dao.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.common.featurestore.feature.FeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import org.apache.calcite.sql.JoinType;

import java.util.List;

/**
 * Internal representation of a Query. It's a superset of QueryDTO.java as it contains
 * cached reference to feature gropus and list of available features for each feature group.
 * This is to avoid hitting the db too many times.
 */
public class Query {
  private FeaturegroupDTO leftFgDTO;
  private List<FeatureDTO> leftFeatures;
  private Featuregroup leftFg;
  private String leftFgAs;
  private List<FeatureDTO> leftAvailableFeatures;

  // Recursively merge QueryDTOs
  private Query query;

  // Join with another featuregroup
  private FeaturegroupDTO rightFgDTO;
  private List<FeatureDTO> rightFeatures;
  private Featuregroup rightFg;
  private String rightFgAs;
  private List<FeatureDTO> rightAvailableFeatures;

  private List<FeatureDTO> on;
  private List<FeatureDTO> leftOn;
  private List<FeatureDTO> rightOn;

  private JoinType type = JoinType.INNER;

  // For testing
  public Query() {
  }

  // Constructor for testing
  public Query(Featuregroup leftFg, Featuregroup rightFg) {
    this.leftFg = leftFg;
    this.rightFg = rightFg;
  }

  // Constructor for testing
  public Query(Featuregroup leftFg, List<FeatureDTO> leftFeatures,
               Featuregroup rightFg, List<FeatureDTO> rightFeatures) {
    this.leftFg = leftFg;
    this.leftFeatures = leftFeatures;
    this.rightFg = rightFg;
    this.rightFeatures = rightFeatures;
  }

  public Query(FeaturegroupDTO leftFgDTO, List<FeatureDTO> leftFeatures, Query query, FeaturegroupDTO rightFgDTO,
               List<FeatureDTO> rightFeatures, List<FeatureDTO> on, List<FeatureDTO> leftOn,
               List<FeatureDTO> rightOn, JoinType type) {

    this.leftFgDTO = leftFgDTO;
    this.leftFeatures = leftFeatures;
    this.query = query;
    this.rightFgDTO = rightFgDTO;
    this.rightFeatures = rightFeatures;
    this.on = on;
    this.leftOn = leftOn;
    this.rightOn = rightOn;
    this.type = type;
  }

  public FeaturegroupDTO getLeftFgDTO() {
    return leftFgDTO;
  }

  public void setLeftFgDTO(FeaturegroupDTO leftFgDTO) {
    this.leftFgDTO = leftFgDTO;
  }

  public List<FeatureDTO> getLeftFeatures() {
    return leftFeatures;
  }

  public void setLeftFeatures(List<FeatureDTO> leftFeatures) {
    this.leftFeatures = leftFeatures;
  }

  public Featuregroup getLeftFg() {
    return leftFg;
  }

  public void setLeftFg(Featuregroup leftFg) {
    this.leftFg = leftFg;
  }

  public List<FeatureDTO> getLeftAvailableFeatures() {
    return leftAvailableFeatures;
  }

  public void setLeftAvailableFeatures(List<FeatureDTO> leftAvailableFeatures) {
    this.leftAvailableFeatures = leftAvailableFeatures;
  }

  public Query getQuery() {
    return query;
  }

  public void setQuery(Query query) {
    this.query = query;
  }

  public FeaturegroupDTO getRightFgDTO() {
    return rightFgDTO;
  }

  public void setRightFgDTO(FeaturegroupDTO rightFgDTO) {
    this.rightFgDTO = rightFgDTO;
  }

  public List<FeatureDTO> getRightFeatures() {
    return rightFeatures;
  }

  public void setRightFeatures(List<FeatureDTO> rightFeatures) {
    this.rightFeatures = rightFeatures;
  }

  public Featuregroup getRightFg() {
    return rightFg;
  }

  public void setRightFg(Featuregroup rightFg) {
    this.rightFg = rightFg;
  }

  public String getLeftFgAs() {
    return leftFgAs;
  }

  public void setLeftFgAs(String leftFgAs) {
    this.leftFgAs = leftFgAs;
  }

  public String getRightFgAs() {
    return rightFgAs;
  }

  public void setRightFgAs(String rightFgAs) {
    this.rightFgAs = rightFgAs;
  }

  public List<FeatureDTO> getRightAvailableFeatures() {
    return rightAvailableFeatures;
  }

  public void setRightAvailableFeatures(List<FeatureDTO> rightAvailableFeatures) {
    this.rightAvailableFeatures = rightAvailableFeatures;
  }

  public List<FeatureDTO> getOn() {
    return on;
  }

  public void setOn(List<FeatureDTO> on) {
    this.on = on;
  }

  public List<FeatureDTO> getLeftOn() {
    return leftOn;
  }

  public void setLeftOn(List<FeatureDTO> leftOn) {
    this.leftOn = leftOn;
  }

  public List<FeatureDTO> getRightOn() {
    return rightOn;
  }

  public void setRightOn(List<FeatureDTO> rightOn) {
    this.rightOn = rightOn;
  }

  public JoinType getType() {
    return type;
  }

  public void setType(JoinType type) {
    this.type = type;
  }
}
