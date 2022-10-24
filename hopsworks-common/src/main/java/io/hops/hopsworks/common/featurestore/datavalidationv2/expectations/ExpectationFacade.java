/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Hopsworks AB. All rights reserved
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


package io.hops.hopsworks.common.featurestore.datavalidationv2.expectations;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.featurestore.datavalidationv2.suites.ExpectationSuiteFacade;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.Expectation;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.ExpectationSuite;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.validation.ConstraintViolationException;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Optional;
import javax.persistence.NoResultException;

/**
 * A facade for the expectation_suite table in the Hopsworks database, use this interface when performing database
 * operations against the table.
 */
@Stateless
public class ExpectationFacade extends AbstractFacade<Expectation> {
  private static final Logger LOGGER = Logger.getLogger(ExpectationSuiteFacade.class.getName());
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;


  public ExpectationFacade() {
    super(Expectation.class);
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
   * Retrieves a particular expectation given its Id from the database
   *
   * @param id
   *   id of the Expectation
   * @return a single GreatExpectation entity
   */
  public Optional<Expectation> findById(Integer id) {
    try {
      return Optional.of(em.createNamedQuery("Expectation.findById", Expectation.class).setParameter("id", id)
        .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  /**
   * Retrieves all expectations which belong to the Expectation Suite
   *
   * @param expectationSuite expectation suite 
   * @return a single GreatExpectation entity
   */
  public CollectionInfo<Expectation> findByExpectationSuite(ExpectationSuite expectationSuite) {
    String queryStr = "SELECT expec from Expectation expec WHERE expec.expectationSuite = :expectationSuite";
    String queryCountStr = "SELECT COUNT(expec.id) from Expectation expec" + 
      " WHERE expec.expectationSuite = :expectationSuite";
    Query query = em.createQuery(queryStr, Expectation.class)
        .setParameter("expectationSuite", expectationSuite);
    Query queryCount = em.createQuery(queryCountStr, Expectation.class)
        .setParameter("expectationSuite", expectationSuite);
    return new CollectionInfo<Expectation>((Long) queryCount.getSingleResult(), query.getResultList());
  }


  /**
   * Transaction to create a new expectation in the database
   *
   * @param expectation
   *   the expectation to persist
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void createExpectation(Expectation expectation) {
    try {
      em.persist(expectation);
      em.flush();
    } catch (ConstraintViolationException cve) {
      LOGGER.log(Level.WARNING, "Could not persist the new Expectation", cve);
    }
  }

  /**
   * Transaction to update an expectation in the database
   *
   * @param expectation
   *   the expectation to update
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void updateExpectation(Expectation expectation) {
    try {
      em.merge(expectation);
      em.flush();
    } catch (ConstraintViolationException cve) {
      LOGGER.log(Level.WARNING, "Could not update the Expectation", cve);
    }
  }
}
