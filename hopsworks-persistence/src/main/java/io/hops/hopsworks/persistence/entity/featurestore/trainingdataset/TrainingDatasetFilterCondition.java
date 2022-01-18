/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.persistence.entity.featurestore.trainingdataset;

import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "training_dataset_filter_condition", catalog = "hopsworks")
public class TrainingDatasetFilterCondition implements Serializable {

  private static final long serialVersionUID = 1L;

  @JoinColumn(name = "training_dataset_filter_id", referencedColumnName = "id")
  @OneToOne
  private TrainingDatasetFilter trainingDatasetFilter;
  @JoinColumn(name = "feature_group_id", referencedColumnName = "id")
  @OneToOne
  private Featuregroup featureGroup;
  @Column(name = "feature_name")
  private String feature;
  @Column(name = "filter_condition")
  private String condition;
  @Column(name = "filter_value_fg_id")
  private Integer valueFeatureGroupId;
  @Column(name = "filter_value")
  private String value;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

  public TrainingDatasetFilterCondition() {
  }

  public TrainingDatasetFilterCondition(
      TrainingDatasetFilter trainingDatasetFilter,
      Featuregroup featureGroup, String feature, String condition, Integer valueFeatureGroupId, String value) {
    this.trainingDatasetFilter = trainingDatasetFilter;
    this.featureGroup = featureGroup;
    this.feature = feature;
    this.condition = condition;
    this.valueFeatureGroupId = valueFeatureGroupId;
    this.value = value;
  }

  public TrainingDatasetFilter getTrainingDatasetFilter() {
    return trainingDatasetFilter;
  }

  public void setTrainingDatasetFilter(
      TrainingDatasetFilter trainingDatasetFilter) {
    this.trainingDatasetFilter = trainingDatasetFilter;
  }

  public Featuregroup getFeatureGroup() {
    return featureGroup;
  }

  public void setFeatureGroup(Featuregroup featureGroup) {
    this.featureGroup = featureGroup;
  }

  public String getFeature() {
    return feature;
  }

  public void setFeature(String feature) {
    this.feature = feature;
  }

  public String getCondition() {
    return condition;
  }

  public void setCondition(String condition) {
    this.condition = condition;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getValueFeatureGroupId() {
    return valueFeatureGroupId;
  }

  public void setValueFeatureGroupId(Integer valueFeatureGroupId) {
    this.valueFeatureGroupId = valueFeatureGroupId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TrainingDatasetFilterCondition that = (TrainingDatasetFilterCondition) o;
    return Objects.equals(featureGroup, that.featureGroup) && Objects.equals(feature, that.feature) &&
        Objects.equals(condition, that.condition) &&
        Objects.equals(valueFeatureGroupId, that.valueFeatureGroupId) &&
        Objects.equals(value, that.value) && Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(featureGroup, feature, condition, valueFeatureGroupId, value, id);
  }
}
