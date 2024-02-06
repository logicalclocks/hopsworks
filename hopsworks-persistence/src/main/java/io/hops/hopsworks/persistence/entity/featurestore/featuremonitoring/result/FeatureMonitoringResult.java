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

package io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.result;

import io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.config.FeatureMonitoringConfiguration;
import io.hops.hopsworks.persistence.entity.featurestore.statistics.FeatureDescriptiveStatistics;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@Entity
@Table(name = "feature_monitoring_result", catalog = "hopsworks")
@NamedQueries({@NamedQuery(name = "FeatureMonitoringResult.findByResultId",
  query = "SELECT result FROM FeatureMonitoringResult result WHERE result.id=:resultId")})
@XmlRootElement
public class FeatureMonitoringResult implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  
  @JoinColumn(name = "feature_monitoring_config_id", nullable = false, referencedColumnName = "id")
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  private FeatureMonitoringConfiguration featureMonitoringConfig;
  
  @Basic
  @Column(name = "execution_id")
  @NotNull
  private Integer executionId;
  
  @Basic(optional = false)
  @Column(name = "detection_stats_id", updatable = false)
  @NotNull
  private Integer detectionStatsId;
  
  @Basic
  @Column(name = "reference_stats_id", updatable = false)
  private Integer referenceStatsId;
  
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "detection_stats_id", referencedColumnName = "id", updatable = false, insertable = false)
  private FeatureDescriptiveStatistics detectionStatistics;
  
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reference_stats_id", referencedColumnName = "id", updatable = false, insertable = false)
  private FeatureDescriptiveStatistics referenceStatistics;
  
  @Basic
  @Column(name = "difference")
  private Double difference;
  
  @Basic
  @Column(name = "specific_value")
  private Double specificValue;
  
  @Basic
  @Column(name = "monitoring_time")
  @Temporal(TemporalType.TIMESTAMP)
  @NotNull
  private Date monitoringTime;
  
  @Basic
  @Column(name = "shift_detected")
  private Boolean shiftDetected;
  
  @Basic
  @Column(name = "feature_name")
  @Size(max = 63)
  private String featureName;
  
  @Basic
  @Column(name = "raised_exception")
  private Boolean raisedException;
  
  @Basic
  @Column(name = "empty_detection_window")
  private Boolean emptyDetectionWindow;
  
  @Basic
  @Column(name = "empty_reference_window")
  private Boolean emptyReferenceWindow;
  
  // additional config field that are editable could be added, e.g threshold
  public Integer getId() { return id; }
  
  public void setId(Integer id) { this.id = id; }
  
  public String getFeatureName() { return featureName; }
  
  public void setFeatureName(String featureName) { this.featureName = featureName; }
  
  public FeatureMonitoringConfiguration getFeatureMonitoringConfig() { return featureMonitoringConfig; }
  
  public void setFeatureMonitoringConfig(FeatureMonitoringConfiguration featureMonitoringConfig) {
    this.featureMonitoringConfig = featureMonitoringConfig;
  }
  
  public Integer getExecutionId() {
    return executionId;
  }
  
  public void setExecutionId(Integer executionId) {
    this.executionId = executionId;
  }
  
  public Integer getDetectionStatsId() {
    return detectionStatsId;
  }
  
  public void setDetectionStatsId(Integer detectionStatsId) {
    this.detectionStatsId = detectionStatsId;
  }
  
  public Integer getReferenceStatsId() {
    return referenceStatsId;
  }
  
  public void setReferenceStatsId(Integer referenceStatsId) {
    this.referenceStatsId = referenceStatsId;
  }
  
  public FeatureDescriptiveStatistics getDetectionStatistics() { return detectionStatistics; }
  
  public FeatureDescriptiveStatistics getReferenceStatistics() { return referenceStatistics; }
  
  public Double getDifference() {
    return difference;
  }
  
  public void setDifference(Double difference) {
    this.difference = difference;
  }
  
  public Double getSpecificValue() {
    return specificValue;
  }
  
  public void setSpecificValue(Double specificValue) {
    this.specificValue = specificValue;
  }
  
  public Date getMonitoringTime() {
    return monitoringTime;
  }
  
  public void setMonitoringTime(Date monitoringTime) {
    this.monitoringTime = monitoringTime;
  }
  
  public Boolean getShiftDetected() {
    return shiftDetected;
  }
  
  public void setShiftDetected(Boolean shiftDetected) {
    this.shiftDetected = shiftDetected;
  }
  
  public Boolean getRaisedException() {
    return raisedException;
  }
  
  public void setRaisedException(Boolean raisedException) {
    this.raisedException = raisedException;
  }
  
  public Boolean getEmptyDetectionWindow() {
    return emptyDetectionWindow;
  }
  
  public void setEmptyDetectionWindow(Boolean emptyDetectionWindow) {
    this.emptyDetectionWindow = emptyDetectionWindow;
  }
  
  public Boolean getEmptyReferenceWindow() {
    return emptyReferenceWindow;
  }
  
  public void setEmptyReferenceWindow(Boolean emptyReferenceWindow) {
    this.emptyReferenceWindow = emptyReferenceWindow;
  }
  
  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof FeatureMonitoringResult)) {
      return false;
    }
    FeatureMonitoringResult featureMonitoringResult = (FeatureMonitoringResult) o;
    return Objects.equals(id, featureMonitoringResult.id) &&
      Objects.equals(featureMonitoringConfig.getId(), featureMonitoringResult.featureMonitoringConfig.getId()) &&
      Objects.equals(detectionStatsId, featureMonitoringResult.detectionStatsId) &&
      Objects.equals(referenceStatsId, featureMonitoringResult.referenceStatsId) &&
      Objects.equals(difference, featureMonitoringResult.difference) &&
      Objects.equals(specificValue, featureMonitoringResult.specificValue) &&
      Objects.equals(shiftDetected, featureMonitoringResult.shiftDetected) &&
      Objects.equals(featureName, featureMonitoringResult.featureName) &&
      Objects.equals(monitoringTime, featureMonitoringResult.monitoringTime) &&
      Objects.equals(raisedException, featureMonitoringResult.raisedException) &&
      Objects.equals(emptyDetectionWindow, featureMonitoringResult.emptyDetectionWindow) &&
      Objects.equals(emptyReferenceWindow, featureMonitoringResult.emptyReferenceWindow);
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(id, difference, specificValue, monitoringTime, shiftDetected, raisedException,
      emptyDetectionWindow, emptyReferenceWindow);
  }
  
}