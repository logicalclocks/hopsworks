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

package io.hops.hopsworks.common.featurestore.statistics;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.statistics.FeatureGroupStatistics;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * A facade for the featurestore_statistic table in the Hopsworks database, use this interface when performing database
 * operations against the table.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class FeatureGroupStatisticsFacade extends AbstractFacade<FeatureGroupStatistics> {
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  public FeatureGroupStatisticsFacade() {
    super(FeatureGroupStatistics.class);
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
  
  public CollectionInfo<FeatureGroupStatistics> findByFeaturegroup(Integer offset, Integer limit,
    Set<? extends SortBy> sorts, Set<? extends FilterBy> filters, Featuregroup featuregroup) {
    String queryStr =
      buildQuery("SELECT s from FeatureGroupStatistics s ", filters, sorts, "s.featureGroup = :featureGroup");
    String queryCountStr =
      buildQuery("SELECT COUNT(s.id) from FeatureGroupStatistics s ", filters, sorts, "s.featureGroup = :featureGroup");
    Query query = em.createQuery(queryStr, FeatureGroupStatistics.class).setParameter("featureGroup", featuregroup);
    Query queryCount =
      em.createQuery(queryCountStr, FeatureGroupStatistics.class).setParameter("featureGroup", featuregroup);
    StatisticsFilters.setFilter(filters, query);
    StatisticsFilters.setFilter(filters, queryCount);
    setOffsetAndLim(offset, limit, query);
    
    return new CollectionInfo<>((Long) queryCount.getSingleResult(), query.getResultList());
  }
  
  public CollectionInfo<FeatureGroupStatistics> findByFeaturegroupWithFeatureNames(Integer offset, Integer limit,
    Set<? extends SortBy> sorts, Set<? extends FilterBy> filters, Set<String> featureNames, Featuregroup featuregroup)
    throws FeaturestoreException {
    String fNsOperator;
    Object fNsParamValue;
    if (featureNames.size() > 1) {
      fNsOperator = "IN";
      fNsParamValue = featureNames;
    } else {
      fNsOperator = "=";
      fNsParamValue = featureNames.iterator().next();
    }
    if (limit != null) {
      // if limit is set, we need to run two sql queries
      return findByFeaturegroupWithFeatureNames(offset, limit, sorts, filters, fNsOperator, fNsParamValue,
        featuregroup);
    } else {
      // otherwise, we can use a single query
      return findByFeaturegroupWithFeatureNames(sorts, filters, fNsOperator, fNsParamValue, featuregroup);
    }
  }
  
  private CollectionInfo<FeatureGroupStatistics> findByFeaturegroupWithFeatureNames(Set<? extends SortBy> sorts,
    Set<? extends FilterBy> filters, String fNsOperator, Object fNsParamValue, Featuregroup featuregroup)
    throws FeaturestoreException {
    
    String queryStr = buildQuery(
      "SELECT DISTINCT s from FeatureGroupStatistics s LEFT JOIN FETCH s.featureDescriptiveStatistics " + "fds ",
      filters, sorts, "s.featureGroup = :featureGroup AND fds.featureName " + fNsOperator + " :featureName");
    String queryCountStr = buildQuery("SELECT COUNT(DISTINCT s.id) from FeatureGroupStatistics s LEFT JOIN FETCH s" +
        ".featureDescriptiveStatistics fds ", filters, sorts,
      "s.featureGroup = :featureGroup AND fds.featureName " + fNsOperator + " :featureName");
    Query query = em.createQuery(queryStr, FeatureGroupStatistics.class).setParameter("featureGroup", featuregroup)
      .setParameter("featureName", fNsParamValue);
    Query queryCount =
      em.createQuery(queryCountStr, FeatureGroupStatistics.class).setParameter("featureGroup", featuregroup)
        .setParameter("featureName", fNsParamValue);
    StatisticsFilters.setFilter(filters, query);
    StatisticsFilters.setFilter(filters, queryCount);
    
    long count = (long) queryCount.getSingleResult();
    if (count == 0) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.STATISTICS_NOT_FOUND, Level.FINE,
        "Statistics for feature group id '" + featuregroup.getId() + "' not found. Please, try again with different " +
          "filters.");
    }
    return new CollectionInfo<>(count, query.getResultList());
  }
  
  private CollectionInfo<FeatureGroupStatistics> findByFeaturegroupWithFeatureNames(Integer offset, Integer limit,
    Set<? extends SortBy> sorts, Set<? extends FilterBy> filters, String fNsOperator, Object fNsParamValue,
    Featuregroup featuregroup) throws FeaturestoreException {
    
    // 1. fetch feature group statistics ids
    String fgsQueryStr =
      buildQuery("SELECT DISTINCT s.id from FeatureGroupStatistics s LEFT JOIN s.featureDescriptiveStatistics fds ",
        filters, sorts, "s.featureGroup = :featureGroup AND fds.featureName " + fNsOperator + " :featureName");
    String fgsQueryCountStr = buildQuery(
      "SELECT COUNT(DISTINCT s.id) from FeatureGroupStatistics s LEFT JOIN s.featureDescriptiveStatistics" + " fds ",
      filters, sorts, "s.featureGroup = :featureGroup AND fds.featureName " + fNsOperator + " :featureName");
    Query fgsQuery =
      em.createQuery(fgsQueryStr).setParameter("featureGroup", featuregroup).setParameter("featureName", fNsParamValue);
    Query fgsQueryCount = em.createQuery(fgsQueryCountStr).setParameter("featureGroup", featuregroup)
      .setParameter("featureName", fNsParamValue);
    StatisticsFilters.setFilter(filters, fgsQuery);
    setOffsetAndLim(offset, limit, fgsQuery);
    StatisticsFilters.setFilter(filters, fgsQueryCount);
    
    long count = (long) fgsQueryCount.getSingleResult();
    if (count == 0) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.STATISTICS_NOT_FOUND, Level.FINE,
        "Statistics for feature group id '" + featuregroup.getId() + "' not found. Please, try again with different " +
          "filters.");
    }
    Set<Integer> fgsIds = (Set<Integer>) fgsQuery.getResultList().stream().collect(Collectors.toSet());
    
    // 2. fetch feature group statistics with feature descriptive statistics
    String queryStr = buildQuery(
      "SELECT DISTINCT s from FeatureGroupStatistics s LEFT JOIN FETCH s.featureDescriptiveStatistics " + "fds ",
      filters, sorts, "s.id IN :fgsIds AND fds.featureName " + fNsOperator + " :featureName");
    Query query = em.createQuery(queryStr, FeatureGroupStatistics.class).setParameter("fgsIds", fgsIds)
      .setParameter("featureName", fNsParamValue);
    StatisticsFilters.setFilter(filters, query);
    
    return new CollectionInfo<>(count, query.getResultList());
  }
}