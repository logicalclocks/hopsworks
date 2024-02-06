/*
 * This file is part of Hopsworks
 * Copyright (C) 2024, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.featuremonitoring.result;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.config.FeatureMonitoringConfiguration;
import io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.result.FeatureMonitoringResult;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.Set;

@Stateless
public class FeatureMonitoringResultFacade extends AbstractFacade<FeatureMonitoringResult> {
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }
  
  public FeatureMonitoringResultFacade() {
    super(FeatureMonitoringResult.class);
  }
  
  public Optional<FeatureMonitoringResult> findById(Integer resultId) {
    try {
      return Optional.of(em.createNamedQuery("FeatureMonitoringResult.findByResultId", FeatureMonitoringResult.class)
        .setParameter("resultId", resultId).getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
  
  public CollectionInfo<FeatureMonitoringResult> findByConfigId(Integer offset, Integer limit,
    Set<? extends SortBy> sorts, Set<? extends FilterBy> filters, FeatureMonitoringConfiguration config) {
    
    String queryStr = buildQuery("SELECT result from FeatureMonitoringResult result ", filters, sorts,
      "result.featureMonitoringConfig=:config");
    String queryCountStr = buildQuery("SELECT COUNT(result.id) from FeatureMonitoringResult result ", filters, sorts,
      "result.featureMonitoringConfig=:config");
    
    Query query = em.createQuery(queryStr, FeatureMonitoringResult.class)
      .setParameter("config", config);
    Query queryCount =
      em.createQuery(queryCountStr, FeatureMonitoringResult.class)
        .setParameter("config", config);
    
    setFilter(filters, query);
    setFilter(filters, queryCount);
    setOffsetAndLim(offset, limit, query);
    
    return new CollectionInfo<FeatureMonitoringResult>((Long) queryCount.getSingleResult(), query.getResultList());
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
    MONITORING_TIME("MONITORING_TIME", "result.monitoringTime ", "DESC");
    
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
    MONITORING_TIME_GT("MONITORING_TIME_GT", "result.monitoringTime > :monitoringTimeGt ", "monitoringTimeGt", ""),
    MONITORING_TIME_LT("MONITORING_TIME_LT", "result.monitoringTime < :monitoringTimeLt ", "monitoringTimeLt", ""),
    MONITORING_TIME_GTE("MONITORING_TIME_GTE", "result.monitoringTime >= :monitoringTimeGte ", "monitoringTimeGte", ""),
    MONITORING_TIME_LTE("MONITORING_TIME_LTE", "result.monitoringTime <= :monitoringTimeLte ", "monitoringTimeLte", ""),
    MONITORING_TIME_EQ("MONITORING_TIME_EQ", "result.monitoringTime = :monitoringTimeEq", "monitoringTimeEq", "");
    
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