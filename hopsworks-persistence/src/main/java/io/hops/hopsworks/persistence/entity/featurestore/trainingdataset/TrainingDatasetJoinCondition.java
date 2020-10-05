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

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
@Table(name = "training_dataset_join_condition", catalog = "hopsworks")
public class TrainingDatasetJoinCondition implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @JoinColumn(name = "td_join", referencedColumnName = "id")
  private TrainingDatasetJoin join;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 1000)
  @Column(name = "left_feature")
  private String leftFeature;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 1000)
  @Column(name = "right_feature")
  private String rightFeature;

  public TrainingDatasetJoinCondition() {
  }
  public TrainingDatasetJoinCondition(Integer id) {
    this.id = id;
  }

  public TrainingDatasetJoinCondition(Integer id, TrainingDatasetJoin join, String leftFeature, String rightFeature) {
    this.id = id;
    this.join = join;
    this.leftFeature = leftFeature;
    this.rightFeature = rightFeature;
  }

  public TrainingDatasetJoinCondition(TrainingDatasetJoin join, String leftFeature, String rightFeature) {
    this.join = join;
    this.leftFeature = leftFeature;
    this.rightFeature = rightFeature;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public TrainingDatasetJoin getJoin() {
    return join;
  }

  public void setJoin(TrainingDatasetJoin join) {
    this.join = join;
  }

  public String getLeftFeature() {
    return leftFeature;
  }

  public void setLeftFeature(String leftFeature) {
    this.leftFeature = leftFeature;
  }

  public String getRightFeature() {
    return rightFeature;
  }

  public void setRightFeature(String rightFeature) {
    this.rightFeature = rightFeature;
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
    if (!(object instanceof TrainingDatasetJoinCondition)) {
      return false;
    }
    TrainingDatasetJoinCondition other = (TrainingDatasetJoinCondition) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }
  @Override
  public String toString() {
    return "io.hops.hopsworks.persistence.entity.featurestore.TrainingDatasetJoinCondition[ id=" + id + " ]";
  }
}

