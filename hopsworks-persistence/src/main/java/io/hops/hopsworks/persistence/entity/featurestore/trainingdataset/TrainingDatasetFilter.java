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

import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "training_dataset_filter", catalog = "hopsworks")
public class TrainingDatasetFilter implements Serializable {

  private static final long serialVersionUID = 1L;

  @JoinColumn(name = "training_dataset_id", referencedColumnName = "id")
  @ManyToOne
  private TrainingDataset trainingDataset;
  @JoinColumn(name = "feature_view_id", referencedColumnName = "id")
  @ManyToOne
  private FeatureView featureView;
  @OneToOne(cascade = CascadeType.ALL, mappedBy = "trainingDatasetFilter")
  private TrainingDatasetFilterCondition condition;
  @Column(name = "type")
  @Enumerated(EnumType.STRING)
  private SqlFilterLogic type;
  @Column(name = "path")
  private String path;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

  public TrainingDatasetFilter() {
  }

  public TrainingDatasetFilter(
      FeatureView featureView) {
    this.featureView = featureView;
  }

  public TrainingDatasetFilter(
      TrainingDataset trainingDataset) {
    this.trainingDataset = trainingDataset;
  }

  public TrainingDataset getTrainingDataset() {
    return trainingDataset;
  }

  public void setTrainingDataset(
      TrainingDataset trainingDataset) {
    this.trainingDataset = trainingDataset;
  }

  public FeatureView getFeatureView() {
    return featureView;
  }

  public void setFeatureView(FeatureView featureView) {
    this.featureView = featureView;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public TrainingDatasetFilterCondition getCondition() {
    return condition;
  }

  public void setCondition(
      TrainingDatasetFilterCondition condition) {
    this.condition = condition;
  }

  public SqlFilterLogic getType() {
    return type;
  }

  public void setType(SqlFilterLogic type) {
    this.type = type;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TrainingDatasetFilter that = (TrainingDatasetFilter) o;
    return Objects.equals(condition, that.condition) && Objects.equals(type, that.type) &&
        Objects.equals(path, that.path) && Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(condition, type, path, id);
  }
}
