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

package io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.config;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Objects;


@Entity
@Table(name = "monitoring_window_config", catalog = "hopsworks")
@XmlRootElement
public class MonitoringWindowConfiguration implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "window_config_type")
  private WindowConfigurationType windowConfigType;
  
  // Could think of a way to use these fields with string like "1ingestion" or "2upserts"
  @Basic
  @Column(name = "time_offset")
  @Size(max = 63)
  private String timeOffset; // Duration e.g "1w"
  
  @Basic
  @Column(name = "window_length")
  @Size(max = 63)
  private String windowLength; // Duration e.g "1w" or "1h
  
  @Basic
  @Column(name = "training_dataset_version")
  private Integer trainingDatasetVersion;
  
  @Basic
  @Column(name = "row_percentage")
  private Float rowPercentage;
  
  @Basic
  @Column(name = "specific_value")
  private Double specificValue;
  
  public Integer getId() {
    return this.id;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  public WindowConfigurationType getWindowConfigType() {
    return this.windowConfigType;
  }
  
  public void setWindowConfigType(WindowConfigurationType windowConfigType) {
    this.windowConfigType = windowConfigType;
  }
  
  public String getTimeOffset() {
    return this.timeOffset;
  }
  
  public void setTimeOffset(String timeOffset) {
    this.timeOffset = timeOffset;
  }
  
  public String getWindowLength() {
    return this.windowLength;
  }
  
  public void setWindowLength(String windowLength) {
    this.windowLength = windowLength;
  }
  
  public Integer getTrainingDatasetVersion() {
    return this.trainingDatasetVersion;
  }
  
  public void setTrainingDatasetVersion(Integer trainingDatasetVersion) {
    this.trainingDatasetVersion = trainingDatasetVersion;
  }
  
  public Double getSpecificValue() {
    return this.specificValue;
  }
  
  public void setSpecificValue(Double specificValue) {
    this.specificValue = specificValue;
  }
  
  public Float getRowPercentage() {
    return this.rowPercentage;
  }
  
  public void setRowPercentage(Float rowPercentage) {
    this.rowPercentage = rowPercentage;
  }
  
  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof MonitoringWindowConfiguration)) {
      return false;
    }
    MonitoringWindowConfiguration monitoringWindowConfiguration = (MonitoringWindowConfiguration) o;
    return Objects.equals(id, monitoringWindowConfiguration.id) &&
      Objects.equals(windowConfigType, monitoringWindowConfiguration.windowConfigType) &&
      Objects.equals(timeOffset, monitoringWindowConfiguration.timeOffset) &&
      Objects.equals(windowLength, monitoringWindowConfiguration.windowLength) &&
      Objects.equals(trainingDatasetVersion, monitoringWindowConfiguration.trainingDatasetVersion) &&
      Objects.equals(specificValue, monitoringWindowConfiguration.specificValue) &&
      Objects.equals(rowPercentage, monitoringWindowConfiguration.rowPercentage);
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(id, windowConfigType, timeOffset, windowLength, trainingDatasetVersion, rowPercentage,
      specificValue);
  }
  
}