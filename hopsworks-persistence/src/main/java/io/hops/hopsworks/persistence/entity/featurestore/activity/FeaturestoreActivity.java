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

package io.hops.hopsworks.persistence.entity.featurestore.activity;

import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.FeatureGroupCommit;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.FeatureGroupValidation;
import io.hops.hopsworks.persistence.entity.featurestore.statistics.FeaturestoreStatistic;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.jobs.history.Execution;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@Entity
@Table(name = "feature_store_activity", catalog = "hopsworks")
@NamedQueries({
    @NamedQuery(name = "FeaturestoreActivity.lastFgExecution",
        query = "SELECT fsa FROM FeaturestoreActivity fsa " +
            "WHERE fsa.featureGroup = :featureGroup AND fsa.type = :type " +
            "ORDER BY fsa.eventTime DESC"),
    @NamedQuery(name = "FeaturestoreActivity.lastTdExecution",
        query = "SELECT fsa FROM FeaturestoreActivity fsa " +
            "WHERE fsa.trainingDataset = :trainingDataset AND fsa.type = :type " +
            "ORDER BY fsa.eventTime DESC"),
    @NamedQuery(name = "FeaturestoreActivity.findByFgExecution",
        query = "SELECT fsa FROM FeaturestoreActivity fsa " +
            "WHERE fsa.featureGroup = :featureGroup AND fsa.execution = :execution"),
    @NamedQuery(name = "FeaturestoreActivity.findByTdExecution",
        query = "SELECT fsa FROM FeaturestoreActivity fsa " +
            "WHERE fsa.trainingDataset = :trainingDataset AND fsa.execution = :execution")})
public class FeaturestoreActivity implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

  @Basic(optional = false)
  @Column(name = "event_time")
  @Temporal(TemporalType.TIMESTAMP)
  private Date eventTime;

  @JoinColumn(name = "uid", referencedColumnName = "uid")
  @ManyToOne(optional = false)
  private Users user;

  @Basic(optional = false)
  @NotNull
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "type")
  private ActivityType type;

  @Enumerated(EnumType.ORDINAL)
  @Column(name = "meta_type")
  private FeaturestoreActivityMeta activityMeta;

  @Column(name = "meta_msg")
  private String activityMetaMsg;

  @JoinColumn(name = "execution_id", referencedColumnName = "id")
  private Execution execution;

  @Column(name = "execution_last_event_time")
  private Long executionLastEventTime;

  @JoinColumn(name = "statistics_id", referencedColumnName = "id")
  private FeaturestoreStatistic statistics;

  @JoinColumns({
      @JoinColumn(name = "commit_id", referencedColumnName = "commit_id"),
      @JoinColumn(name = "feature_group_id", referencedColumnName = "feature_group_id",
          insertable = false, updatable = false)
    })
  private FeatureGroupCommit commit;

  @JoinColumn(name = "validation_id", referencedColumnName = "id")
  private FeatureGroupValidation validation;

  @JoinColumn(name = "feature_group_id", referencedColumnName = "id")
  private Featuregroup featureGroup;

  @JoinColumn(name = "training_dataset_id", referencedColumnName = "id")
  private TrainingDataset trainingDataset;

  public FeaturestoreActivity() {}

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Date getEventTime() {
    return eventTime;
  }

  public void setEventTime(Date eventTime) {
    this.eventTime = eventTime;
  }

  public Users getUser() {
    return user;
  }

  public void setUser(Users user) {
    this.user = user;
  }

  public ActivityType getType() {
    return type;
  }

  public void setType(ActivityType type) {
    this.type = type;
  }

  public FeaturestoreActivityMeta getActivityMeta() {
    return activityMeta;
  }

  public void setActivityMeta(FeaturestoreActivityMeta activityMeta) {
    this.activityMeta = activityMeta;
  }

  public String getActivityMetaMsg() {
    return activityMetaMsg;
  }

  public void setActivityMetaMsg(String activityMetaMsg) {
    this.activityMetaMsg = activityMetaMsg;
  }

  public Execution getExecution() {
    return execution;
  }

  public void setExecution(Execution execution) {
    this.execution = execution;
  }

  public Long getExecutionLastEventTime() {
    return executionLastEventTime;
  }

  public void setExecutionLastEventTime(Long executionLastEventTime) {
    this.executionLastEventTime = executionLastEventTime;
  }

  public FeaturestoreStatistic getStatistics() {
    return statistics;
  }

  public void setStatistics(FeaturestoreStatistic statistics) {
    this.statistics = statistics;
  }

  public FeatureGroupCommit getCommit() {
    return commit;
  }

  public void setCommit(FeatureGroupCommit commit) {
    this.commit = commit;
  }

  public FeatureGroupValidation getValidation() {
    return validation;
  }

  public void setValidation(FeatureGroupValidation validation) {
    this.validation = validation;
  }

  public Featuregroup getFeatureGroup() {
    return featureGroup;
  }

  public void setFeatureGroup(Featuregroup featureGroup) {
    this.featureGroup = featureGroup;
  }

  public TrainingDataset getTrainingDataset() {
    return trainingDataset;
  }

  public void setTrainingDataset(TrainingDataset trainingDataset) {
    this.trainingDataset = trainingDataset;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    FeaturestoreActivity that = (FeaturestoreActivity) o;

    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }
}
