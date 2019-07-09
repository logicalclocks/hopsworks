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

package io.hops.hopsworks.common.dao.featurestore.trainingdataset;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.featurestore.Featurestore;

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
 * A facade for the training_dataset table in the Hopsworks database, use this interface when performing database
 * operations against the table.
 */
@Stateless
public class TrainingDatasetFacade extends AbstractFacade<TrainingDataset> {
  private static final Logger LOGGER = Logger.getLogger(TrainingDatasetFacade.class.getName());
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public TrainingDatasetFacade() {
    super(TrainingDataset.class);
  }

  /**
   * Retrieves a particular TrainingDataset given its Id from the database
   *
   * @param id id of the TrainingDataset
   * @return a single TrainingDataset entity
   */
  public TrainingDataset findById(Integer id) {
    try {
      return em.createNamedQuery("TrainingDataset.findById", TrainingDataset.class)
          .setParameter("id", id).getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * Retrieves a particular trainingDataset given its Id and featurestore from the database
   *
   * @param id           id of the trainingDataset
   * @param featurestore featurestore of the trainingDataset
   * @return a single TrainingDataset entity
   */
  public TrainingDataset findByIdAndFeaturestore(Integer id, Featurestore featurestore) {
    try {
      return em.createNamedQuery("TrainingDataset.findByFeaturestoreAndId", TrainingDataset.class)
          .setParameter("featurestore", featurestore)
          .setParameter("id", id)
          .getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * Retrieves all trainingDatasets from the database
   *
   * @return list of trainingDataset entities
   */
  @Override
  public List<TrainingDataset> findAll() {
    TypedQuery<TrainingDataset> q = em.createNamedQuery("TrainingDataset.findAll", TrainingDataset.class);
    return q.getResultList();
  }

  /**
   * Retrieves all trainingDatasets for a particular featurestore
   *
   * @param featurestore
   * @return
   */
  public List<TrainingDataset> findByFeaturestore(Featurestore featurestore) {
    TypedQuery<TrainingDataset> q = em.createNamedQuery("TrainingDataset.findByFeaturestore", TrainingDataset.class)
        .setParameter("featurestore", featurestore);
    return q.getResultList();
  }

  /**
   * Transaction to create a new trainingDataset in the database
   *
   * @param trainingDataset the trainingDataset to persist
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void persist(TrainingDataset trainingDataset) {
    try {
      em.persist(trainingDataset);
      em.flush();
    } catch (ConstraintViolationException cve) {
      LOGGER.log(Level.WARNING, "Could not persist the new TrainingDataset", cve);
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
   * Updates metadata about a training dataset
   *
   * @param trainingDataset      the training dataset to update
   * @return
   */
  public TrainingDataset updateTrainingDatasetMetadata(TrainingDataset trainingDataset) {
    em.merge(trainingDataset);
    return trainingDataset;
  }
}
