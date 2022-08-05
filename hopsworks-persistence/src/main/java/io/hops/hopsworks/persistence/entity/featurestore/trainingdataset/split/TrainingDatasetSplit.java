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

package io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.split;

import io.hops.hopsworks.common.featurestore.trainingdatasets.split.SplitType;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * Entity class representing the training_dataset_split table in Hopsworks database.
 * An instance of this class represents a row in the database.
 */
@Entity
@Table(name = "training_dataset_split", catalog = "hopsworks", uniqueConstraints={@UniqueConstraint(columnNames={
  "training_dataset_id", "name"})})
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "TrainingDatasetSplit.findAll", query = "SELECT split FROM TrainingDatasetSplit split"),
  @NamedQuery(name = "TrainingDatasetSplit.findById",
    query = "SELECT split FROM TrainingDatasetSplit split WHERE split.id = :id"),
  @NamedQuery(name = "TrainingDatasetSplit.findByTrainingDataset",
    query = "SELECT split FROM TrainingDatasetSplit split WHERE split.trainingDataset = :training_dataset")})
public class TrainingDatasetSplit implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @JoinColumn(name = "training_dataset_id", referencedColumnName = "id")
  private TrainingDataset trainingDataset;
  @Column(name = "name")
  @Basic(optional = false)
  private String name;
  @Column(name = "percentage")
  @Basic(optional = true)
  private Float percentage;
  @Column(name = "split_type")
  @Enumerated(EnumType.STRING)
  @Basic(optional = true)
  private SplitType splitType;
  @Basic
  @Column(name = "start_time")
  @Temporal(TemporalType.TIMESTAMP)
  private Date startTime;
  @Basic
  @Column(name = "end_time")
  @Temporal(TemporalType.TIMESTAMP)
  private Date endTime;
  
  public TrainingDatasetSplit() {
  }
  
  public TrainingDatasetSplit(TrainingDataset trainingDataset, String name, Float percentage) {
    this.trainingDataset = trainingDataset;
    this.splitType = SplitType.RANDOM_SPLIT;
    this.name = name;
    this.percentage = percentage;
  }

  public TrainingDatasetSplit(TrainingDataset trainingDataset, String name, Date startTime, Date endTime) {
    this.trainingDataset = trainingDataset;
    this.name = name;
    this.startTime = startTime;
    this.endTime = endTime;
    this.splitType = SplitType.TIME_SERIES_SPLIT;
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
  
  public TrainingDataset getTrainingDataset() {
    return trainingDataset;
  }
  
  public void setTrainingDataset(
    TrainingDataset trainingDataset) {
    this.trainingDataset = trainingDataset;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public Float getPercentage() {
    return percentage;
  }
  
  public void setPercentage(Float percentage) {
    this.percentage = percentage;
  }

  public SplitType getSplitType() {
    return splitType;
  }

  public void setSplitType(SplitType splitType) {
    this.splitType = splitType;
  }

  public Date getStartTime() {
    return startTime;
  }

  public void setStartTime(Date startTime) {
    this.startTime = startTime;
  }

  public Date getEndTime() {
    return endTime;
  }

  public void setEndTime(Date endTime) {
    this.endTime = endTime;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TrainingDatasetSplit that = (TrainingDatasetSplit) o;
    return id.equals(that.id) && trainingDataset.getId().equals(that.trainingDataset.getId())
        && name.equals(that.name) &&
        Objects.equals(percentage, that.percentage) && splitType == that.splitType &&
        Objects.equals(startTime, that.startTime) && Objects.equals(endTime, that.endTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, trainingDataset.getId(), name, percentage, splitType, startTime, endTime);
  }
}
