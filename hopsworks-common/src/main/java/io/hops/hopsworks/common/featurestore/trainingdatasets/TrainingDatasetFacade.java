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

package io.hops.hopsworks.common.featurestore.trainingdatasets;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.featurestore.Featurestore;
import io.hops.hopsworks.common.dao.featurestore.trainingdataset.TrainingDataset;

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
   * Retrieves a list of trainingDataset (different versions) given their name and feature store from the database
   *
   * @param name name of the trainingDataset
   * @param featurestore featurestore of the trainingDataset
   * @return a single TrainingDataset entity
   */
  public List<TrainingDataset> findByNameAndFeaturestore(String name, Featurestore featurestore) {
    return em.createNamedQuery("TrainingDataset.findByFeaturestoreAndName", TrainingDataset.class)
          .setParameter("featurestore", featurestore)
          .setParameter("name", name)
          .getResultList();
  }

  /**
   * Retrieves a training dataset given its name, version and feature store from the database
   *
   * @param name name of the trainingDataset
   * @param version version of the trainingDataset
   * @param featurestore featurestore of the trainingDataset
   * @return a single TrainingDataset entity
   */
  public Optional<TrainingDataset> findByNameVersionAndFeaturestore(String name, Integer version,
                                                                    Featurestore featurestore) {
    try {
      return Optional.of(
          em.createNamedQuery("TrainingDataset.findByFeaturestoreAndNameVersion", TrainingDataset.class)
          .setParameter("featurestore", featurestore)
          .setParameter("name", name)
          .setParameter("version", version)
          .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
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
