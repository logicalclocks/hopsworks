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

package io.hops.hopsworks.common.featurestore.trainingdatasets.hopsfs;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.hopsfs.FeaturestoreHopsfsConnector;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.hopsfs.HopsfsTrainingDataset;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * A facade for the hopsfs_training_dataset table in the Hopsworks database,
 * use this interface when performing database operations against the table.
 */
@Stateless
public class HopsfsTrainingDatasetFacade extends AbstractFacade<HopsfsTrainingDataset> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public HopsfsTrainingDatasetFacade() {
    super(HopsfsTrainingDataset.class);
  }

  /**
   * Create and persiste a HopsFS training dataset
   * @param connector
   * @param inode
   * @return
   */
  public HopsfsTrainingDataset createHopsfsTrainingDataset(FeaturestoreHopsfsConnector connector, Inode inode) {
    HopsfsTrainingDataset hopsfsTrainingDataset = new HopsfsTrainingDataset();
    hopsfsTrainingDataset.setInode(inode);
    hopsfsTrainingDataset.setFeaturestoreHopsfsConnector(connector);
    em.persist(hopsfsTrainingDataset);
    em.flush();
    return hopsfsTrainingDataset;
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
