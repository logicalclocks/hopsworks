/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.persistence.entity.featurestore.code;

import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.FeatureGroupCommit;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Id;
import javax.persistence.Basic;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Column;
import javax.persistence.JoinColumns;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;

/**
 * Entity class representing the feature_store_code table in Hopsworks database.
 * An instance of this class represents a row in the database.
 */
@Entity
@Table(name = "feature_store_code", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
        @NamedQuery(name = "FeaturestoreCode.findAll", query = "SELECT fsc FROM FeaturestoreCode fsc"),
        @NamedQuery(name = "FeaturestoreCode.findByIdAndTrainingDataset",
                query = "SELECT fsc FROM FeaturestoreCode fsc WHERE fsc.id = :id "
                        + "AND fsc.trainingDataset = :trainingDataset"),
        @NamedQuery(name = "FeaturestoreCode.findByIdAndFeatureGroup",
                query = "SELECT fsc FROM FeaturestoreCode fsc WHERE fsc.id = :id "
                        + "AND fsc.featureGroup = :featureGroup")})
public class FeaturestoreCode implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

  @Basic(optional = false)
  @Column(name = "commit_time")
  private Date commitTime;

  @Basic(optional = true)
  @Column(name = "application_id")
  private String applicationID;

  @Basic(optional = false)
  @Column(name = "name")
  @Size(max = 255)
  private String fileName;

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

  public FeaturestoreCode() {
  }

  public FeaturestoreCode(Date commitTime, TrainingDataset trainingDataset, String fileName, String applicationID) {
    this.commitTime = commitTime;
    this.fileName = fileName;
    this.trainingDataset = trainingDataset;
    this.applicationID = applicationID;
  }

  public FeaturestoreCode(Date commitTime, Featuregroup featureGroup, String fileName, String applicationID) {
    this.commitTime = commitTime;
    this.fileName = fileName;
    this.featureGroup = featureGroup;
    this.applicationID = applicationID;
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

  public String getApplicationID() {
    return this.applicationID;
  }

  public String getFileName() { return fileName; }
  public void setFileName(String fileName) { this.fileName = fileName; }

  public void setApplicationID(String applicationID) {
    this.applicationID = applicationID;
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

    FeaturestoreCode that = (FeaturestoreCode) o;

    if (!id.equals(that.id)) {
      return false;
    }
    if (!commitTime.equals(that.commitTime)) {
      return false;
    }
    if (!fileName.equals(that.fileName)) {
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
    result = 31 * result + fileName.hashCode();
    result = 31 * result + (featureGroup != null ? featureGroup.hashCode() : 0);
    result = 31 * result + (featureGroupCommit != null ? featureGroupCommit.hashCode() : 0);
    result = 31 * result + (trainingDataset != null ? trainingDataset.hashCode() : 0);
    return result;
  }
}
