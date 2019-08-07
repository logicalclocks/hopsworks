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

package io.hops.hopsworks.common.dao.featurestore.trainingdataset.hopsfs_trainingdataset;

import io.hops.hopsworks.common.dao.AbstractFacade;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.validation.ConstraintViolationException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A facade for the hopsfs_training_dataset table in the Hopsworks database,
 * use this interface when performing database operations against the table.
 */
@Stateless
public class HopsfsTrainingDatasetFacade extends AbstractFacade<HopsfsTrainingDataset> {
  private static final Logger LOGGER = Logger.getLogger(
    HopsfsTrainingDatasetFacade.class.getName());
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public HopsfsTrainingDatasetFacade() {
    super(HopsfsTrainingDataset.class);
  }

  /**
   * A transaction to persist a hopsfs training dataset for the featurestore in the database
   *
   * @param hopsfsTrainingDataset the hopsfs training dataset to persist
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void persist(HopsfsTrainingDataset hopsfsTrainingDataset) {
    try {
      em.persist(hopsfsTrainingDataset);
      em.flush();
    } catch (ConstraintViolationException cve) {
      LOGGER.log(Level.WARNING, "Could not persist the new hopsfs training dataset", cve);
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
