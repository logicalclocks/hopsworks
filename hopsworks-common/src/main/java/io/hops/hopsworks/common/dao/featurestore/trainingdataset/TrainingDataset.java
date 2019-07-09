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

package io.hops.hopsworks.common.dao.featurestore.trainingdataset;

import io.hops.hopsworks.common.dao.featurestore.Featurestore;
import io.hops.hopsworks.common.dao.featurestore.feature.FeaturestoreFeature;
import io.hops.hopsworks.common.dao.featurestore.stats.FeaturestoreStatistic;
import io.hops.hopsworks.common.dao.featurestore.trainingdataset.external_trainingdataset.ExternalTrainingDataset;
import io.hops.hopsworks.common.dao.featurestore.trainingdataset.hopsfs_trainingdataset.HopsfsTrainingDataset;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.user.Users;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;

/**
 * Entity class representing the training_dataset table in Hopsworks database.
 * An instance of this class represents a row in the database.
 */
@Entity
@Table(name = "training_dataset", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "TrainingDataset.findAll", query = "SELECT td FROM TrainingDataset td"),
    @NamedQuery(name = "TrainingDataset.findById", query = "SELECT td FROM TrainingDataset td WHERE td.id = :id"),
    @NamedQuery(name = "TrainingDataset.findByFeaturestore", query = "SELECT td FROM TrainingDataset td " +
        "WHERE td.featurestore = :featurestore"),
    @NamedQuery(name = "TrainingDataset.findByFeaturestoreAndId", query = "SELECT td FROM TrainingDataset td " +
        "WHERE td.featurestore = :featurestore AND td.id = :id")})
public class TrainingDataset implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @JoinColumn(name = "feature_store_id", referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Featurestore featurestore;
  @JoinColumn(name = "job_id", referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Jobs job;
  @Basic(optional = false)
  @NotNull
  @Column(name = "hdfs_user_id")
  private Integer hdfsUserId;
  @Basic(optional = false)
  @Column(name = "created")
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;
  @JoinColumn(name = "creator", referencedColumnName = "uid")
  @ManyToOne(optional = false)
  private Users creator;
  @Basic(optional = false)
  @NotNull
  @Column(name = "version")
  private Integer version;
  @Basic(optional = false)
  @NotNull
  @Column(name = "data_format")
  private String dataFormat;
  @Basic(optional = false)
  @Column(name = "description")
  private String description;
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "trainingDataset")
  private Collection<FeaturestoreStatistic> statistics;
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "trainingDataset")
  private Collection<FeaturestoreFeature> features;
  @NotNull
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "training_dataset_type")
  private TrainingDatasetType trainingDatasetType = TrainingDatasetType.HOPSFS_TRAINING_DATASET;
  @JoinColumn(name = "hopsfs_training_dataset_id", referencedColumnName = "id")
  @OneToOne
  private HopsfsTrainingDataset hopsfsTrainingDataset;
  @JoinColumn(name = "external_training_dataset_id", referencedColumnName = "id")
  @OneToOne
  private ExternalTrainingDataset externalTrainingDataset;

  public static long getSerialVersionUID() {
    return serialVersionUID;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Featurestore getFeaturestore() {
    return featurestore;
  }

  public void setFeaturestore(Featurestore featurestore) {
    this.featurestore = featurestore;
  }

  public Integer getHdfsUserId() {
    return hdfsUserId;
  }

  public void setHdfsUserId(Integer hdfsUserId) {
    this.hdfsUserId = hdfsUserId;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public Users getCreator() {
    return creator;
  }

  public void setCreator(Users creator) {
    this.creator = creator;
  }

  public Jobs getJob() {
    return job;
  }

  public void setJob(Jobs job) {
    this.job = job;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public String getDataFormat() {
    return dataFormat;
  }

  public void setDataFormat(String dataFormat) {
    this.dataFormat = dataFormat;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Collection<FeaturestoreStatistic> getStatistics() {
    return statistics;
  }

  public void setStatistics(Collection<FeaturestoreStatistic> statistics) {
    this.statistics = statistics;
  }

  public Collection<FeaturestoreFeature> getFeatures() {
    return features;
  }

  public void setFeatures(Collection<FeaturestoreFeature> features) {
    this.features = features;
  }
  
  public HopsfsTrainingDataset getHopsfsTrainingDataset() {
    return hopsfsTrainingDataset;
  }
  
  public void setHopsfsTrainingDataset(
    HopsfsTrainingDataset hopsfsTrainingDataset) {
    this.hopsfsTrainingDataset = hopsfsTrainingDataset;
  }
  
  public ExternalTrainingDataset getExternalTrainingDataset() {
    return externalTrainingDataset;
  }
  
  public void setExternalTrainingDataset(
    ExternalTrainingDataset externalTrainingDataset) {
    this.externalTrainingDataset = externalTrainingDataset;
  }
  
  @XmlElement
  public TrainingDatasetType getTrainingDatasetType() {
    return trainingDatasetType;
  }
  
  public void setTrainingDatasetType(
    TrainingDatasetType trainingDatasetType) {
    this.trainingDatasetType = trainingDatasetType;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TrainingDataset)) return false;

    TrainingDataset that = (TrainingDataset) o;
    
    if (id != null && !id.equals(that.id)) return false;
    if (job != null && !job.equals(that.job)) return false;
    if (!hdfsUserId.equals(that.hdfsUserId)) return false;
    if (!version.equals(that.version)) return false;
    if (!dataFormat.equals(that.dataFormat)) return false;
    if (!description.equals(that.description)) return false;
    if (!trainingDatasetType.equals(that.trainingDatasetType)) return false;
    if (created != null && !created.equals(that.created)) return false;
    if (!creator.equals(that.creator)) return false;
    if (features != null && !features.equals(that.features)) return false;
    return featurestore.equals(that.featurestore);
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + featurestore.hashCode();
    result = 31 * result + hdfsUserId.hashCode();
    result = 31 * result + dataFormat.hashCode();
    result = 31 * result + version.hashCode();
    result = 31 * result + description.hashCode();
    result = 31 * result + trainingDatasetType.hashCode();
    result = 31 * result + (created != null ? created.hashCode() : 0);
    result = 31 * result + (job != null ? job.hashCode() : 0);
    result = 31 * result + (features != null ? features.hashCode() : 0);
    result = 31 * result + creator.hashCode();
    return result;
  }
}
