/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.persistence.entity.featurestore.metadata;

import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@Entity
@Table(name = "feature_store_keyword", catalog = "hopsworks")
@XmlRootElement
public class FeatureStoreKeyword implements FeatureStoreMetadata, Serializable {
  
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  
  @JoinColumn(name = "feature_group_id", referencedColumnName = "id")
  @ManyToOne()
  private Featuregroup featureGroup;
  @JoinColumn(name = "feature_view_id", referencedColumnName = "id")
  @ManyToOne()
  private FeatureView featureView;
  @JoinColumn(name = "training_dataset_id", referencedColumnName = "id")
  @ManyToOne()
  private TrainingDataset trainingDataset;
  
  @Column(name = "name")
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 255)
  private String name;
  
  public FeatureStoreKeyword() {
  }
  
  public FeatureStoreKeyword(String name) {
    this.name = name;
  }
  
  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  public Featuregroup getFeatureGroup() {
    return featureGroup;
  }
  
  public void setFeatureGroup(Featuregroup featureGroup) {
    this.featureGroup = featureGroup;
  }
  
  public FeatureView getFeatureView() {
    return featureView;
  }
  
  public void setFeatureView(FeatureView featureView) {
    this.featureView = featureView;
  }
  
  public TrainingDataset getTrainingDataset() {
    return trainingDataset;
  }
  
  public void setTrainingDataset(
    TrainingDataset trainingDataset) {
    this.trainingDataset = trainingDataset;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  @Override
  public String toString() {
    return "FeatureStoreKeyword[ id=" + id + " ]";
  }
}
