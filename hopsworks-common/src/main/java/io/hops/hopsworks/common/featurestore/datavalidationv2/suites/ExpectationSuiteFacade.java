/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
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


package io.hops.hopsworks.common.featurestore.datavalidationv2.suites;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.ExpectationSuite;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.validation.ConstraintViolationException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Optional;

/**
 * A facade for the expectation_suite table in the Hopsworks database, use this interface when performing database
 * operations against the table.
 */
@Stateless
public class ExpectationSuiteFacade extends AbstractFacade<ExpectationSuite> {
  private static final Logger LOGGER = Logger.getLogger(ExpectationSuiteFacade.class.getName());
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;


  public ExpectationSuiteFacade() {
    super(ExpectationSuite.class);
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
   * Transaction to create a new expectation suite in the database
   *
   * @param expectationSuite
   *   the expectation suite to persist
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void persist(ExpectationSuite expectationSuite) {
    try {
      em.persist(expectationSuite);
      em.flush();
    } catch (ConstraintViolationException cve) {
      LOGGER.log(Level.WARNING, "Could not persist the new ExpectationSuite", cve);
    }
  }

  public Optional<ExpectationSuite> findByFeaturegroup(Featuregroup featuregroup) {
    try {
      return Optional.of(em.createNamedQuery("ExpectationSuite.findByFeatureGroup", ExpectationSuite.class)
        .setParameter("featureGroup", featuregroup).getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public Optional<ExpectationSuite> findById(Integer expectationSuiteId) {
    try {
      return Optional.of(em.createNamedQuery("ExpectationSuite.findById", ExpectationSuite.class)
        .setParameter("expectationSuiteId", expectationSuiteId).getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  /**
   * Transaction to create a new expectation suite in the database
   *
   * @param expectationSuite
   *   the expectation to update
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void updateExpectationSuite(ExpectationSuite expectationSuite) {
    try {
      em.merge(expectationSuite);
      em.flush();
    } catch (ConstraintViolationException cve) {
      LOGGER.log(Level.WARNING, "Could not update the Expectation Suite", cve);
    }
  }
}
