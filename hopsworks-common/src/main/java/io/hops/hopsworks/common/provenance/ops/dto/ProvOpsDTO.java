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

import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.common.provenance.app.dto.ProvAppStateDTO;
import io.hops.hopsworks.common.provenance.core.ProvParser;
import io.hops.hopsworks.common.provenance.core.Provenance;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ProvOpsDTO extends RestDTO<ProvOpsDTO> {
  private String id;
  private Float score;
  
  private Long inodeId;
  private Provenance.FileOps inodeOperation;
  private String appId;
  private Integer userId;
  private Long parentInodeId;
  private Long projectInodeId;
  private Long datasetInodeId;
  private Integer logicalTime;
  private Long timestamp;
  private String readableTimestamp;
  private String inodeName;
  private String xattrName;
  private String xattrVal;
  private String inodePath;
  private Long partitionId;
  private String projectName;
  private ProvParser.DocSubType docSubType;
  private String mlId;
  private ProvAppStateDTO appState;
  
  private String aggregation;
  
  public ProvOpsDTO() {}
  
  public String getId() {
    return id;
  }
  
  public void setId(String id) {
    this.id = id;
  }
  
  public Float getScore() {
    return score;
  }
  
  public void setScore(Float score) {
    this.score = score;
  }
  
  public Long getInodeId() {
    return inodeId;
  }
  
  public void setInodeId(Long inodeId) {
    this.inodeId = inodeId;
  }
  
  public Provenance.FileOps getInodeOperation() {
    return inodeOperation;
  }
  
  public void setInodeOperation(Provenance.FileOps inodeOperation) {
    this.inodeOperation = inodeOperation;
  }
  
  public String getAppId() {
    return appId;
  }
  
  public void setAppId(String appId) {
    this.appId = appId;
  }
  
  public Integer getLogicalTime() {
    return logicalTime;
  }
  
  public void setLogicalTime(Integer logicalTime) {
    this.logicalTime = logicalTime;
  }
  
  public Long getTimestamp() {
    return timestamp;
  }
  
  public void setTimestamp(Long timestamp) {
    this.timestamp = timestamp;
  }
  
  public String getReadableTimestamp() {
    return readableTimestamp;
  }
  
  public void setReadableTimestamp(String readableTimestamp) {
    this.readableTimestamp = readableTimestamp;
  }
  
  public String getInodeName() {
    return inodeName;
  }
  
  public void setInodeName(String inodeName) {
    this.inodeName = inodeName;
  }
  
  public String getXattrName() {
    return xattrName;
  }
  
  public void setXattrName(String xattrName) {
    this.xattrName = xattrName;
  }
  
  public String getXattrVal() {
    return xattrVal;
  }
  
  public void setXattrVal(String xattrVal) {
    this.xattrVal = xattrVal;
  }
  
  public String getInodePath() {
    return inodePath;
  }
  
  public void setInodePath(String inodePath) {
    this.inodePath = inodePath;
  }
  
  public Long getParentInodeId() {
    return parentInodeId;
  }
  
  public void setParentInodeId(Long parentInodeId) {
    this.parentInodeId = parentInodeId;
  }
  
  public Integer getUserId() {
    return userId;
  }
  
  public void setUserId(Integer userId) {
    this.userId = userId;
  }
  
  public Long getPartitionId() {
    return partitionId;
  }
  
  public void setPartitionId(Long partitionId) {
    this.partitionId = partitionId;
  }
  
  public Long getProjectInodeId() {
    return projectInodeId;
  }
  
  public void setProjectInodeId(Long projectInodeId) {
    this.projectInodeId = projectInodeId;
  }
  
  public ProvAppStateDTO getAppState() {
    return appState;
  }
  
  public void setAppState(ProvAppStateDTO appState) {
    this.appState = appState;
  }
  
  public ProvParser.DocSubType getDocSubType() {
    return docSubType;
  }
  
  public void setDocSubType(ProvParser.DocSubType docSubType) {
    this.docSubType = docSubType;
  }
  
  public String getMlId() {
    return mlId;
  }
  
  public void setMlId(String mlId) {
    this.mlId = mlId;
  }
  
  public String getProjectName() {
    return projectName;
  }
  
  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }
  
  public Long getDatasetInodeId() {
    return datasetInodeId;
  }
  
  public void setDatasetInodeId(Long datasetInodeId) {
    this.datasetInodeId = datasetInodeId;
  }
  
  public ProvOpsDTO upgradePart() {
    switch(docSubType) {
      case HIVE_PART:
        setDocSubType(ProvParser.DocSubType.HIVE); break;
      case FEATURE_PART:
        setDocSubType(ProvParser.DocSubType.FEATURE); break;
      case TRAINING_DATASET_PART:
        setDocSubType(ProvParser.DocSubType.TRAINING_DATASET); break;
      case MODEL_PART:
        setDocSubType(ProvParser.DocSubType.MODEL); break;
      case EXPERIMENT_PART:
        setDocSubType(ProvParser.DocSubType.EXPERIMENT); break;
      case DATASET_PART:
        setDocSubType(ProvParser.DocSubType.DATASET); break;
    }
    return this;
  }
  
  public String getAggregation() {
    return aggregation;
  }
  
  public void setAggregation(String aggregation) {
    this.aggregation = aggregation;
  }
}