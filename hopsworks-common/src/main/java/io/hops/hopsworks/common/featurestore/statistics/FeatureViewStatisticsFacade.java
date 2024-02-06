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
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.statistics.FeatureViewStatistics;
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
 * A facade for the feature_view_statistics table in the Hopsworks database, use this interface when performing database
 * operations against the table.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class FeatureViewStatisticsFacade extends AbstractFacade<FeatureViewStatistics> {
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  public FeatureViewStatisticsFacade() {
    super(FeatureViewStatistics.class);
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
  
  public CollectionInfo<FeatureViewStatistics> findByFeatureView(Integer offset, Integer limit,
    Set<? extends SortBy> sorts, Set<? extends FilterBy> filters, FeatureView featureView) {
    String queryStr =
      buildQuery("SELECT s from FeatureViewStatistics s ", filters, sorts, "s.featureView = :featureView");
    String queryCountStr =
      buildQuery("SELECT COUNT(s.id) from FeatureViewStatistics s ", filters, sorts, "s.featureView = :featureView");
    Query query = em.createQuery(queryStr, FeatureViewStatistics.class).setParameter("featureView", featureView);
    Query queryCount =
      em.createQuery(queryCountStr, FeatureViewStatistics.class).setParameter("featureView", featureView);
    StatisticsFilters.setFilter(filters, query);
    StatisticsFilters.setFilter(filters, queryCount);
    setOffsetAndLim(offset, limit, query);
    
    return new CollectionInfo<>((Long) queryCount.getSingleResult(), query.getResultList());
  }
  
  public CollectionInfo<FeatureViewStatistics> findByFeatureViewWithFeatureNames(Integer offset, Integer limit,
    Set<? extends SortBy> sorts, Set<? extends FilterBy> filters, Set<String> featureNames, FeatureView featureView)
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
      return findByFeatureViewWithFeatureNames(offset, limit, sorts, filters, fNsOperator, fNsParamValue,
        featureView);
    } else {
      // otherwise, we can use a single query
      return findByFeatureViewWithFeatureNames(sorts, filters, fNsOperator, fNsParamValue, featureView);
    }
  }
  
  private CollectionInfo<FeatureViewStatistics> findByFeatureViewWithFeatureNames(Set<? extends SortBy> sorts,
    Set<? extends FilterBy> filters, String fNsOperator, Object fNsParamValue, FeatureView featureView)
    throws FeaturestoreException {
    
    String queryStr = buildQuery(
      "SELECT DISTINCT s from FeatureViewStatistics s LEFT JOIN FETCH s.featureDescriptiveStatistics " + "fds ",
      filters, sorts, "s.featureView = :featureView AND fds.featureName " + fNsOperator + " :featureName");
    String queryCountStr = buildQuery("SELECT COUNT(DISTINCT s.id) from FeatureViewStatistics s LEFT JOIN FETCH s" +
        ".featureDescriptiveStatistics fds ", filters, sorts,
      "s.featureView = :featureView AND fds.featureName " + fNsOperator + " :featureName");
    Query query = em.createQuery(queryStr, FeatureViewStatistics.class).setParameter("featureView", featureView)
      .setParameter("featureName", fNsParamValue);
    Query queryCount =
      em.createQuery(queryCountStr, FeatureViewStatistics.class).setParameter("featureView", featureView)
        .setParameter("featureName", fNsParamValue);
    StatisticsFilters.setFilter(filters, query);
    StatisticsFilters.setFilter(filters, queryCount);
    
    long count = (long) queryCount.getSingleResult();
    if (count == 0) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.STATISTICS_NOT_FOUND, Level.FINE,
        "Statistics for feature view id '" + featureView.getId() + "' not found. Please, try again with different " +
          "filters.");
    }
    return new CollectionInfo<>(count, query.getResultList());
  }
  
  private CollectionInfo<FeatureViewStatistics> findByFeatureViewWithFeatureNames(Integer offset, Integer limit,
    Set<? extends SortBy> sorts, Set<? extends FilterBy> filters, String fNsOperator, Object fNsParamValue,
    FeatureView featureView) throws FeaturestoreException {
    
    // 1. fetch feature view statistics ids
    String fgsQueryStr =
      buildQuery("SELECT DISTINCT s.id from FeatureViewStatistics s LEFT JOIN s.featureDescriptiveStatistics fds ",
        filters, sorts, "s.featureView = :featureView AND fds.featureName " + fNsOperator + " :featureName");
    String fgsQueryCountStr = buildQuery(
      "SELECT COUNT(DISTINCT s.id) from FeatureViewStatistics s LEFT JOIN s.featureDescriptiveStatistics" + " fds ",
      filters, sorts, "s.featureView = :featureView AND fds.featureName " + fNsOperator + " :featureName");
    Query fgsQuery =
      em.createQuery(fgsQueryStr).setParameter("featureView", featureView).setParameter("featureName", fNsParamValue);
    Query fgsQueryCount = em.createQuery(fgsQueryCountStr).setParameter("featureView", featureView)
      .setParameter("featureName", fNsParamValue);
    StatisticsFilters.setFilter(filters, fgsQuery);
    setOffsetAndLim(offset, limit, fgsQuery);
    StatisticsFilters.setFilter(filters, fgsQueryCount);
    
    long count = (long) fgsQueryCount.getSingleResult();
    if (count == 0) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.STATISTICS_NOT_FOUND, Level.FINE,
        "Statistics for feature view id '" + featureView.getId() + "' not found. Please, try again with different " +
          "filters.");
    }
    Set<Integer> fgsIds = (Set<Integer>) fgsQuery.getResultList().stream().collect(Collectors.toSet());
    
    // 2. fetch feature view statistics with feature descriptive statistics
    String queryStr = buildQuery(
      "SELECT DISTINCT s from FeatureViewStatistics s LEFT JOIN FETCH s.featureDescriptiveStatistics " + "fds ",
      filters, sorts, "s.id IN :fgsIds AND fds.featureName " + fNsOperator + " :featureName");
    Query query = em.createQuery(queryStr, FeatureViewStatistics.class).setParameter("fgsIds", fgsIds)
      .setParameter("featureName", fNsParamValue);
    StatisticsFilters.setFilter(filters, query);
    
    return new CollectionInfo<>(count, query.getResultList());
  }
}