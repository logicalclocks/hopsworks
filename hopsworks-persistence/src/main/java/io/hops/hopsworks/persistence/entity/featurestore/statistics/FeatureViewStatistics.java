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

import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;

/**
 * Entity class representing the feature_view_statistics table in Hopsworks database.
 * An instance of this class represents a row in the database.
 */
@Entity
@Table(name = "feature_view_statistics", catalog = "hopsworks")
@XmlRootElement
@NamedQueries(
  {@NamedQuery(name = "FeatureViewStatistics.findAll", query = "SELECT fgs FROM FeatureViewStatistics fgs"),
    @NamedQuery(name = "FeatureViewStatistics.findById",
      query = "SELECT s FROM FeatureViewStatistics s WHERE s.id = :id")})
public class FeatureViewStatistics extends EntityStatistics {
  
  @JoinTable(name = "hopsworks.feature_view_descriptive_statistics",
    joinColumns = {@JoinColumn(name = "feature_view_statistics_id", referencedColumnName = "id")},
    inverseJoinColumns = {@JoinColumn(name = "feature_descriptive_statistics_id", referencedColumnName = "id")})
  @ManyToMany(fetch = FetchType.LAZY)
  private Collection<FeatureDescriptiveStatistics> featureDescriptiveStatistics;
  
  @JoinColumn(name = "feature_view_id", referencedColumnName = "id")
  private FeatureView featureView;
  
  @Basic(optional = false)
  @Column(name = "window_start_commit_time")
  @NotNull
  private Long windowStartCommitTime;
  
  @Basic(optional = false)
  @Column(name = "window_end_commit_time")
  @NotNull
  private Long windowEndCommitTime;
  
  public FeatureViewStatistics() {
  }
  
  // statistics of joined feature groups with time-travel disabled.
  
  public FeatureViewStatistics(Date computationTime, Float rowPercentage,
    Collection<FeatureDescriptiveStatistics> featureDescriptiveStatistics, FeatureView featureView) {
    // statistics computed on the whole joined feature group data
    super(computationTime, rowPercentage);
    this.featureView = featureView;
    this.featureDescriptiveStatistics = featureDescriptiveStatistics;
    this.windowStartCommitTime = 0L;
    this.windowEndCommitTime = computationTime.getTime();
  }
  
  // statistics of joined feature group with time-travel enabled
  
  public FeatureViewStatistics(Date computationTime, Long windowStartCommitTime, Long windowEndCommitTime,
    Float rowPercentage, Collection<FeatureDescriptiveStatistics> featureDescriptiveStatistics,
    FeatureView featureView) {
    // statistics computed on joined feature group data for a specific commit window or a specific commit (i.e., start
    // time = null)
    super(computationTime, rowPercentage);
    this.featureView = featureView;
    this.featureDescriptiveStatistics = featureDescriptiveStatistics;
    this.windowStartCommitTime = windowStartCommitTime;
    this.windowEndCommitTime = windowEndCommitTime;
  }
  
  public Collection<FeatureDescriptiveStatistics> getFeatureDescriptiveStatistics() {
    return featureDescriptiveStatistics;
  }
  
  public void setFeatureDescriptiveStatistics(Collection<FeatureDescriptiveStatistics> featureDescriptiveStatistics) {
    this.featureDescriptiveStatistics = featureDescriptiveStatistics;
  }
  
  public FeatureView getFeatureView() {
    return featureView;
  }
  
  public void setFeatureView(FeatureView featureView) {
    this.featureView = featureView;
  }
  
  public Long getWindowStartCommitTime() {
    return windowStartCommitTime;
  }
  
  public void setWindowStartCommitTime(Long windowStartCommitTime) {
    this.windowStartCommitTime = windowStartCommitTime;
  }
  
  public Long getWindowEndCommitTime() {
    return windowEndCommitTime;
  }
  
  public void setWindowEndCommitTime(Long windowEndCommitTime) {
    this.windowEndCommitTime = windowEndCommitTime;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    
    FeatureViewStatistics that = (FeatureViewStatistics) o;
    
    if (!super.equals(that)) {
      return false;
    }
    if (!Objects.equals(featureView, that.featureView)) {
      return false;
    }
    if (!Objects.equals(windowStartCommitTime, that.windowStartCommitTime)) {
      return false;
    }
    if (!Objects.equals(windowEndCommitTime, that.windowEndCommitTime)) {
      return false;
    }
    return true;
  }
  
  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (featureView != null ? featureView.hashCode() : 0);
    result = 31 * result + (windowStartCommitTime != null ? windowStartCommitTime.hashCode() : 0);
    result = 31 * result + (windowEndCommitTime != null ? windowEndCommitTime.hashCode() : 0);
    return result;
  }
}