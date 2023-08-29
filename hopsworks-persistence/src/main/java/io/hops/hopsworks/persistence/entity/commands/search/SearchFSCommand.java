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

import io.hops.hopsworks.persistence.entity.commands.Command;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@Entity
@Table(name = "command_search_fs",
       catalog = "hopsworks",
       schema = "")
@XmlRootElement
public class SearchFSCommand extends Command implements Serializable {
  public static final String TABLE_NAME = "SearchFSCommand";
  
  private static final long serialVersionUID = 1L;
  
  
  @JoinColumn(name = "feature_group_id", referencedColumnName = "id")
  @ManyToOne()
  private Featuregroup featureGroup;
  @JoinColumn(name = "feature_view_id", referencedColumnName = "id")
  @ManyToOne()
  private FeatureView featureView;
  @JoinColumn(name = "training_dataset_id", referencedColumnName = "id")
  @ManyToOne()
  private TrainingDataset trainingDataset;
  
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
  
  public Featuregroup getFeatureGroup() {
    return featureGroup;
  }
  
  public void setFeatureGroup(Featuregroup featureGroup) {
    this.featureGroup = featureGroup;
  }
  
  public FeatureView getFeatureView() {
    return featureView;
  }
  
  public void setFeatureView(FeatureView featureView) {
    this.featureView = featureView;
  }
  
  public TrainingDataset getTrainingDataset() {
    return trainingDataset;
  }
  
  public void setTrainingDataset(
    TrainingDataset trainingDataset) {
    this.trainingDataset = trainingDataset;
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
  
  @Override
  public String toString() {
    return super.toString() + ", op=" + op;
  }
  
  public SearchFSCommand shallowClone() {
    SearchFSCommand clone = new SearchFSCommand();
    clone.setProject(getProject());
    clone.setStatus(getStatus());
    clone.setErrorMsg(getErrorMsg());
    clone.setFeatureGroup(featureGroup);
    clone.setFeatureView(featureView);
    clone.setTrainingDataset(trainingDataset);
    clone.setOp(op);
    clone.setInodeId(inodeId);
    return clone;
  }
}
