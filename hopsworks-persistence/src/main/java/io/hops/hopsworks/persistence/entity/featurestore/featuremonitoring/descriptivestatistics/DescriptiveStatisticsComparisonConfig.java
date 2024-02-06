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

package io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.descriptivestatistics;

import io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.config.FeatureMonitoringConfiguration;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "statistics_comparison_config", catalog = "hopsworks")
@XmlRootElement
public class DescriptiveStatisticsComparisonConfig implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  
  @OneToOne(mappedBy = "dsComparisonConfig")
  private FeatureMonitoringConfiguration featureMonitoringConfig;
  
  @Basic
  @Column(name = "threshold")
  private Double threshold;
  
  @Basic
  @Column(name = "relative") // apply treshold to relative or absolute difference
  private Boolean relative;
  
  @Basic
  @Column(name = "strict")
  private Boolean strict;
  
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "metric")
  private MetricDescriptiveStatistics metric;
  
  public Integer getId() {
    return this.id;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  public Double getThreshold() {
    return this.threshold;
  }
  
  public void setThreshold(Double threshold) {
    this.threshold = threshold;
  }
  
  public Boolean getRelative() {
    return this.relative;
  }
  
  public void setRelative(Boolean relative) {
    this.relative = relative;
  }
  
  public Boolean getStrict() {
    return this.strict;
  }
  
  public void setStrict(Boolean strict) {
    this.strict = strict;
  }
  
  public MetricDescriptiveStatistics getMetric() {
    return this.metric;
  }
  
  public void setMetric(MetricDescriptiveStatistics metric) {
    this.metric = metric;
  }
  
  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof DescriptiveStatisticsComparisonConfig)) {
      return false;
    }
    DescriptiveStatisticsComparisonConfig descriptiveStatisticsComparisonConfig =
      (DescriptiveStatisticsComparisonConfig) o;
    return Objects.equals(id, descriptiveStatisticsComparisonConfig.id) &&
      Objects.equals(threshold, descriptiveStatisticsComparisonConfig.threshold) &&
      Objects.equals(relative, descriptiveStatisticsComparisonConfig.relative) &&
      Objects.equals(strict, descriptiveStatisticsComparisonConfig.strict) &&
      Objects.equals(metric, descriptiveStatisticsComparisonConfig.metric);
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(id, threshold, strict, relative, metric);
  }
}