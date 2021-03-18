/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Collection;

/**
 * Entity class representing the statistics_config table in Hopsworks database.
 * An instance of this class represents a row in the database.
 */
@Entity
@Table(name = "statistics_config", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "StatisticConfig.findAll", query = "SELECT statsconfig FROM StatisticsConfig statsconfig"),
  @NamedQuery(name = "StatisticConfig.findById",
    query = "SELECT statsconfig FROM StatisticsConfig statsconfig WHERE statsconfig.id = :id"),
  @NamedQuery(name = "StatisticColumn.findByFeatureGroup",
    query = "SELECT statsconfig FROM StatisticsConfig statsconfig WHERE statsconfig.featuregroup =" +
      ":feature_group"),
  @NamedQuery(name = "StatisticColumn.findByTrainingDataset",
    query = "SELECT statsconfig FROM StatisticsConfig statsconfig WHERE statsconfig.trainingDataset =" +
      ":training_dataset")})
public class StatisticsConfig implements Serializable {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  
  @JoinColumn(name = "feature_group_id", referencedColumnName = "id")
  private Featuregroup featuregroup;
  @JoinColumn(name = "training_dataset_id", referencedColumnName = "id")
  private TrainingDataset trainingDataset;
  
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "statisticsConfig")
  private Collection<StatisticColumn> statisticColumns;
  
  @Basic(optional = false)
  @Column(name = "descriptive")
  private boolean descriptive;
  @Basic(optional = false)
  @Column(name = "correlations")
  private boolean correlations;
  @Basic(optional = false)
  @Column(name = "histograms")
  private boolean histograms;
  
  public StatisticsConfig() {}

  public StatisticsConfig(boolean descriptive, boolean correlations, boolean histograms) {
    this.descriptive = descriptive;
    this.histograms = histograms;
    this.correlations = correlations;
  }
  
  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  public Featuregroup getFeaturegroup() {
    return featuregroup;
  }
  
  public void setFeaturegroup(Featuregroup featuregroup) {
    this.featuregroup = featuregroup;
  }
  
  public TrainingDataset getTrainingDataset() {
    return trainingDataset;
  }
  
  public void setTrainingDataset(
    TrainingDataset trainingDataset) {
    this.trainingDataset = trainingDataset;
  }
  
  public Collection<StatisticColumn> getStatisticColumns() {
    return statisticColumns;
  }
  
  public void setStatisticColumns(
    Collection<StatisticColumn> statisticColumns) {
    this.statisticColumns = statisticColumns;
  }
  
  public boolean isDescriptive() {
    return descriptive;
  }
  
  public void setDescriptive(boolean descriptive) {
    this.descriptive = descriptive;
  }
  
  public boolean isCorrelations() {
    return correlations;
  }
  
  public void setCorrelations(boolean correlations) {
    this.correlations = correlations;
  }
  
  public boolean isHistograms() {
    return histograms;
  }
  
  public void setHistograms(boolean featHistEnabled) {
    this.histograms = featHistEnabled;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    
    StatisticsConfig that = (StatisticsConfig) o;
    
    if (descriptive != that.descriptive) {
      return false;
    }
    if (correlations != that.correlations) {
      return false;
    }
    if (histograms != that.histograms) {
      return false;
    }
    if (!id.equals(that.id)) {
      return false;
    }
    if (featuregroup != null ? !featuregroup.equals(that.featuregroup) : that.featuregroup != null) {
      return false;
    }
    if (trainingDataset != null ? !trainingDataset.equals(that.trainingDataset) : that.trainingDataset != null) {
      return false;
    }
    return statisticColumns != null ? statisticColumns.equals(that.statisticColumns) : that.statisticColumns == null;
  }
  
  @Override
  public int hashCode() {
    int result = id.hashCode();
    result = 31 * result + (featuregroup != null ? featuregroup.hashCode() : 0);
    result = 31 * result + (trainingDataset != null ? trainingDataset.hashCode() : 0);
    result = 31 * result + (statisticColumns != null ? statisticColumns.hashCode() : 0);
    result = 31 * result + (descriptive ? 1 : 0);
    result = 31 * result + (correlations ? 1 : 0);
    result = 31 * result + (histograms ? 1 : 0);
    return result;
  }
}
