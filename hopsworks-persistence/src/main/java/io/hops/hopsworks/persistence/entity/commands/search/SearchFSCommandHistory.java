/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.persistence.entity.commands.search;

import io.hops.hopsworks.persistence.entity.commands.CommandHistory;
import io.hops.hopsworks.persistence.entity.commands.CommandStatus;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "command_search_fs_history",
       catalog = "hopsworks",
       schema = "")
@XmlRootElement
public class SearchFSCommandHistory implements CommandHistory, Serializable {
  public static final String TABLE_NAME = "SearchFSCommandHistory";
  private static final long serialVersionUID = 1L;
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "h_id")
  private Long historyId;
  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "executed", insertable = false)
  private Date executed;
  
  @Basic(optional = false)
  @Column(name = "id")
  private Long id;
  @Column(name = "project_id")
  private Integer projectId;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 20)
  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private CommandStatus status;
  @Size(max = 10000)
  @Column(name = "error_message")
  private String errorMsg="";
  
  @Column(name = "feature_group_id")
  private Integer featureGroupId;
  @Column(name = "feature_view_id")
  private Integer featureViewId;
  @Column(name = "training_dataset_id")
  private Integer trainingDatasetId;
  
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 20)
  @Column(name = "op")
  @Enumerated(EnumType.STRING)
  private SearchFSCommandOp op;
  @Basic(optional = false)
  @NotNull
  @Column(name = "inode_id")
  private Long inodeId;
  
  public SearchFSCommandHistory() {
  }
  
  public SearchFSCommandHistory(SearchFSCommand command) {
    this.id = command.getId();
    this.projectId = command.getProject() != null ? command.getProject().getId() : null;
    this.status = command.getStatus();
    this.errorMsg = command.getErrorMsg();
    this.featureGroupId = command.getFeatureGroup() != null ? command.getFeatureGroup().getId() : null;
    this.featureViewId = command.getFeatureView() != null ? command.getFeatureView().getId() : null;
    this.trainingDatasetId = command.getTrainingDataset() != null ? command.getTrainingDataset().getId() : null;
    this.op = command.getOp();
    this.inodeId = command.getInodeId();
  }
  
  @Override
  public Long getHistoryId() {
    return historyId;
  }
  
  public void setHistoryId(Long historyId) {
    this.historyId = historyId;
  }
  
  @Override
  public Date getExecuted() {
    return executed;
  }
  
  public void setExecuted(Date executed) {
    this.executed = executed;
  }
  
  public Long getId() {
    return id;
  }
  
  public void setId(Long id) {
    this.id = id;
  }
  
  public Integer getProjectId() {
    return projectId;
  }
  
  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }
  
  public CommandStatus getStatus() {
    return status;
  }
  
  public void setStatus(CommandStatus status) {
    this.status = status;
  }
  
  public String getErrorMsg() {
    return errorMsg;
  }
  
  public void setErrorMsg(String errorMsg) {
    this.errorMsg = errorMsg;
  }
  
  public Integer getFeatureGroupId() {
    return featureGroupId;
  }
  
  public void setFeatureGroupId(Integer featureGroupId) {
    this.featureGroupId = featureGroupId;
  }
  
  public Integer getFeatureViewId() {
    return featureViewId;
  }
  
  public void setFeatureViewId(Integer featureViewId) {
    this.featureViewId = featureViewId;
  }
  
  public Integer getTrainingDatasetId() {
    return trainingDatasetId;
  }
  
  public void setTrainingDatasetId(Integer trainingDatasetId) {
    this.trainingDatasetId = trainingDatasetId;
  }
  
  public SearchFSCommandOp getOp() {
    return op;
  }
  
  public void setOp(SearchFSCommandOp op) {
    this.op = op;
  }
  
  public Long getInodeId() {
    return inodeId;
  }
  
  public void setInodeId(Long inodeId) {
    this.inodeId = inodeId;
  }
}
