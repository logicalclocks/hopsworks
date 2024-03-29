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

package io.hops.hopsworks.common.featurestore;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.project.Project;

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
 * A facade for the feature_store table in the Hopsworks database, use this interface when performing database
 * operations against the table.
 */
@Stateless
public class FeaturestoreFacade extends AbstractFacade<Featurestore> {
  private static final Logger LOGGER = Logger.getLogger(FeaturestoreFacade.class.getName());
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public FeaturestoreFacade() {
    super(Featurestore.class);
  }

  /**
   * Retrieves all featurestores from the database
   *
   * @return list of featurestores
   */
  @Override
  public List<Featurestore> findAll() {
    TypedQuery<Featurestore> q = em.createNamedQuery("Featurestore.findAll", Featurestore.class);
    return q.getResultList();
  }

  /**
   * Retrieves the featurestores for a particular project from the database
   *
   * @param project the project to get featurestores for
   * @return list of featurestores for the project
   */
  public List<Featurestore> findByProject(Project project) {
    TypedQuery<Featurestore> q = em.createNamedQuery("Featurestore.findByProject", Featurestore.class)
        .setParameter("project", project);
    return q.getResultList();
  }

  /**
   * Retrieves a featurestore with a specific Id from the database
   *
   * @param id if of the featurestore
   * @return featurestore entity with the given id
   */
  public Featurestore findById(Integer id) {
    try {
      return em.createNamedQuery("Featurestore.findById", Featurestore.class)
          .setParameter("id", id).getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * A transaction to persist a featurestore in the database
   *
   * @param featurestore the featurestore to persist
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void persist(Featurestore featurestore) {
    try {
      em.persist(featurestore);
      em.flush();
    } catch (ConstraintViolationException cve) {
      LOGGER.log(Level.WARNING, "Could not persist the new Featurestore", cve);
      throw cve;
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
}
