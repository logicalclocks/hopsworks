/*
 * This file is part of Hopsworks
 * Copyright (C) 2024, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.persistence.entity.provenance;

import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.models.version.ModelVersion;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Objects;

/**
 * Entity class representing the model_link table in Hopsworks database.
 * An instance of this class represents a row in the table.
 */
@Entity
@Table(name = "model_link", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "ModelLink.findByChildren",
        query = "SELECT l FROM ModelLink l LEFT JOIN FETCH l.parentTrainingDataset " +
            "WHERE l.model IN :children " +
            "ORDER BY l.parentTrainingDataset ASC, l.id DESC"),
    @NamedQuery(name = "ModelLink.findByParents",
        query = "SELECT l FROM ModelLink l LEFT JOIN FETCH l.model " +
            "WHERE l.parentTrainingDataset IN :parents " +
            "ORDER BY l.model.model.name ASC, l.model.version DESC, l.id DESC")})
public class ModelLink implements ProvExplicitNode, Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @JoinColumn(name = "model_version_id", referencedColumnName = "id")
  @OneToOne(fetch = FetchType.LAZY)
  private ModelVersion model;
  @JoinColumn(name = "parent_training_dataset_id", referencedColumnName = "id")
  @OneToOne(fetch = FetchType.LAZY)
  private TrainingDataset parentTrainingDataset;
  @Basic(optional = false)
  @Column(name = "parent_feature_store")
  private String parentFeatureStore;
  @Basic(optional = false)
  @Column(name = "parent_feature_view_name")
  private String parentFeatureViewName;
  @Basic(optional = false)
  @Column(name = "parent_feature_view_version")
  private Integer parentFeatureViewVersion;
  @Basic(optional = false)
  @Column(name = "parent_training_dataset_version")
  private Integer parentTrainingDatasetVersion;

  public ModelLink() {}

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public ModelVersion getModel() {
    return model;
  }

  public void setModel(ModelVersion model) {
    this.model = model;
  }

  public TrainingDataset getParentTrainingDataset() {
    return parentTrainingDataset;
  }

  public void setParentTrainingDataset(TrainingDataset parentTrainingDataset) {
    this.parentTrainingDataset = parentTrainingDataset;
  }

  public String getParentFeatureStore() {
    return parentFeatureStore;
  }

  public void setParentFeatureStore(String parentFeatureStore) {
    this.parentFeatureStore = parentFeatureStore;
  }

  public String getParentFeatureViewName() {
    return parentFeatureViewName;
  }

  public void setParentFeatureViewName(String parentFeatureViewName) {
    this.parentFeatureViewName = parentFeatureViewName;
  }

  public Integer getParentFeatureViewVersion() {
    return parentFeatureViewVersion;
  }

  public void setParentFeatureViewVersion(Integer parentFeatureViewVersion) {
    this.parentFeatureViewVersion = parentFeatureViewVersion;
  }

  public Integer getParentTrainingDatasetVersion() {
    return parentTrainingDatasetVersion;
  }

  public void setParentTrainingDatasetVersion(Integer parentTrainingDatasetVersion) {
    this.parentTrainingDatasetVersion = parentTrainingDatasetVersion;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ModelLink)) {
      return false;
    }
    ModelLink modelLink = (ModelLink) o;
    return id.equals(modelLink.id) &&
        model.equals(modelLink.model) &&
        parentTrainingDataset.equals(modelLink.parentTrainingDataset) &&
        parentFeatureStore.equals(modelLink.parentFeatureStore) &&
        parentFeatureViewName.equals(modelLink.parentFeatureViewName) &&
        parentFeatureViewVersion.equals(modelLink.parentFeatureViewVersion) &&
        parentTrainingDatasetVersion.equals(modelLink.parentTrainingDatasetVersion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, model, parentTrainingDataset, parentFeatureStore, parentFeatureViewName,
        parentFeatureViewVersion, parentTrainingDatasetVersion);
  }

  @Override
  public String parentProject() {
    return parentFeatureStore;
  }

  @Override
  public String parentName() {
    return parentFeatureViewName + "_" + parentFeatureViewVersion;
  }

  @Override
  public Integer parentVersion() {
    return parentTrainingDatasetVersion;
  }

  @Override
  public Integer parentId() {
    return parentTrainingDataset != null ? parentTrainingDataset.getId() : null;
  }
}