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

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Embeddable
public class FeatureViewDescriptiveStatisticsPK implements Serializable {
  
  @Basic(optional = false)
  @NotNull
  @Column(name = "feature_view_statistics_id")
  private Integer featureViewStatisticsId;
  
  @Basic(optional = false)
  @NotNull
  @Column(name = "feature_descriptive_statistics_id")
  private Integer featureDescriptiveStatisticsId;
  
  public FeatureViewDescriptiveStatisticsPK() {
  }
  
  public FeatureViewDescriptiveStatisticsPK(Integer featureViewStatisticsId, Integer featureDescriptiveStatisticsId) {
    this.featureViewStatisticsId = featureViewStatisticsId;
    this.featureDescriptiveStatisticsId = featureDescriptiveStatisticsId;
  }
  
  public Integer getFeatureViewStatisticsId() {
    return featureViewStatisticsId;
  }
  
  public void setFeatureViewStatisticsId(Integer featureViewStatisticsId) {
    this.featureViewStatisticsId = featureViewStatisticsId;
  }
  
  public Integer getFeatureDescriptiveStatisticsId() {
    return featureDescriptiveStatisticsId;
  }
  
  public void setFeatureDescriptiveStatisticsId(Integer featureDescriptiveStatisticsId) {
    this.featureDescriptiveStatisticsId = featureDescriptiveStatisticsId;
  }
  
  @Override
  public int hashCode() {
    int hash = 0;
    hash += featureViewStatisticsId.hashCode();
    hash += featureDescriptiveStatisticsId.hashCode();
    return hash;
  }
  
  @Override
  public boolean equals(Object object) {
    if (!(object instanceof FeatureViewDescriptiveStatisticsPK)) {
      return false;
    }
    FeatureViewDescriptiveStatisticsPK other = (FeatureViewDescriptiveStatisticsPK) object;
    if (!this.featureViewStatisticsId.equals(other.featureViewStatisticsId)) {
      return false;
    }
    if (!this.featureDescriptiveStatisticsId.equals(other.featureDescriptiveStatisticsId)) {
      return false;
    }
    return true;
  }
}