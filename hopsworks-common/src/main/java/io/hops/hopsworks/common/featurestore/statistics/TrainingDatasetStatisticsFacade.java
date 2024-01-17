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
import io.hops.hopsworks.persistence.entity.featurestore.statistics.TrainingDatasetStatistics;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

/**
 * A facade for the featurestore_statistic table in the Hopsworks database, use this interface when performing database
 * operations against the table.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class TrainingDatasetStatisticsFacade extends AbstractFacade<TrainingDatasetStatistics> {
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  public TrainingDatasetStatisticsFacade() {
    super(TrainingDatasetStatistics.class);
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
  
  public CollectionInfo<TrainingDatasetStatistics> findByTrainingDataset(Set<? extends FilterBy> filters,
    TrainingDataset trainingDataset) {
    // multiple training dataset statistics are not supported. Only one statistics per TD is expected.
    String queryStr =
      buildQuery("SELECT s from TrainingDatasetStatistics s ", filters, null, "s.trainingDataset = :trainingDataset");
    String queryCountStr = buildQuery("SELECT COUNT(s.id) from TrainingDatasetStatistics s ", filters, null,
      "s.trainingDataset = " + ":trainingDataset");
    Query query =
      em.createQuery(queryStr, TrainingDatasetStatistics.class).setParameter("trainingDataset", trainingDataset);
    Query queryCount =
      em.createQuery(queryCountStr, TrainingDatasetStatistics.class).setParameter("trainingDataset", trainingDataset);
    StatisticsFilters.setFilter(filters, query);
    StatisticsFilters.setFilter(filters, queryCount);
    return new CollectionInfo<>((Long) queryCount.getSingleResult(), query.getResultList());
  }
  
  public CollectionInfo<TrainingDatasetStatistics> findByTrainingDatasetWithFeatureNames(
    Set<? extends FilterBy> filters, Set<String> featureNames, TrainingDataset trainingDataset)
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
    // retrieve training dataset statistics with train split feature descriptive statistics
    List<TrainingDatasetStatistics> tdss =
      findByTrainingDatasetSplitWithFeatureNames(filters, fNsOperator, fNsParamValue,
        "trainFeatureDescriptiveStatistics", trainingDataset);
    if (tdss.isEmpty()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.STATISTICS_NOT_FOUND, Level.FINE,
        "Statistics for training dataset id '" + trainingDataset.getId() + "' not found. Please, try again with" +
          " different filters.");
    }
    
    TrainingDatasetStatistics tds = tdss.get(0);
    detach(tds); // detach object from entity manager context
    
    // if no filter of statistics before transformation functions, retrieve split statistics
    Optional<? extends FilterBy> forTransFilter = filters.stream()
      .filter(f -> f.getValue().startsWith(StatisticsFilters.Filters.BEFORE_TRANSFORMATION_EQ.getValue()))
      .findAny();
    if (forTransFilter.isPresent() && !Boolean.parseBoolean(forTransFilter.get().getParam())) {
      int numSplits = trainingDataset.getSplits().size();
      if (numSplits > 1) { // add test split feature descriptive statistics
        List<TrainingDatasetStatistics> testFDS =
          findByTrainingDatasetSplitWithFeatureNames(filters, fNsOperator, fNsParamValue,
            "testFeatureDescriptiveStatistics", trainingDataset);
        tds.setTestFeatureDescriptiveStatistics(testFDS.get(0).getTestFeatureDescriptiveStatistics());
      }
      if (numSplits > 2) { // add validation split feature descriptive statistics
        List<TrainingDatasetStatistics> valFDS =
          findByTrainingDatasetSplitWithFeatureNames(filters, fNsOperator, fNsParamValue,
            "valFeatureDescriptiveStatistics", trainingDataset);
        tds.setValFeatureDescriptiveStatistics(valFDS.get(0).getValFeatureDescriptiveStatistics());
      }
    }
    return new CollectionInfo<>(1L, Collections.singletonList(tds));
  }
  
  private List<TrainingDatasetStatistics> findByTrainingDatasetSplitWithFeatureNames(Set<? extends FilterBy> filters,
    String fNsOperator, Object fNsParamValue, String splitCollectionName, TrainingDataset trainingDataset) {
    String queryStr = buildQuery(
      "SELECT DISTINCT s from TrainingDatasetStatistics s LEFT JOIN FETCH s" + "." + splitCollectionName + " fds ",
      filters, null, "s.trainingDataset = :trainingDataset AND fds.featureName " + fNsOperator + " :featureName");
    Query query =
      em.createQuery(queryStr, TrainingDatasetStatistics.class).setParameter("trainingDataset", trainingDataset)
        .setParameter("featureName", fNsParamValue);
    StatisticsFilters.setFilter(filters, query);
    return query.getResultList();
  }
}