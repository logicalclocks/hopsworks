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
import io.hops.hopsworks.persistence.entity.featurestore.statistics.FeatureDescriptiveStatistics;
import org.javatuples.Pair;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A facade for the featurestore_statistic table in the Hopsworks database, use this interface when performing database
 * operations against the table.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class FeatureDescriptiveStatisticsFacade extends AbstractFacade<FeatureDescriptiveStatistics> {
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  public FeatureDescriptiveStatisticsFacade() {
    super(FeatureDescriptiveStatistics.class);
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
  
  public Optional<FeatureDescriptiveStatistics> findById(Integer id) {
    try {
      return Optional.of(
        em.createNamedQuery("FeatureDescriptiveStatistics.findById", FeatureDescriptiveStatistics.class)
          .setParameter("id", id).getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
  
  public List<FeatureDescriptiveStatistics> findOrphaned(Pair<Integer, Integer> range) {
    String queryString = "SELECT fds.* FROM hopsworks.feature_descriptive_statistics fds " +
      "WHERE NOT EXISTS (SELECT id FROM hopsworks.feature_group_descriptive_statistics WHERE " +
      "feature_descriptive_statistics_id = fds.id) " +
      "AND NOT EXISTS (SELECT id FROM hopsworks.training_dataset_descriptive_statistics WHERE " +
      "feature_descriptive_statistics_id = fds.id) " +
      "AND NOT EXISTS (SELECT id FROM hopsworks.test_dataset_descriptive_statistics WHERE " +
      "feature_descriptive_statistics_id = fds.id) " +
      "AND NOT EXISTS (SELECT id FROM hopsworks.val_dataset_descriptive_statistics WHERE " +
      "feature_descriptive_statistics_id = fds.id)";
    Query q = em.createNativeQuery(queryString, FeatureDescriptiveStatistics.class);
    if (range != null) {
      q.setMaxResults(range.getValue1() - range.getValue0());
      q.setFirstResult(range.getValue0());
    } else {
      q.setFirstResult(0);
      q.setMaxResults(Integer.MAX_VALUE);
    }
    return q.getResultList();
  }
  
  public int batchDelete(List<FeatureDescriptiveStatistics> fds) {
    List<Integer> fdsIds = fds.stream().map(FeatureDescriptiveStatistics::getId).collect(Collectors.toList());
    Query query = em.createNamedQuery("FeatureDescriptiveStatistics.deleteBatch", FeatureDescriptiveStatistics.class);
    query.setParameter("fdsIds", fdsIds);
    return query.executeUpdate();
  }
}