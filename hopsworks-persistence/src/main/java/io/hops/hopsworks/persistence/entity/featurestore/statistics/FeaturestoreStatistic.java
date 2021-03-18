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

package io.hops.hopsworks.persistence.entity.featurestore.statistics;

import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.FeatureGroupCommit;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;

/**
 * Entity class representing the featurestore_statistic table in Hopsworks database.
 * An instance of this class represents a row in the database.
 */
@Entity
@Table(name = "feature_store_statistic", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "FeaturestoreStatistic.findAll", query = "SELECT fss FROM FeaturestoreStatistic fss"),
    @NamedQuery(name = "FeaturestoreStatistic.findById",
        query = "SELECT fss FROM FeaturestoreStatistic fss WHERE fss.id = :id"),
    @NamedQuery(name = "FeaturestoreStatistic.commitTime",
        query = "SELECT fss FROM FeaturestoreStatistic fss WHERE fss.featureGroup = :featureGroup "
            + "AND fss.commitTime = :commitTime")})
public class FeaturestoreStatistic implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

  @Basic(optional = false)
  @Column(name = "commit_time")
  private Date commitTime;

  @JoinColumns({
      @JoinColumn(name = "inode_pid",
          referencedColumnName = "parent_id"),
      @JoinColumn(name = "inode_name",
          referencedColumnName = "name"),
      @JoinColumn(name = "partition_id",
          referencedColumnName = "partition_id")})
  @ManyToOne(optional = false)
  private Inode inode;

  @JoinColumn(name = "feature_group_id", referencedColumnName = "id")
  private Featuregroup featureGroup;

  @JoinColumns({
      @JoinColumn(name = "feature_group_id",
          referencedColumnName = "feature_group_id", insertable=false, updatable=false),
      @JoinColumn(name = "feature_group_commit_id",
          referencedColumnName = "commit_id")})
  @ManyToOne(optional = false)
  private FeatureGroupCommit featureGroupCommit;

  @JoinColumn(name = "training_dataset_id", referencedColumnName = "id")
  private TrainingDataset trainingDataset;

  public FeaturestoreStatistic() {
  }

  public FeaturestoreStatistic(Date commitTime, Inode inode, TrainingDataset trainingDataset) {
    this.commitTime = commitTime;
    this.inode = inode;
    this.trainingDataset = trainingDataset;
  }
  
  public FeaturestoreStatistic(Date commitTime, Inode inode, Featuregroup featureGroup) {
    this.commitTime = commitTime;
    this.inode = inode;
    this.featureGroup = featureGroup;
  }

  public FeaturestoreStatistic(Date commitTime, Inode inode, Featuregroup featureGroup,
                               FeatureGroupCommit featureGroupCommit) {
    this.commitTime = commitTime;
    this.inode = inode;
    this.featureGroup = featureGroup;
    this.featureGroupCommit = featureGroupCommit;
  }

  public static long getSerialVersionUID() {
    return serialVersionUID;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Date getCommitTime() {
    return this.commitTime;
  }

  public void setCommitTime(Date commitTime) {
    this.commitTime = commitTime;
  }

  public Inode getInode() {
    return inode;
  }

  public void setInode(Inode inode) {
    this.inode = inode;
  }

  public Featuregroup getFeatureGroup() {
    return featureGroup;
  }

  public void setFeatureGroup(Featuregroup featureGroup) {
    this.featureGroup = featureGroup;
  }

  public FeatureGroupCommit getFeatureGroupCommit() {
    return featureGroupCommit;
  }

  public void setFeatureGroupCommit(FeatureGroupCommit featureGroupCommit) {
    this.featureGroupCommit = featureGroupCommit;
  }

  public TrainingDataset getTrainingDataset() {
    return trainingDataset;
  }

  public void setTrainingDataset(TrainingDataset trainingDataset) {
    this.trainingDataset = trainingDataset;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    
    FeaturestoreStatistic that = (FeaturestoreStatistic) o;
    
    if (!id.equals(that.id)) {
      return false;
    }
    if (!commitTime.equals(that.commitTime)) {
      return false;
    }
    if (!inode.equals(that.inode)) {
      return false;
    }
    if (featureGroup != null ? !featureGroup.equals(that.featureGroup) : that.featureGroup != null) {
      return false;
    }
    if (featureGroupCommit != null ? !featureGroupCommit.equals(that.featureGroupCommit)
        : that.featureGroupCommit != null) {
      return false;
    }
    return trainingDataset != null ? trainingDataset.equals(that.trainingDataset) : that.trainingDataset == null;
  }
  
  @Override
  public int hashCode() {
    int result = id.hashCode();
    result = 31 * result + commitTime.hashCode();
    result = 31 * result + inode.hashCode();
    result = 31 * result + (featureGroup != null ? featureGroup.hashCode() : 0);
    result = 31 * result + (featureGroupCommit != null ? featureGroupCommit.hashCode() : 0);
    result = 31 * result + (trainingDataset != null ? trainingDataset.hashCode() : 0);
    return result;
  }
}
