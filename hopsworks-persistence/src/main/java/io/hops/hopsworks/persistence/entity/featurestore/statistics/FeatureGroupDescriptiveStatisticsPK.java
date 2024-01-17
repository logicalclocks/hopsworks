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
public class FeatureGroupDescriptiveStatisticsPK implements Serializable {
  
  @Basic(optional = false)
  @NotNull
  @Column(name = "feature_group_statistics_id")
  private Integer featureGroupStatisticsId;
  
  @Basic(optional = false)
  @NotNull
  @Column(name = "feature_descriptive_statistics_id")
  private Integer featureDescriptiveStatisticsId;
  
  public FeatureGroupDescriptiveStatisticsPK() {
  }
  
  public FeatureGroupDescriptiveStatisticsPK(Integer featureGroupStatisticsId, Integer featureDescriptiveStatisticsId) {
    this.featureGroupStatisticsId = featureGroupStatisticsId;
    this.featureDescriptiveStatisticsId = featureDescriptiveStatisticsId;
  }
  
  public Integer getFeatureGroupStatisticsId() {
    return featureGroupStatisticsId;
  }
  
  public void setFeatureGroupStatisticsId(Integer featureGroupStatisticsId) {
    this.featureGroupStatisticsId = featureGroupStatisticsId;
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
    hash += featureGroupStatisticsId.hashCode();
    hash += featureDescriptiveStatisticsId.hashCode();
    return hash;
  }
  
  @Override
  public boolean equals(Object object) {
    if (!(object instanceof FeatureGroupDescriptiveStatisticsPK)) {
      return false;
    }
    FeatureGroupDescriptiveStatisticsPK other = (FeatureGroupDescriptiveStatisticsPK) object;
    if (!this.featureGroupStatisticsId.equals(other.featureGroupStatisticsId)) {
      return false;
    }
    if (!this.featureDescriptiveStatisticsId.equals(other.featureDescriptiveStatisticsId)) {
      return false;
    }
    return true;
  }
}