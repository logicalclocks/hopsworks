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

package io.hops.hopsworks.common.featurestore.trainingdatasets.external;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.s3.FeaturestoreS3Connector;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.external.ExternalTrainingDataset;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * A facade for the external_training_dataset table in the Hopsworks database,
 * use this interface when performing database operations against the table.
 */
@Stateless
public class ExternalTrainingDatasetFacade extends AbstractFacade<ExternalTrainingDataset> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public ExternalTrainingDatasetFacade() {
    super(ExternalTrainingDataset.class);
  }

  /**
   * Create and persist an external training dataset
   * @param connector
   * @param path
   * @return
   */
  public ExternalTrainingDataset createExternalTrainingDataset(FeaturestoreS3Connector connector, String path) {
    ExternalTrainingDataset externalTrainingDataset = new ExternalTrainingDataset();
    externalTrainingDataset.setFeaturestoreS3Connector(connector);
    externalTrainingDataset.setPath(path);
    em.persist(externalTrainingDataset);
    em.flush();
    return externalTrainingDataset;
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
