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

package io.hops.hopsworks.persistence.entity.featurestore.featureview;

import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.activity.FeaturestoreActivity;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetFeature;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetJoin;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
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
import java.util.Objects;

@Entity
@Table(name = "feature_view", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "FeatureView.findAll", query = "SELECT fv FROM FeatureView fv"),
    @NamedQuery(name = "FeatureView.findById", query = "SELECT fv FROM FeatureView fv WHERE fv.id = :id"),
    @NamedQuery(name = "FeatureView.findByFeaturestore", query = "SELECT fv FROM FeatureView fv " +
        "WHERE fv.featurestore = :featurestore"),
    @NamedQuery(name = "FeatureView.findByFeaturestoreAndNameVersion",
        query = "SELECT fv FROM FeatureView fv WHERE fv.featurestore = :featurestore " +
            "AND fv.name= :name AND fv.version = :version"),
    @NamedQuery(name = "FeatureView.findByFeaturestoreAndNameOrderedByDescVersion", query = "SELECT fv FROM " +
        "FeatureView fv WHERE fv.featurestore = :featurestore AND fv.name = :name ORDER BY fv.version DESC")})
public class FeatureView implements Serializable {
  private static final long serialVersionUID = 1L;
  public static final String TABLE_NAME = "FeatureView";
  public static final String TABLE_NAME_ALIAS = "fv";
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
  @Column(name = "description")
  private String description;
  @Basic
  @Column(name = "label")
  private String label;
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "featureView")
  private Collection<TrainingDatasetFeature> features;
  //TODO featureview: add TrainingDatasetFilterCondition;
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "featureView")
  private Collection<TrainingDatasetJoin> joins;
  // TODO featureview: can be removed?
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "featureView")
  private Collection<FeaturestoreActivity> activities;
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "featureView")
  private Collection<TrainingDataset> trainingDatasets;

  public FeatureView() {
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public Collection<TrainingDatasetFeature> getFeatures() {
    return features;
  }

  public void setFeatures(
      Collection<TrainingDatasetFeature> features) {
    this.features = features;
  }

  public Collection<TrainingDatasetJoin> getJoins() {
    return joins;
  }

  public void setJoins(
      Collection<TrainingDatasetJoin> joins) {
    this.joins = joins;
  }

  public Collection<FeaturestoreActivity> getActivities() {
    return activities;
  }

  public void setActivities(
      Collection<FeaturestoreActivity> activities) {
    this.activities = activities;
  }

  public Collection<TrainingDataset> getTrainingDatasets() {
    return trainingDatasets;
  }

  public void setTrainingDatasets(
      Collection<TrainingDataset> trainingDatasets) {
    this.trainingDatasets = trainingDatasets;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FeatureView that = (FeatureView) o;
    return Objects.equals(id, that.id) && Objects.equals(name, that.name) &&
        Objects.equals(featurestore, that.featurestore) && Objects.equals(created, that.created) &&
        Objects.equals(creator, that.creator) && Objects.equals(version, that.version) &&
        Objects.equals(description, that.description) && Objects.equals(label, that.label) &&
        Objects.equals(features, that.features) && Objects.equals(joins, that.joins) &&
        Objects.equals(trainingDatasets, that.trainingDatasets);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, featurestore, created, creator, version, description, label, features, joins,
        trainingDatasets);
  }
}
