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
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "feature_descriptive_statistics", catalog = "hopsworks")
@NamedQueries({@NamedQuery(name = "FeatureDescriptiveStatistics.findById",
  query = "SELECT fds FROM FeatureDescriptiveStatistics fds WHERE fds.id = :id"),
  @NamedQuery(name = "FeatureDescriptiveStatistics.deleteBatch",
    query = "DELETE FROM FeatureDescriptiveStatistics fds WHERE fds.id IN :fdsIds")})

@XmlRootElement
public class FeatureDescriptiveStatistics implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  
  @Basic
  @Column(name = "feature_name")
  @NotNull
  private String featureName;
  
  @Basic
  @Column(name = "feature_type")
  private String featureType;
  
  // for any feature type
  
  @Basic
  @Column(name = "count")
  private Long count;
  
  @Basic
  @Column(name = "completeness")
  private Double completeness;
  
  @Basic
  @Column(name = "num_non_null_values")
  private Long numNonNullValues;
  
  @Basic
  @Column(name = "num_null_values")
  private Long numNullValues;
  
  @Basic
  @Column(name = "approx_num_distinct_values")
  private Long approxNumDistinctValues;
  
  // for numerical features
  
  @Basic
  @Column(name = "min")
  private Double min;
  
  @Basic
  @Column(name = "max")
  private Double max;
  
  @Basic
  @Column(name = "sum")
  private Double sum;
  
  @Basic
  @Column(name = "mean")
  private Double mean;
  
  @Basic
  @Column(name = "stddev")
  private Double stddev;
  
  @Basic
  @Column(name = "percentiles")
  @Convert(converter = PercentilesConverter.class)
  private List<Double> percentiles;
  
  // with exact uniqueness
  
  @Basic
  @Column(name = "distinctness")
  private Double distinctness;
  
  @Basic
  @Column(name = "entropy")
  private Double entropy;
  
  @Basic
  @Column(name = "uniqueness")
  private Double uniqueness;
  
  @Basic
  @Column(name = "exact_num_distinct_values")
  private Long exactNumDistinctValues;
  
  // extended statistics (hdfs file): histogram, correlation, kll
  @Basic
  @Column(name = "extended_statistics_path")
  private String extendedStatisticsPath;
  
  @Transient
  private String extendedStatistics;
  
  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  public String getFeatureName() {
    return featureName;
  }
  
  public void setFeatureName(String featureName) {
    this.featureName = featureName;
  }
  
  public String getFeatureType() {
    return featureType;
  }
  
  public void setFeatureType(String featureType) {
    this.featureType = featureType;
  }
  
  public Double getMin() {
    return min;
  }
  
  public void setMin(Double min) {
    this.min = min;
  }
  
  public Double getMax() {
    return max;
  }
  
  public void setMax(Double max) {
    this.max = max;
  }
  
  public Double getSum() {
    return sum;
  }
  
  public void setSum(Double sum) {
    this.sum = sum;
  }
  
  public Long getCount() {
    return count;
  }
  
  public void setCount(Long count) {
    this.count = count;
  }
  
  public Double getMean() {
    return mean;
  }
  
  public void setMean(Double mean) {
    this.mean = mean;
  }
  
  public Double getStddev() {
    return stddev;
  }
  
  public void setStddev(Double stddev) {
    this.stddev = stddev;
  }
  
  public List<Double> getPercentiles() {
    return percentiles;
  }
  
  public void setPercentiles(List<Double> percentiles) {
    this.percentiles = percentiles;
  }
  
  public Double getCompleteness() {
    return completeness;
  }
  
  public void setCompleteness(Double completeness) {
    this.completeness = completeness;
  }
  
  public Long getNumNonNullValues() {
    return numNonNullValues;
  }
  
  public void setNumNonNullValues(Long numNonNullValues) {
    this.numNonNullValues = numNonNullValues;
  }
  
  public Long getNumNullValues() {
    return numNullValues;
  }
  
  public void setNumNullValues(Long numNullValues) {
    this.numNullValues = numNullValues;
  }
  
  public Double getDistinctness() {
    return distinctness;
  }
  
  public void setDistinctness(Double distinctness) {
    this.distinctness = distinctness;
  }
  
  public Double getEntropy() {
    return entropy;
  }
  
  public void setEntropy(Double entropy) {
    this.entropy = entropy;
  }
  
  public Double getUniqueness() {
    return uniqueness;
  }
  
  public void setUniqueness(Double uniqueness) {
    this.uniqueness = uniqueness;
  }
  
  public Long getApproxNumDistinctValues() {
    return approxNumDistinctValues;
  }
  
  public void setApproxNumDistinctValues(Long approxNumDistinctValues) {
    this.approxNumDistinctValues = approxNumDistinctValues;
  }
  
  public Long getExactNumDistinctValues() {
    return exactNumDistinctValues;
  }
  
  public void setExactNumDistinctValues(Long exactNumDistinctValues) {
    this.exactNumDistinctValues = exactNumDistinctValues;
  }
  
  public String getExtendedStatisticsPath() {
    return extendedStatisticsPath;
  }
  
  public void setExtendedStatisticsPath(String extendedStatisticsPath) {
    this.extendedStatisticsPath = extendedStatisticsPath;
  }
  
  public String getExtendedStatistics() {
    return extendedStatistics;
  }
  
  public void setExtendedStatistics(String extendedStatistics) {
    this.extendedStatistics = extendedStatistics;
  }
  
  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof FeatureDescriptiveStatistics)) {
      return false;
    }
    FeatureDescriptiveStatistics featureDescriptiveStatistics = (FeatureDescriptiveStatistics) o;
    return Objects.equals(id, featureDescriptiveStatistics.id) &&
      Objects.equals(featureName, featureDescriptiveStatistics.getFeatureName()) &&
      Objects.equals(featureType, featureDescriptiveStatistics.getFeatureType()) &&
      Objects.equals(min, featureDescriptiveStatistics.getMin()) &&
      Objects.equals(max, featureDescriptiveStatistics.getMax()) &&
      Objects.equals(sum, featureDescriptiveStatistics.getSum()) &&
      Objects.equals(count, featureDescriptiveStatistics.getCount()) &&
      Objects.equals(mean, featureDescriptiveStatistics.getMean()) &&
      Objects.equals(stddev, featureDescriptiveStatistics.getStddev()) &&
      Objects.equals(percentiles, featureDescriptiveStatistics.getPercentiles()) &&
      Objects.equals(completeness, featureDescriptiveStatistics.getCompleteness()) &&
      Objects.equals(numNonNullValues, featureDescriptiveStatistics.getNumNonNullValues()) &&
      Objects.equals(numNullValues, featureDescriptiveStatistics.getNumNullValues()) &&
      Objects.equals(distinctness, featureDescriptiveStatistics.getDistinctness()) &&
      Objects.equals(entropy, featureDescriptiveStatistics.getEntropy()) &&
      Objects.equals(uniqueness, featureDescriptiveStatistics.getUniqueness()) &&
      Objects.equals(approxNumDistinctValues, featureDescriptiveStatistics.getApproxNumDistinctValues()) &&
      Objects.equals(exactNumDistinctValues, featureDescriptiveStatistics.getExactNumDistinctValues()) &&
      Objects.equals(extendedStatisticsPath, featureDescriptiveStatistics.getExtendedStatisticsPath());
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(id, featureName, featureType, min, max, sum, count, mean, stddev, percentiles, completeness,
      numNonNullValues, numNullValues, distinctness, entropy, uniqueness, approxNumDistinctValues,
      exactNumDistinctValues, extendedStatisticsPath);
  }
}