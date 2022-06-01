/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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


package io.hops.hopsworks.common.featurestore.datavalidationv2;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.GreatExpectation;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import java.util.logging.Logger;
import java.util.List;
import java.util.Optional;
import javax.persistence.NoResultException;

/**
 * A facade for the expectation_suite table in the Hopsworks database, use this interface when performing database
 * operations against the table.
 */
@Stateless
public class GreatExpectationFacade extends AbstractFacade<GreatExpectation> {
  private static final Logger LOGGER = Logger.getLogger(ExpectationSuiteFacade.class.getName());
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;


  public GreatExpectationFacade() {
    super(GreatExpectation.class);
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
   * Retrieves a particular great expectation given its Id from the database
   *
   * @param id
   *   id of the GreatExpectation
   * @return a single GreatExpectation entity
   */
  public Optional<GreatExpectation> findById(Integer id) {
    try {
      return Optional.of(em.createNamedQuery("GreatExpectation.findById", GreatExpectation.class).setParameter("id", id)
        .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  /**
   * Retrieves all GreatExpectations from the database
   *
   * @return collection of Great Expectation entities
   */
  public CollectionInfo<GreatExpectation> findAllGreatExpectation() {
    TypedQuery<GreatExpectation> query = em.createNamedQuery("GreatExpectation.findAll", GreatExpectation.class);
    TypedQuery<Long> queryCount = em.createNamedQuery("GreatExpectation.countAll", Long.class);

    return new CollectionInfo<GreatExpectation>((Long) queryCount.getSingleResult(), query.getResultList());
  }

  /**
   * Retrieves a particular great expectation based on expectation type
   *
   * @param expectationType
   *   of the GreatExpectation
   * @return a single GreatExpectation entity
   */
  public Optional<GreatExpectation> findByExpectationType(String expectationType) {
    try {
      return Optional.of(em.createNamedQuery("GreatExpectation.findByExpectationType", GreatExpectation.class)
        .setParameter("expectationType", expectationType).getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  /**
   * Retrieves a particular great expectation based on partial match expectation type
   *
   * @param partialExpectationType
   *   for a GreatExpectation
   * @return a list of GreatExpectation entities
   */
  public List<GreatExpectation> findByPartialExpectationType(String partialExpectationType) {
    return em.createNamedQuery("GreatExpectation.findByExpectationType", GreatExpectation.class)
      .setParameter("partialExpectationType", partialExpectationType).getResultList();
  }

  /* TODOs (no emergency just ideas to improve the facade. Can be moved to a github issue instead of code comment
  when cleaning up)
  There is potential value in enabling the retrieval of GE templates based on whether: 
    - Spark/Pandas engine implemented
    - FeatureType they can be applied to e.g categorical vs numerical
    - Favourite list
  */
}
