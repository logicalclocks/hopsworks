/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.dao.featurestore.featuregroup;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.featurestore.Featurestore;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A facade for the feature_group table in the Hopsworks database, use this interface when performing database
 * operations against the table.
 */
@Stateless
public class FeaturegroupFacade extends AbstractFacade<Featuregroup> {
  private static final Logger LOGGER = Logger.getLogger(FeaturegroupFacade.class.getName());
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  /**
   * Class constructor, invoke parent class and initialize Hive Queries
   */
  public FeaturegroupFacade() {
    super(Featuregroup.class);
  }
  
  /**
   * Retrieves a particular featuregroup given its Id from the database
   *
   * @param id id of the featuregroup
   * @return a single Featuregroup entity
   */
  public Featuregroup findById(Integer id) {
    try {
      return em.createNamedQuery("Featuregroup.findById", Featuregroup.class)
        .setParameter("id", id).getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  /**
   * Retrieves a particular featuregroup given its Id and featurestore from the database
   *
   * @param id id of the featuregroup
   * @param featurestore featurestore of the featuregroup
   * @return a single Featuregroup entity
   */
  public Featuregroup findByIdAndFeaturestore(Integer id, Featurestore featurestore) {
    try {
      return em.createNamedQuery("Featuregroup.findByFeaturestoreAndId", Featuregroup.class)
        .setParameter("featurestore", featurestore)
        .setParameter("id", id)
        .getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  /**
   * Retrieves all featuregroups from the database
   *
   * @return list of featuregroup entities
   */
  @Override
  public List<Featuregroup> findAll() {
    TypedQuery<Featuregroup> q = em.createNamedQuery("Featuregroup.findAll", Featuregroup.class);
    return q.getResultList();
  }
  
  /**
   * Retrieves all featuregroups for a particular featurestore
   *
   * @param featurestore the featurestore to query
   * @return list of featuregroup entities
   */
  public List<Featuregroup> findByFeaturestore(Featurestore featurestore) {
    TypedQuery<Featuregroup> q = em.createNamedQuery("Featuregroup.findByFeaturestore", Featuregroup.class)
      .setParameter("featurestore", featurestore);
    return q.getResultList();
  }
  
  /**
   * Transaction to create a new featuregroup in the database
   *
   * @param featuregroup the featuregroup to persist
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void persist(Featuregroup featuregroup) {
    try {
      em.persist(featuregroup);
      em.flush();
    } catch (ConstraintViolationException cve) {
      LOGGER.log(Level.WARNING, "Could not persist the new Featuregroup", cve);
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
   * Updates metadata about a featuregroup (since only metadata is changed, the Hive table does not need
   * to be modified)
   *
   * @param featuregroup the featuregroup to update
   * @param job the new job of the featuregroup
   * @return the updated featuregroup entity
   */
  public Featuregroup updateFeaturegroupMetadata(
    Featuregroup featuregroup, Jobs job) {
    featuregroup.setJob(job);
    em.merge(featuregroup);
    return featuregroup;
  }
  
}
