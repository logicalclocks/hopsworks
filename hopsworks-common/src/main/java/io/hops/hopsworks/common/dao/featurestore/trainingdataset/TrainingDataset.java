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

import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.featurestore.Featurestore;
import io.hops.hopsworks.common.dao.featurestore.feature.FeaturestoreFeature;
import io.hops.hopsworks.common.dao.featurestore.stats.FeaturestoreStatistic;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.user.Users;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
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
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
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
  @JoinColumns({
      @JoinColumn(name = "inode_pid",
          referencedColumnName = "parent_id"),
      @JoinColumn(name = "inode_name",
          referencedColumnName = "name"),
      @JoinColumn(name = "partition_id",
          referencedColumnName = "partition_id")})
  @ManyToOne(optional = false)
  private Inode inode;
  @Basic(optional = false)
  @Column(name = "inode_name",
      updatable = false,
      insertable = false)
  private String name;
  @JoinColumn(name = "training_dataset_folder", referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Dataset trainingDatasetFolder;
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

  public Inode getInode() {
    return inode;
  }

  public void setInode(Inode inode) {
    this.inode = inode;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Dataset getTrainingDatasetFolder() {
    return trainingDatasetFolder;
  }

  public void setTrainingDatasetFolder(Dataset trainingDatasetFolder) {
    this.trainingDatasetFolder = trainingDatasetFolder;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TrainingDataset)) return false;

    TrainingDataset that = (TrainingDataset) o;

    if (id != null)
      if (!id.equals(that.id)) return false;
    if (job != null)
      if (!job.equals(that.job)) return false;
    if (!hdfsUserId.equals(that.hdfsUserId)) return false;
    if (!version.equals(that.version)) return false;
    if (!dataFormat.equals(that.dataFormat)) return false;
    if (!inode.equals(that.inode)) return false;
    if (!description.equals(that.description)) return false;
    if (!trainingDatasetFolder.equals(that.trainingDatasetFolder)) return false;
    if (!name.equals(that.name)) return false;
    if (created != null)
      if (!created.equals(that.created)) return false;
    if (!creator.equals(that.creator)) return false;
    if (features != null)
      if (!features.equals(that.features)) return false;
    return featurestore.equals(that.featurestore);
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + featurestore.hashCode();
    result = 31 * result + hdfsUserId.hashCode();
    result = 31 * result + dataFormat.hashCode();
    result = 31 * result + version.hashCode();
    result = 31 * result + inode.hashCode();
    result = 31 * result + description.hashCode();
    result = 31 * result + name.hashCode();
    result = 31 * result + trainingDatasetFolder.hashCode();
    result = 31 * result + (created != null ? created.hashCode() : 0);
    result = 31 * result + (job != null ? job.hashCode() : 0);
    result = 31 * result + (features != null ? features.hashCode() : 0);
    result = 31 * result + creator.hashCode();
    return result;
  }
}
