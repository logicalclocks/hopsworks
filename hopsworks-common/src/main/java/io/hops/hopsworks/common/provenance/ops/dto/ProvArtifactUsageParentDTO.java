/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.provenance.ops.dto;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class ProvArtifactUsageParentDTO {
  private Integer projectId;
  private String projectName;
  private Integer datasetId;
  private String datasetName;
  private String artifactId;
  private ProvArtifactUsageDTO readLast;
  private ProvArtifactUsageDTO writeLast;
  private List<ProvArtifactUsageDTO> readCurrent;
  private List<ProvArtifactUsageDTO> writeCurrent;
  private List<ProvArtifactUsageDTO> readHistory;
  private List<ProvArtifactUsageDTO> writeHistory;
  
  public ProvArtifactUsageParentDTO() {
  }
  
  public Integer getProjectId() {
    return projectId;
  }
  
  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }
  
  public String getProjectName() {
    return projectName;
  }
  
  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }
  
  public Integer getDatasetId() {
    return datasetId;
  }
  
  public void setDatasetId(Integer datasetId) {
    this.datasetId = datasetId;
  }
  
  public String getDatasetName() {
    return datasetName;
  }
  
  public void setDatasetName(String datasetName) {
    this.datasetName = datasetName;
  }
  
  public String getArtifactId() {
    return artifactId;
  }
  
  public void setArtifactId(String artifactId) {
    this.artifactId = artifactId;
  }
  
  public ProvArtifactUsageDTO getReadLast() {
    return readLast;
  }
  
  public void setReadLast(ProvArtifactUsageDTO readLast) {
    this.readLast = readLast;
  }
  
  public ProvArtifactUsageDTO getWriteLast() {
    return writeLast;
  }
  
  public void setWriteLast(ProvArtifactUsageDTO writeLast) {
    this.writeLast = writeLast;
  }
  
  public List<ProvArtifactUsageDTO> getReadCurrent() {
    return readCurrent;
  }
  
  public void setReadCurrent(List<ProvArtifactUsageDTO> readCurrent) {
    this.readCurrent = readCurrent;
  }
  
  public List<ProvArtifactUsageDTO> getWriteCurrent() {
    return writeCurrent;
  }
  
  public void setWriteCurrent(List<ProvArtifactUsageDTO> writeCurrent) {
    this.writeCurrent = writeCurrent;
  }
  
  public List<ProvArtifactUsageDTO> getReadHistory() {
    return readHistory;
  }
  
  public void setReadHistory(List<ProvArtifactUsageDTO> readHistory) {
    this.readHistory = readHistory;
  }
  
  public List<ProvArtifactUsageDTO> getWriteHistory() {
    return writeHistory;
  }
  
  public void setWriteHistory(List<ProvArtifactUsageDTO> writeHistory) {
    this.writeHistory = writeHistory;
  }
}
