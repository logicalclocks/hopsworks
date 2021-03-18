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
package io.hops.hopsworks.persistence.entity.featurestore.trainingdataset;

import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "training_dataset_join", catalog = "hopsworks")
public class TrainingDatasetJoin implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @Column(name = "type")
  private short type;
  @Basic(optional = false)
  @NotNull
  @Column(name = "idx")
  private Integer index;
  @Basic(optional = false)
  @Column(name = "feature_group_commit_id")
  private Long featureGroupCommitId;
  @JoinColumn(name = "training_dataset", referencedColumnName = "id")
  private TrainingDataset trainingDataset;
  @JoinColumn(name = "feature_group", referencedColumnName = "id")
  private Featuregroup featureGroup;
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "join")
  private Collection<TrainingDatasetJoinCondition> conditions;

  public TrainingDatasetJoin() {
  }

  public TrainingDatasetJoin(Integer id) {
    this.id = id;
  }

  public TrainingDatasetJoin(TrainingDataset trainingDataset, Featuregroup featureGroup, short type, int index) {
    this.trainingDataset = trainingDataset;
    this.featureGroup = featureGroup;
    this.type = type;
    this.index = index;
  }

  public TrainingDatasetJoin(TrainingDataset trainingDataset, Featuregroup featureGroup, Long featureGroupCommitId,
                             short type, int index) {
    this.trainingDataset = trainingDataset;
    this.featureGroup = featureGroup;
    this.featureGroupCommitId = featureGroupCommitId;
    this.type = type;
    this.index = index;
  }

  public TrainingDatasetJoin(Featuregroup featureGroup, short type, int index,
                             List<TrainingDatasetJoinCondition> conditions) {
    this.featureGroup = featureGroup;
    this.type = type;
    this.index = index;
    this.conditions = conditions;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public short getType() {
    return type;
  }

  public void setType(short type) {
    this.type = type;
  }

  public Integer getIndex() {
    return index;
  }

  public void setIdx(Integer index) {
    this.index = index;
  }

  public TrainingDataset getTrainingDataset() {
    return trainingDataset;
  }

  public void setTrainingDataset(TrainingDataset trainingDataset) {
    this.trainingDataset = trainingDataset;
  }

  public Long getFeatureGroupCommitId() {
    return featureGroupCommitId;
  }

  public void setFeatureGroupCommitId(Long featureGroupCommitId) {
    this.featureGroupCommitId = featureGroupCommitId;
  }

  public Featuregroup getFeatureGroup() {
    return featureGroup;
  }

  public void setFeatureGroup(Featuregroup featureGroup) {
    this.featureGroup = featureGroup;
  }

  public Collection<TrainingDatasetJoinCondition> getConditions() {
    return conditions;
  }

  public void setConditions(Collection<TrainingDatasetJoinCondition> conditions) {
    this.conditions = conditions;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (id != null ? id.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof TrainingDatasetJoin)) {
      return false;
    }
    TrainingDatasetJoin other = (TrainingDatasetJoin) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.persistence.entity.featurestore.TrainingDatasetJoin[ id=" + id + " ]";
  }
}

