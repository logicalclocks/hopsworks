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

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

/**
 * Entity class representing the feature_view_descriptive_statistics table in Hopsworks database.
 * An instance of this class represents a row in the database.
 */
@Entity
@Table(name = "feature_view_descriptive_statistics", catalog = "hopsworks")
@XmlRootElement
public class FeatureViewDescriptiveStatistics {
  
  @EmbeddedId
  protected FeatureViewDescriptiveStatisticsPK featureViewDescriptiveStatisticsPK;
  
  public FeatureViewDescriptiveStatistics() {
  }
  
  public FeatureViewDescriptiveStatistics(FeatureViewStatistics featureViewStatistics,
    FeatureDescriptiveStatistics featureDescriptiveStatistics) {
    this.featureViewDescriptiveStatisticsPK =
      new FeatureViewDescriptiveStatisticsPK(featureViewStatistics.getId(), featureDescriptiveStatistics.getId());
  }
  
  public Integer getFeatureViewStatisticsId() {
    return featureViewDescriptiveStatisticsPK.getFeatureViewStatisticsId();
  }
  
  public void setFeatureViewStatisticsId(Integer featureViewStatisticsId) {
    this.featureViewDescriptiveStatisticsPK.setFeatureViewStatisticsId(featureViewStatisticsId);
  }
  
  public Integer getFeatureDescriptiveStatisticsId() {
    return this.featureViewDescriptiveStatisticsPK.getFeatureDescriptiveStatisticsId();
  }
  
  public void setFeatureDescriptiveStatisticsId(Integer featureDescriptiveStatisticsId) {
    this.featureViewDescriptiveStatisticsPK.setFeatureDescriptiveStatisticsId(featureDescriptiveStatisticsId);
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    
    FeatureViewDescriptiveStatistics that = (FeatureViewDescriptiveStatistics) o;
    if (!Objects.equals(featureViewDescriptiveStatisticsPK, that.featureViewDescriptiveStatisticsPK)) {
      return false;
    }
    return true;
  }
  
  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + featureViewDescriptiveStatisticsPK.hashCode();
    return result;
  }
}