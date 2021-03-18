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

package io.hops.hopsworks.persistence.entity.featurestore.trainingdataset;

import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.activity.FeaturestoreActivity;
import io.hops.hopsworks.persistence.entity.featurestore.statistics.StatisticsConfig;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.external.ExternalTrainingDataset;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.hopsfs.HopsfsTrainingDataset;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.split.TrainingDatasetSplit;
import io.hops.hopsworks.persistence.entity.user.Users;

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
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;

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
  @NamedQuery(name = "TrainingDataset.countByFeaturestore", query = "SELECT count(td.id) FROM TrainingDataset td " +
        "WHERE td.featurestore = :featurestore"),
  @NamedQuery(name = "TrainingDataset.findByFeaturestoreAndId", query = "SELECT td FROM TrainingDataset td " +
    "WHERE td.featurestore = :featurestore AND td.id = :id"),
  @NamedQuery(name = "TrainingDataset.findByFeaturestoreAndNameVersion",
    query = "SELECT td FROM TrainingDataset td WHERE td.featurestore = :featurestore " +
      "AND td.name= :name AND td.version = :version"),
  @NamedQuery(name = "TrainingDataset.findByFeaturestoreAndName", query = "SELECT td FROM TrainingDataset td " +
    "WHERE td.featurestore = :featurestore AND td.name = :name"),
  @NamedQuery(name = "TrainingDataset.findByFeaturestoreAndNameOrderedByDescVersion", query = "SELECT td FROM " +
    "TrainingDataset td WHERE td.featurestore = :featurestore AND td.name = :name ORDER BY td.version DESC")})
public class TrainingDataset implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @Column(name = "name")
  private String name;
  @JoinColumn(name = "feature_store_id", referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Featurestore featurestore;
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
  @Basic
  @Column(name = "coalesce")
  private Boolean coalesce;
  @Basic(optional = false)
  @Column(name = "description")
  private String description;
  @Basic
  @Column(name = "seed")
  private Long seed;
  @Basic
  @Column(name = "query")
  private boolean query;
  @OneToOne(cascade = CascadeType.ALL, mappedBy = "trainingDataset")
  private StatisticsConfig statisticsConfig;
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "trainingDataset")
  private Collection<TrainingDatasetFeature> features;
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "trainingDataset")
  private Collection<TrainingDatasetJoin> joins;
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
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "trainingDataset")
  private Collection<TrainingDatasetSplit> splits;
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "trainingDataset")
  private Collection<FeaturestoreActivity> activities;

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

  public Boolean getCoalesce() {
    return coalesce;
  }

  public void setCoalesce(Boolean coalesce) {
    this.coalesce = coalesce;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Collection<TrainingDatasetFeature> getFeatures() {
    return features;
  }

  public void setFeatures(Collection<TrainingDatasetFeature> features) {
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
  
  public TrainingDatasetType getTrainingDatasetType() {
    return trainingDatasetType;
  }
  
  public void setTrainingDatasetType(
    TrainingDatasetType trainingDatasetType) {
    this.trainingDatasetType = trainingDatasetType;
  }
  
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
  
  public Collection<TrainingDatasetSplit> getSplits() {
    return splits;
  }
  
  public void setSplits(
    Collection<TrainingDatasetSplit> splits) {
    this.splits = splits;
  }
  
  public Long getSeed() {
    return seed;
  }
  
  public void setSeed(Long seed) {
    this.seed = seed;
  }

  public boolean isQuery() {
    return query;
  }

  public void setQuery(boolean query) {
    this.query = query;
  }

  public Collection<TrainingDatasetJoin> getJoins() {
    return joins;
  }

  public void setJoins(Collection<TrainingDatasetJoin> joins) {
    this.joins = joins;
  }

  public Collection<FeaturestoreActivity> getActivities() {
    return activities;
  }

  public void setActivities(Collection<FeaturestoreActivity> activities) {
    this.activities = activities;
  }

  public StatisticsConfig getStatisticsConfig() {
    return statisticsConfig;
  }

  public void setStatisticsConfig(StatisticsConfig statisticsConfig) {
    this.statisticsConfig = statisticsConfig;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TrainingDataset that = (TrainingDataset) o;

    if (query != that.query) return false;
    if (!Objects.equals(id, that.id)) return false;
    if (!Objects.equals(name, that.name)) return false;
    if (!Objects.equals(featurestore, that.featurestore)) return false;
    if (!Objects.equals(created, that.created)) return false;
    if (!Objects.equals(creator, that.creator)) return false;
    if (!Objects.equals(version, that.version)) return false;
    if (!Objects.equals(dataFormat, that.dataFormat)) return false;
    if (!Objects.equals(description, that.description)) return false;
    if (!Objects.equals(seed, that.seed)) return false;
    if (!Objects.equals(features, that.features)) return false;
    if (!Objects.equals(joins, that.joins)) return false;
    if (trainingDatasetType != that.trainingDatasetType) return false;
    if (!Objects.equals(hopsfsTrainingDataset, that.hopsfsTrainingDataset)) return false;
    if (!Objects.equals(externalTrainingDataset, that.externalTrainingDataset)) return false;
    return Objects.equals(splits, that.splits);
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + (featurestore != null ? featurestore.hashCode() : 0);
    result = 31 * result + (created != null ? created.hashCode() : 0);
    result = 31 * result + (creator != null ? creator.hashCode() : 0);
    result = 31 * result + (version != null ? version.hashCode() : 0);
    result = 31 * result + (dataFormat != null ? dataFormat.hashCode() : 0);
    result = 31 * result + (description != null ? description.hashCode() : 0);
    result = 31 * result + (seed != null ? seed.hashCode() : 0);
    result = 31 * result + (query ? 1 : 0);
    result = 31 * result + (features != null ? features.hashCode() : 0);
    result = 31 * result + (joins != null ? joins.hashCode() : 0);
    result = 31 * result + (trainingDatasetType != null ? trainingDatasetType.hashCode() : 0);
    result = 31 * result + (hopsfsTrainingDataset != null ? hopsfsTrainingDataset.hashCode() : 0);
    result = 31 * result + (externalTrainingDataset != null ? externalTrainingDataset.hashCode() : 0);
    result = 31 * result + (splits != null ? splits.hashCode() : 0);
    return result;
  }
}
