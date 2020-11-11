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

package io.hops.hopsworks.persistence.entity.featurestore.trainingdataset;

import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "training_dataset_feature", catalog = "hopsworks")
@XmlRootElement
public class TrainingDatasetFeature implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @JoinColumn(name = "training_dataset", referencedColumnName = "id")
  private TrainingDataset trainingDataset;
  @JoinColumn(name = "feature_group", referencedColumnName = "id")
  private Featuregroup featureGroup;
  @JoinColumn(name = "td_join", referencedColumnName = "id")
  private TrainingDatasetJoin trainingDatasetJoin;
  @Basic(optional = false)
  @Column(name = "name")
  private String name;
  @Basic(optional = false)
  @Column(name = "type")
  private String type;
  @Basic(optional = false)
  @Column(name = "idx")
  private Integer index;
  @Basic(optional = false)
  @Column(name = "label")
  private boolean label;

  public TrainingDatasetFeature() {
  }

  public TrainingDatasetFeature(TrainingDataset trainingDataset, TrainingDatasetJoin trainingDatasetJoin,
                                Featuregroup featureGroup, String name, String type, Integer index, boolean label) {
    this.trainingDataset = trainingDataset;
    this.trainingDatasetJoin = trainingDatasetJoin;
    this.featureGroup = featureGroup;
    this.name = name;
    this.type = type;
    this.index = index;
    this.label = label;
  }

  public TrainingDatasetFeature(TrainingDataset trainingDataset, String name, String type, Integer index,
                                boolean label) {
    this.trainingDataset = trainingDataset;
    this.name = name;
    this.type = type;
    this.index = index;
    this.label = label;
  }

  public static long getSerialVersionUID() {
    return serialVersionUID;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public TrainingDataset getTrainingDataset() {
    return trainingDataset;
  }

  public void setTrainingDataset(TrainingDataset trainingDataset) {
    this.trainingDataset = trainingDataset;
  }

  public Featuregroup getFeatureGroup() {
    return featureGroup;
  }

  public void setFeatureGroup(Featuregroup featureGroup) {
    this.featureGroup = featureGroup;
  }

  public TrainingDatasetJoin getTrainingDatasetJoin() {
    return trainingDatasetJoin;
  }

  public void setTrainingDatasetJoin(TrainingDatasetJoin trainingDatasetJoin) {
    this.trainingDatasetJoin = trainingDatasetJoin;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Integer getIndex() {
    return index;
  }

  public void setIndex(Integer index) {
    this.index = index;
  }
  
  public boolean isLabel() {
    return label;
  }
  
  public void setLabel(boolean label) {
    this.label = label;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TrainingDatasetFeature that = (TrainingDatasetFeature) o;

    if (!Objects.equals(id, that.id)) return false;
    if (!Objects.equals(trainingDataset, that.trainingDataset))
      return false;
    if (!Objects.equals(featureGroup, that.featureGroup)) return false;
    if (!Objects.equals(name, that.name)) return false;
    return Objects.equals(index, that.index);
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }
}
