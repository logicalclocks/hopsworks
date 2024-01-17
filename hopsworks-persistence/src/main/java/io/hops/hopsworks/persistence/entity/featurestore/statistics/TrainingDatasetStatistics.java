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
package io.hops.hopsworks.persistence.entity.featurestore.statistics;

import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;
import java.util.Date;

/**
 * Entity class representing the training_dataset_statistics table in Hopsworks database.
 * An instance of this class represents a row in the database.
 */
@Entity
@Table(name = "training_dataset_statistics", catalog = "hopsworks")
@XmlRootElement
@NamedQueries(
  {@NamedQuery(name = "TrainingDatasetStatistics.findAll", query = "SELECT tds FROM TrainingDatasetStatistics tds"),
    @NamedQuery(name = "TrainingDatasetStatistics.findById",
      query = "SELECT tds FROM TrainingDatasetStatistics tds WHERE tds.id = :id")})
public class TrainingDatasetStatistics extends EntityStatistics {
  
  @JoinTable(name = "hopsworks.training_dataset_descriptive_statistics",
    joinColumns = {@JoinColumn(name = "training_dataset_statistics_id", referencedColumnName = "id")},
    inverseJoinColumns = {@JoinColumn(name = "feature_descriptive_statistics_id", referencedColumnName = "id")})
  @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
  private Collection<FeatureDescriptiveStatistics> trainFeatureDescriptiveStatistics;
  
  @JoinTable(name = "hopsworks.test_dataset_descriptive_statistics",
    joinColumns = {@JoinColumn(name = "training_dataset_statistics_id", referencedColumnName = "id")},
    inverseJoinColumns = {@JoinColumn(name = "feature_descriptive_statistics_id", referencedColumnName = "id")})
  @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
  private Collection<FeatureDescriptiveStatistics> testFeatureDescriptiveStatistics;
  
  @JoinTable(name = "hopsworks.val_dataset_descriptive_statistics",
    joinColumns = {@JoinColumn(name = "training_dataset_statistics_id", referencedColumnName = "id")},
    inverseJoinColumns = {@JoinColumn(name = "feature_descriptive_statistics_id", referencedColumnName = "id")})
  @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
  private Collection<FeatureDescriptiveStatistics> valFeatureDescriptiveStatistics;
  
  @JoinColumn(name = "training_dataset_id", referencedColumnName = "id")
  private TrainingDataset trainingDataset;
  
  @Basic(optional = false)
  @Column(name = "before_transformation")
  private boolean beforeTransformation;
  
  public TrainingDatasetStatistics() {
  }
  
  public TrainingDatasetStatistics(Date commitTime, Float rowPercentage,
    Collection<FeatureDescriptiveStatistics> featureDescriptiveStatistics, TrainingDataset trainingDataset) {
    super(commitTime, rowPercentage);
    this.trainFeatureDescriptiveStatistics = featureDescriptiveStatistics;
    this.trainingDataset = trainingDataset;
  }
  
  public TrainingDatasetStatistics(Date commitTime, Float rowPercentage,
    Collection<FeatureDescriptiveStatistics> trainFeatureDescriptiveStatistics,
    Collection<FeatureDescriptiveStatistics> testFeatureDescriptiveStatistics,
    Collection<FeatureDescriptiveStatistics> valFeatureDescriptiveStatistics, TrainingDataset trainingDataset) {
    super(commitTime, rowPercentage);
    this.trainFeatureDescriptiveStatistics = trainFeatureDescriptiveStatistics;
    this.valFeatureDescriptiveStatistics = valFeatureDescriptiveStatistics;
    this.testFeatureDescriptiveStatistics = testFeatureDescriptiveStatistics;
    this.trainingDataset = trainingDataset;
  }
  
  public Collection<FeatureDescriptiveStatistics> getTrainFeatureDescriptiveStatistics() {
    return trainFeatureDescriptiveStatistics;
  }
  
  public void setTrainFeatureDescriptiveStatistics(
    Collection<FeatureDescriptiveStatistics> featureDescriptiveStatistics) {
    this.trainFeatureDescriptiveStatistics = featureDescriptiveStatistics;
  }
  
  public Collection<FeatureDescriptiveStatistics> getTestFeatureDescriptiveStatistics() {
    return testFeatureDescriptiveStatistics;
  }
  
  public void setTestFeatureDescriptiveStatistics(
    Collection<FeatureDescriptiveStatistics> featureDescriptiveStatistics) {
    this.testFeatureDescriptiveStatistics = featureDescriptiveStatistics;
  }
  
  public Collection<FeatureDescriptiveStatistics> getValFeatureDescriptiveStatistics() {
    return valFeatureDescriptiveStatistics;
  }
  
  public void setValFeatureDescriptiveStatistics(
    Collection<FeatureDescriptiveStatistics> featureDescriptiveStatistics) {
    this.valFeatureDescriptiveStatistics = featureDescriptiveStatistics;
  }
  
  public TrainingDataset getTrainingDataset() {
    return trainingDataset;
  }
  
  public void setTrainingDataset(TrainingDataset trainingDataset) {
    this.trainingDataset = trainingDataset;
  }
  
  public boolean getForTransformation() {
    return beforeTransformation;
  }
  
  public void setForTransformation(boolean beforeTransformation) {
    this.beforeTransformation = beforeTransformation;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    
    TrainingDatasetStatistics that = (TrainingDatasetStatistics) o;
    
    if (!super.equals(that)) {
      return false;
    }
    if (trainingDataset != null ? trainingDataset.equals(that.trainingDataset) : that.trainingDataset != null) {
      return false;
    }
    if (beforeTransformation != that.beforeTransformation) {
      return false;
    }
    return true;
  }
  
  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (trainingDataset != null ? trainingDataset.hashCode() : 0);
    return result;
  }
}