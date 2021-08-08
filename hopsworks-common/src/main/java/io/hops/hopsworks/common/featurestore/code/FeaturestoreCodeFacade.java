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

package io.hops.hopsworks.common.featurestore.code;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.featurestore.code.FeaturestoreCode;
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
 * A facade for the featurestore_code table in the Hopsworks database, use this interface when performing database
 * operations against the table.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class FeaturestoreCodeFacade extends AbstractFacade<FeaturestoreCode> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public FeaturestoreCodeFacade() {
    super(FeaturestoreCode.class);
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

  public CollectionInfo<FeaturestoreCode> findByFeaturegroup(Integer offset, Integer limit,
                                                             Set<? extends SortBy> sorts,
                                                             Set<? extends FilterBy> filters,
                                                             Featuregroup featuregroup) {
    String queryStr = buildQuery("SELECT s from FeaturestoreCode s ",filters,
            sorts, "s.featureGroup = :featureGroup");
    String queryCountStr = buildQuery("SELECT COUNT(s.id) from FeaturestoreCode s ", filters,
            sorts, "s.featureGroup = :featureGroup");
    Query query = em.createQuery(queryStr, FeaturestoreCode.class)
            .setParameter("featureGroup", featuregroup);
    Query queryCount = em.createQuery(queryCountStr, FeaturestoreCode.class)
            .setParameter("featureGroup", featuregroup);
    setFilter(filters, query);
    setFilter(filters, queryCount);
    setOffsetAndLim(offset, limit, query);
    return new CollectionInfo((Long) queryCount.getSingleResult(), query.getResultList());
  }

  public CollectionInfo<FeaturestoreCode> findByTrainingDataset(Integer offset, Integer limit,
                                                                Set<? extends SortBy> sorts,
                                                                Set<? extends FilterBy> filters,
                                                                TrainingDataset trainingDataset) {
    String queryStr = buildQuery("SELECT s from FeaturestoreCode s ", filters,
            sorts, "s.trainingDataset = :trainingDataset");
    String queryCountStr = buildQuery("SELECT COUNT(s.id) from FeaturestoreCode s ", filters,
            sorts, "s.trainingDataset = :trainingDataset");
    Query query = em.createQuery(queryStr, FeaturestoreCode.class)
            .setParameter("trainingDataset", trainingDataset);
    Query queryCount = em.createQuery(queryCountStr, FeaturestoreCode.class)
            .setParameter("trainingDataset", trainingDataset);
    setFilter(filters, query);
    setFilter(filters, queryCount);
    setOffsetAndLim(offset, limit, query);
    return new CollectionInfo((Long) queryCount.getSingleResult(), query.getResultList());
  }

  public Optional<FeaturestoreCode> findFeaturestoreCodeById(TrainingDataset trainingDataset, Integer id) {
    Query fssQuery =  em.createNamedQuery("FeaturestoreCode.findByIdAndTrainingDataset", FeaturestoreCode.class)
            .setParameter("id", id)
            .setParameter("trainingDataset", trainingDataset);
    try {
      return Optional.of((FeaturestoreCode) fssQuery.getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public Optional<FeaturestoreCode> findFeaturestoreCodeById(Featuregroup featuregroup, Integer id) {
    Query fssQuery =  em.createNamedQuery("FeaturestoreCode.findByIdAndFeatureGroup", FeaturestoreCode.class)
            .setParameter("id", id)
            .setParameter("featureGroup", featuregroup);
    try {
      return Optional.of((FeaturestoreCode) fssQuery.getSingleResult());
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
    COMMIT_TIME_EQ("COMMIT_TIME_EQ", "s.commitTime = :commitTime ","commitTime",""),
    APPLICATION_ID("APPLICATION_ID", "s.application_id = :application_id ","application_id","");

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
