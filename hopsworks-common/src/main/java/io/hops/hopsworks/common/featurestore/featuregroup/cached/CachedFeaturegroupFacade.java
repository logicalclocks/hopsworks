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

package io.hops.hopsworks.common.featurestore.featuregroup.cached;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.CachedFeaturegroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.HivePartitions;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.HiveTbls;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A facade for the cached_feature_group table in the Hopsworks database,
 * use this interface when performing database operations against the table.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class CachedFeaturegroupFacade extends AbstractFacade<CachedFeaturegroup> {
  private static final Logger LOGGER = Logger.getLogger(
    CachedFeaturegroupFacade.class.getName());
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public CachedFeaturegroupFacade() {
    super(CachedFeaturegroup.class);
  }

  /**
   * A transaction to persist a cached featuregroup for the featurestore in the database
   *
   * @param cachedFeaturegroup the cached featuregroup to persist
   */
  public void persist(CachedFeaturegroup cachedFeaturegroup) {
    try {
      em.persist(cachedFeaturegroup);
      em.flush();
    } catch (ConstraintViolationException cve) {
      LOGGER.log(Level.WARNING, "Could not persist the new cached feature group", cve);
      throw cve;
    }
  }

  /**
   * Gets the Hive TableId for the featuregroup and version and DB-ID by querying the metastore
   *
   * @param hiveTableName the id of the hive table
   * @param hiveDbId the id of the hive database
   * @return the hive table id
   */
  public Optional<HiveTbls> getHiveTableByNameAndDB(String hiveTableName, Long hiveDbId) {
    try {
      return Optional.of(em.createNamedQuery("HiveTable.findByNameAndDbId", HiveTbls.class)
          .setParameter("name", hiveTableName.toLowerCase())
          .setParameter("dbId", hiveDbId)
          .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public Optional<HiveTbls> getHiveTable(Long hiveTableId) {
    try {
      return Optional.of(em.createNamedQuery("HiveTable.findById", HiveTbls.class)
          .setParameter("id", hiveTableId)
          .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
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
  

  /**
   * Updates an existing Cached Feature Group
   *
   * @param cachedFeaturegroup the entity to update
   * @return the updated entity
   */
  public CachedFeaturegroup updateMetadata(CachedFeaturegroup cachedFeaturegroup) {
    em.merge(cachedFeaturegroup);
    return cachedFeaturegroup;
  }

  public List<HivePartitions> getHiveTablePartitions(HiveTbls hiveTbls, Integer limit, Integer offset) {
    Query q = em.createNamedQuery("Partitions.findByTbl", HivePartitions.class)
        .setParameter("tbls", hiveTbls);

    if (offset != null && offset > 0) {
      q.setFirstResult(offset);
    }
    if (limit != null && limit > 0) {
      q.setMaxResults(limit);
    }

    return q.getResultList();
  }
}
