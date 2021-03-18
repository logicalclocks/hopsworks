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

package io.hops.hopsworks.common.featurestore.activity;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.featurestore.activity.ActivityType;
import io.hops.hopsworks.persistence.entity.featurestore.activity.FeaturestoreActivity;
import io.hops.hopsworks.persistence.entity.featurestore.activity.FeaturestoreActivityMeta;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.FeatureGroupCommit;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.FeatureGroupValidation;
import io.hops.hopsworks.persistence.entity.featurestore.statistics.FeaturestoreStatistic;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.jobs.history.Execution;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

@Stateless
public class FeaturestoreActivityFacade extends AbstractFacade<FeaturestoreActivity> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public FeaturestoreActivityFacade() {
    super(FeaturestoreActivity.class);
  }

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public void logMetadataActivity(Users user, Featuregroup featuregroup,
                                  FeaturestoreActivityMeta metadataType,
                                  String additionalMsg) {
    FeaturestoreActivity fsActivity = new FeaturestoreActivity();
    fsActivity.setType(ActivityType.METADATA);
    fsActivity.setFeatureGroup(featuregroup);
    fsActivity.setUser(user);
    fsActivity.setEventTime(new Date());
    fsActivity.setActivityMeta(metadataType);
    fsActivity.setActivityMetaMsg(additionalMsg);
    em.persist(fsActivity);
  }

  public void logMetadataActivity(Users user, TrainingDataset trainingDataset,
                                  FeaturestoreActivityMeta metadataType) {
    FeaturestoreActivity fsActivity = new FeaturestoreActivity();
    fsActivity.setType(ActivityType.METADATA);
    fsActivity.setTrainingDataset(trainingDataset);
    fsActivity.setUser(user);
    fsActivity.setEventTime(new Date());
    fsActivity.setActivityMeta(metadataType);
    em.persist(fsActivity);
  }

  public void logStatisticsActivity(Users user, Featuregroup featuregroup, Date eventTime,
                                    FeaturestoreStatistic statistics) {
    FeaturestoreActivity fsActivity = new FeaturestoreActivity();
    fsActivity.setType(ActivityType.STATISTICS);
    fsActivity.setFeatureGroup(featuregroup);
    fsActivity.setUser(user);
    fsActivity.setEventTime(eventTime);
    fsActivity.setStatistics(statistics);
    em.persist(fsActivity);
  }

  public void logStatisticsActivity(Users user, TrainingDataset trainingDataset, Date eventTime,
                                    FeaturestoreStatistic statistics) {
    FeaturestoreActivity fsActivity = new FeaturestoreActivity();
    fsActivity.setType(ActivityType.STATISTICS);
    fsActivity.setTrainingDataset(trainingDataset);
    fsActivity.setUser(user);
    fsActivity.setEventTime(eventTime);
    fsActivity.setStatistics(statistics);
    em.persist(fsActivity);
  }

  public void logCommitActivity(Users user, Featuregroup featuregroup, FeatureGroupCommit commit) {
    FeaturestoreActivity fsActivity = new FeaturestoreActivity();
    fsActivity.setType(ActivityType.COMMIT);
    fsActivity.setFeatureGroup(featuregroup);
    fsActivity.setUser(user);
    fsActivity.setEventTime(new Date(commit.getCommittedOn()));
    fsActivity.setCommit(commit);
    em.persist(fsActivity);
  }

  public void logExecutionActivity(Featuregroup featuregroup, Execution execution, Long executionLastEventTime) {
    // If the execution already exists, updated it, otherwise create a new one
    FeaturestoreActivity fsActivity = null;
    try {
      fsActivity = em.createNamedQuery("FeaturestoreActivity.findByFgExecution",
          FeaturestoreActivity.class)
          .setParameter("featureGroup", featuregroup)
          .setParameter("execution", execution)
          .getSingleResult();
    } catch (NoResultException e) {
      fsActivity = new FeaturestoreActivity();
      fsActivity.setType(ActivityType.JOB);
      fsActivity.setFeatureGroup(featuregroup);
      fsActivity.setUser(execution.getUser());
      fsActivity.setEventTime(execution.getSubmissionTime());
      fsActivity.setExecution(execution);
    }

    fsActivity.setExecutionLastEventTime(executionLastEventTime);
    em.merge(fsActivity);
  }

  public void logExecutionActivity(TrainingDataset trainingDataset, Execution execution, Long executionLastEventTime) {
    // If the execution already exists, updated it, otherwise create a new one
    FeaturestoreActivity fsActivity = null;
    try {
      fsActivity = em.createNamedQuery("FeaturestoreActivity.findByTdExecution",
          FeaturestoreActivity.class)
          .setParameter("trainingDataset", trainingDataset)
          .setParameter("execution", execution)
          .getSingleResult();
    } catch (NoResultException e) {
      fsActivity = new FeaturestoreActivity();
      fsActivity.setType(ActivityType.JOB);
      fsActivity.setTrainingDataset(trainingDataset);
      fsActivity.setUser(execution.getUser());
      fsActivity.setEventTime(execution.getSubmissionTime());
      fsActivity.setExecution(execution);
    }

    fsActivity.setExecutionLastEventTime(executionLastEventTime);
    em.merge(fsActivity);
  }

  public void logValidationActivity(Featuregroup featureGroup, Users user, FeatureGroupValidation validation) {
    FeaturestoreActivity fsActivity = new FeaturestoreActivity();
    fsActivity.setType(ActivityType.VALIDATIONS);
    fsActivity.setFeatureGroup(featureGroup);
    fsActivity.setUser(user);
    fsActivity.setEventTime(validation.getValidationTime());
    fsActivity.setValidation(validation);
    em.persist(fsActivity);
  }

  public Optional<FeaturestoreActivity> getMostRecentExecution(Featuregroup featuregroup) {
    try {
      return Optional.of(em.createNamedQuery("FeaturestoreActivity.lastFgExecution", FeaturestoreActivity.class)
          .setParameter("featureGroup", featuregroup)
          .setParameter("type", ActivityType.JOB)
          .setMaxResults(1)
          .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public Optional<FeaturestoreActivity> getMostRecentExecution(TrainingDataset trainingDataset) {
    try {
      return Optional.of(em.createNamedQuery("FeaturestoreActivity.lastTdExecution", FeaturestoreActivity.class)
          .setParameter("trainingDataset", trainingDataset)
          .setParameter("type", ActivityType.JOB)
          .setMaxResults(1)
          .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public CollectionInfo<FeaturestoreActivity> findByFeaturegroup(Featuregroup featuregroup, Integer offset,
                                                                 Integer limit,
                                                                 Set<? extends FilterBy> filters,
                                                                 Set<? extends AbstractFacade.SortBy> sorts) {

    String queryStr = buildQuery("SELECT a FROM FeaturestoreActivity a ",
        filters, sorts, "a.featureGroup = :featureGroup");
    String queryCountStr = buildQuery("SELECT COUNT(DISTINCT a.id) FROM FeaturestoreActivity a ",
        filters, sorts, "a.featureGroup = :featureGroup");

    Query query = em.createQuery(queryStr, FeaturestoreActivity.class).setParameter("featureGroup", featuregroup);
    setFilters(filters, query);
    setOffsetAndLim(offset, limit, query);

    Query queryCount = em.createQuery(queryCountStr, FeaturestoreActivity.class)
        .setParameter("featureGroup", featuregroup);
    setFilters(filters, queryCount);

    return new CollectionInfo<FeaturestoreActivity>((Long) queryCount.getSingleResult(), query.getResultList());
  }

  public CollectionInfo<FeaturestoreActivity> findByTrainingDataset(TrainingDataset trainingDataset, Integer offset,
                                                                 Integer limit,
                                                                 Set<? extends FilterBy> filters,
                                                                 Set<? extends AbstractFacade.SortBy> sorts) {

    String queryStr = buildQuery("SELECT a FROM FeaturestoreActivity a ",
        filters, sorts, "a.trainingDataset = :trainingDataset");
    String queryCountStr = buildQuery("SELECT COUNT(DISTINCT a.id) FROM FeaturestoreActivity a ",
        filters, sorts, "a.trainingDataset = :trainingDataset");

    Query query = em.createQuery(queryStr, FeaturestoreActivity.class)
        .setParameter("trainingDataset", trainingDataset);
    setFilters(filters, query);
    setOffsetAndLim(offset, limit, query);

    Query queryCount = em.createQuery(queryCountStr, FeaturestoreActivity.class)
        .setParameter("trainingDataset", trainingDataset);
    setFilters(filters, queryCount);

    return new CollectionInfo<FeaturestoreActivity>((Long) queryCount.getSingleResult(), query.getResultList());
  }

  private void setFilters(Set<? extends AbstractFacade.FilterBy> filters, Query query) {
    if (filters == null || filters.isEmpty()) {
      return;
    }

    for (AbstractFacade.FilterBy filter : filters) {
      setFilterQuery(filter, query);
    }
  }

  private void setFilterQuery(AbstractFacade.FilterBy filterBy, Query query) {
    switch (Filters.valueOf(filterBy.getValue())) {
      case TYPE:
        query.setParameter(filterBy.getField(), ActivityType.valueOf(filterBy.getParam().toUpperCase()));
        break;
      case TIMESTAMP_GT:
      case TIMESTAMP_LT:
        query.setParameter(filterBy.getField(), new Date(Long.parseLong(filterBy.getParam())));
      default:
        break;
    }
  }

  public enum Sorts {
    TIMESTAMP("TIMESTAMP", "a.eventTime", "DESC");

    private final String value;
    private final String sql;
    private final String defaultParam;

    Sorts(String value, String sql, String defaultParam) {
      this.value = value;
      this.sql = sql;
      this.defaultParam = defaultParam;
    }

    public String getValue() {
      return value;
    }

    public String getSql() {
      return sql;
    }

    public String getDefaultParam() {
      return defaultParam;
    }
  }

  public enum Filters {
    TYPE("TYPE", "a.type = :type ", "type", ""),
    TIMESTAMP_LT("TIMESTAMP_LT", "a.eventTime < :eventTimeLt ", "eventTimeLt", ""),
    TIMESTAMP_GT("TIMESTAMP_GT", "a.eventTime > :eventTimeGt ", "eventTimeGt", "");


    private final String value;
    private final String sql;
    private final String field;
    private final String defaultParam;

    Filters(String value, String sql, String field, String defaultParam) {
      this.value = value;
      this.sql = sql;
      this.field = field;
      this.defaultParam = defaultParam;
    }

    public String getValue() {
      return value;
    }

    public String getDefaultParam() {
      return defaultParam;
    }

    public String getSql() {
      return sql;
    }

    public String getField() {
      return field;
    }

    @Override
    public String toString() {
      return value;
    }
  }
}
