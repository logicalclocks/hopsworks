/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.persistence.entity.featurestore.jobs;

import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Entity class representing the feature_store_job table in Hopsworks database.
 * An instance of this class represents a row in the database.
 */
@Entity
@Table(name = "feature_store_job", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "FeaturestoreJob.findAll", query = "SELECT fsjob FROM FeaturestoreJob fsjob"),
    @NamedQuery(name = "FeaturestoreJob.findByFeaturegroup", query = "SELECT fsjob FROM FeaturestoreJob fsjob " +
        "WHERE fsjob.featuregroup = :featuregroup "),
    @NamedQuery(name = "FeaturestoreJob.findByTrainingDataset", query = "SELECT fsjob FROM FeaturestoreJob fsjob " +
        "WHERE fsjob.trainingDataset = :trainingDataset"),
    @NamedQuery(name = "FeaturestoreJob.findById",
        query = "SELECT fsjob FROM FeaturestoreJob fsjob WHERE fsjob.id = :id")})
public class FeaturestoreJob implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @JoinColumn(name = "training_dataset_id", referencedColumnName = "id")
  private TrainingDataset trainingDataset;
  @JoinColumn(name = "feature_group_id", referencedColumnName = "id")
  private Featuregroup featuregroup;
  @JoinColumn(name = "job_id", referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Jobs job;

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
  
  public void setTrainingDataset(TrainingDataset trainingDataset) {
    this.trainingDataset = trainingDataset;
  }
  
  public Featuregroup getFeaturegroup() {
    return featuregroup;
  }
  
  public void setFeaturegroup(Featuregroup featuregroup) {
    this.featuregroup = featuregroup;
  }
  
  public Jobs getJob() {
    return job;
  }
  
  public void setJob(Jobs job) {
    this.job = job;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FeaturestoreJob)) {
      return false;
    }
    
    FeaturestoreJob that = (FeaturestoreJob) o;
    
    if (!id.equals(that.id)) {
      return false;
    }
    return job.equals(that.job);
  }
  
  @Override
  public int hashCode() {
    int result = id.hashCode();
    result = 31 * result + job.hashCode();
    return result;
  }
}
