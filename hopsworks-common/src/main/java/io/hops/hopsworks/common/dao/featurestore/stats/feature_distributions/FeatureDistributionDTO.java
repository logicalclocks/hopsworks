/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.dao.featurestore.stats.feature_distributions;

import io.hops.hopsworks.common.dao.featurestore.stats.FeaturestoreStatisticType;
import io.hops.hopsworks.common.dao.featurestore.stats.FeaturestoreStatisticValue;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * XML/JSON DTO representing a single feature distribution
 */
@XmlRootElement
@XmlType(propOrder = {"featureName", "frequencyDistribution", "statisticType"})
public class FeatureDistributionDTO extends FeaturestoreStatisticValue {
  

  private String featureName;
  private List<HistogramBinDTO> frequencyDistribution;

  public FeatureDistributionDTO() {
    //Default empty constructor required for JAXB
  }

  @XmlElement
  public String getFeatureName() {
    return featureName;
  }

  @XmlElement
  public List<HistogramBinDTO> getFrequencyDistribution() {
    return frequencyDistribution;
  }

  public void setFeatureName(String featureName) {
    this.featureName = featureName;
  }

  public void setFrequencyDistribution(List<HistogramBinDTO> frequencyDistribution) {
    this.frequencyDistribution = frequencyDistribution;
  }

  @Override
  @XmlElement(name="statisticType")
  public FeaturestoreStatisticType getStatisticType() {
    return FeaturestoreStatisticType.FEATUREDISTRIBUTIONS;
  }

  @Override
  public String toString() {
    return "FeatureDistributionDTO{" +
        "featureName='" + featureName + '\'' +
        ", frequencyDistribution=" + frequencyDistribution +
        '}';
  }




}
