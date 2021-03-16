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

package io.hops.hopsworks.common.featurestore.featuregroup;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.FeatureGroupExpectation;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.Optional;
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
  public Optional<Featuregroup> findById(Integer id) {
    try {
      return Optional.of(em.createNamedQuery("Featuregroup.findById", Featuregroup.class)
              .setParameter("id", id)
              .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
  
  /**
   * Retrieves a particular featuregroup given its Id and featurestore from the database
   *
   * @param id id of the featuregroup
   * @param featurestore featurestore of the featuregroup
   * @return a single Featuregroup entity
   */
  public Optional<Featuregroup> findByIdAndFeaturestore(Integer id, Featurestore featurestore) {
    try {
      return Optional.of(em.createNamedQuery("Featuregroup.findByFeaturestoreAndId", Featuregroup.class)
        .setParameter("featurestore", featurestore)
        .setParameter("id", id)
        .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  /**
   * Retrieves a list of featuregroups (different versions) given its name and featurestore from the database
   *
   * @param name name of the featuregroup
   * @param featurestore featurestore of the featuregroup
   * @return a single Featuregroup entity
   */
  public List<Featuregroup> findByNameAndFeaturestore(String name, Featurestore featurestore) {
    return em.createNamedQuery("Featuregroup.findByFeaturestoreAndName", Featuregroup.class)
          .setParameter("featurestore", featurestore)
          .setParameter("name", name)
          .getResultList();
  }
  
  /**
   * Retrieves a list of featuregroups (different versions) given its name and featurestore from the database
   * ordered by their version number in descending order
   *
   * @param name name of the featuregroup
   * @param featurestore featurestore of the featuregroup
   * @return a single Featuregroup entity
   */
  public List<Featuregroup> findByNameAndFeaturestoreOrderedDescVersion(String name, Featurestore featurestore) {
    return em.createNamedQuery("Featuregroup.findByFeaturestoreAndNameOrderedByDescVersion", Featuregroup.class)
      .setParameter("featurestore", featurestore)
      .setParameter("name", name)
      .getResultList();
  }

  /**
   * Retrieves a featuregroups given its name, version and feature store
   *
   * @param name name of the featuregroup
   * @param version version of the featurestore
   * @param featurestore featurestore of the featuregroup
   * @return a single Featuregroup entity
   */
  public Optional<Featuregroup> findByNameVersionAndFeaturestore(String name, Integer version,
                                                                 Featurestore featurestore) {
    try {
      return Optional.of(em.createNamedQuery("Featuregroup.findByFeaturestoreAndNameVersion", Featuregroup.class)
          .setParameter("featurestore", featurestore)
          .setParameter("version", version)
          .setParameter("name", name)
          .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
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

  public Long countByFeaturestore(Featurestore featurestore) {
    return em.createNamedQuery("Featuregroup.countByFeaturestore", Long.class)
        .setParameter("featurestore", featurestore)
        .getSingleResult();
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
   * @return the updated featuregroup entity
   */
  public Featuregroup updateFeaturegroupMetadata(Featuregroup featuregroup) {
    em.merge(featuregroup);
    return featuregroup;
  }

  public List<Featuregroup> findByFeatureStoreAndExpectations(Featurestore featurestore,
                                                              List<FeatureGroupExpectation> expectations) {
    TypedQuery<Featuregroup> q = em.createNamedQuery("Featuregroup.findByFeaturestoreAndExpectations",
                                                        Featuregroup.class)
                                    .setParameter("featurestore", featurestore)
                                    .setParameter("expectationsList", expectations);
    return q.getResultList();
  }

  public enum Filters {
    EXPECTATIONS("EXPECTATIONS", "", "expectations", "");

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
