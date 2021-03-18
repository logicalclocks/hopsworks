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

package io.hops.hopsworks.common.featurestore.statistics;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.featurestore.statistics.FeaturestoreStatistic;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.Set;

/**
 * A facade for the featurestore_statistic table in the Hopsworks database, use this interface when performing database
 * operations against the table.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class FeaturestoreStatisticFacade extends AbstractFacade<FeaturestoreStatistic> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public FeaturestoreStatisticFacade() {
    super(FeaturestoreStatistic.class);
  }

  /**
   * Gets the entity manager of the facade
   *
   * @return entity manager
   */
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  // TODO(Fabio) reduce code duplication
  public CollectionInfo findByFeaturegroup(Integer offset, Integer limit,
                                                        Set<? extends SortBy> sorts,
                                                        Set<? extends FilterBy> filters,
                                                        Featuregroup featuregroup) {
    String queryStr = buildQuery("SELECT s from FeaturestoreStatistic s ",filters,
        sorts, "s.featureGroup = :featureGroup");
    String queryCountStr = buildQuery("SELECT COUNT(s.id) from FeaturestoreStatistic s ", filters,
        sorts, "s.featureGroup = :featureGroup");
    Query query = em.createQuery(queryStr, FeaturestoreStatistic.class)
        .setParameter("featureGroup", featuregroup);
    Query queryCount = em.createQuery(queryCountStr, FeaturestoreStatistic.class)
        .setParameter("featureGroup", featuregroup);
    setFilter(filters, query);
    setFilter(filters, queryCount);
    setOffsetAndLim(offset, limit, query);
    return new CollectionInfo((Long) queryCount.getSingleResult(), query.getResultList());
  }

  public CollectionInfo findByTrainingDataset(Integer offset, Integer limit,
                                              Set<? extends SortBy> sorts,
                                              Set<? extends FilterBy> filters,
                                              TrainingDataset trainingDataset) {
    String queryStr = buildQuery("SELECT s from FeaturestoreStatistic s ", filters,
        sorts, "s.trainingDataset = :trainingDataset");
    String queryCountStr = buildQuery("SELECT COUNT(s.id) from FeaturestoreStatistic s ", filters,
        sorts, "s.trainingDataset = :trainingDataset");
    Query query = em.createQuery(queryStr, FeaturestoreStatistic.class)
        .setParameter("trainingDataset", trainingDataset);
    Query queryCount = em.createQuery(queryCountStr, FeaturestoreStatistic.class)
        .setParameter("trainingDataset", trainingDataset);
    setFilter(filters, query);
    setFilter(filters, queryCount);
    setOffsetAndLim(offset, limit, query);
    return new CollectionInfo((Long) queryCount.getSingleResult(), query.getResultList());
  }

  public Optional<FeaturestoreStatistic> findFGStatisticsByCommitTime(Featuregroup featureGroup, Long commitTime) {
    Query fssQuery =  em.createNamedQuery("FeaturestoreStatistic.commitTime", FeaturestoreStatistic.class)
        .setParameter("featureGroup", featureGroup)
        .setParameter("commitTime", new Timestamp(commitTime));
    try {
      return Optional.of((FeaturestoreStatistic) fssQuery.getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  private void setFilter(Set<? extends AbstractFacade.FilterBy> filter, Query q) {
    if (filter == null || filter.isEmpty()) {
      return;
    }
    for (FilterBy aFilter : filter) {
      q.setParameter(aFilter.getField(), new Timestamp(Long.parseLong(aFilter.getParam())));
    }
  }

  public enum Sorts {
    COMMIT_TIME("COMMIT_TIME", "s.commitTime ", "DESC");

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

    public String getDefaultParam() {
      return defaultParam;
    }

    public String getSql() {
      return sql;
    }

    @Override
    public String toString() {
      return value;
    }
  }

  public enum Filters {
    COMMIT_TIME_GT("COMMIT_TIME_GT", "s.commitTime > :commitTime ","commitTime",""),
    COMMIT_TIME_LT("COMMIT_TIME_LT", "s.commitTime < :commitTime ","commitTime",""),
    COMMIT_TIME_EQ("COMMIT_TIME_EQ", "s.commitTime = :commitTime ","commitTime","");

    private final String value;
    private final String sql;
    private final String field;
    private final String defaultParam;

    private Filters(String value, String sql, String field, String defaultParam) {
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
