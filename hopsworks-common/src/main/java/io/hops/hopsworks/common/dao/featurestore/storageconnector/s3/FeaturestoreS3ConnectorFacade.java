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

package io.hops.hopsworks.common.dao.featurestore.storageconnector.s3;

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
 * A facade for the feature_store_s3_connector table in the Hopsworks database,
 * use this interface when performing database operations against the table.
 */
@Stateless
public class FeaturestoreS3ConnectorFacade extends AbstractFacade<FeaturestoreS3Connector> {
  private static final Logger LOGGER = Logger.getLogger(
    FeaturestoreS3ConnectorFacade.class.getName());
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public FeaturestoreS3ConnectorFacade() {
    super(FeaturestoreS3Connector.class);
  }

  /**
   * A transaction to persist a s3 connection for the featurestore in the database
   *
   * @param featurestoreS3Connector the featurestore s3 connection to persist
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void persist(FeaturestoreS3Connector featurestoreS3Connector) {
    try {
      em.persist(featurestoreS3Connector);
      em.flush();
    } catch (ConstraintViolationException cve) {
      LOGGER.log(Level.WARNING, "Could not persist the new s3 connection", cve);
      throw cve;
    }
  }

  /**
   * Retrieves all s3 connectors for a particular featurestore
   *
   * @param featurestore featurestore get s3 connectors for
   * @return list of s3 connectors
   */
  public List<FeaturestoreS3Connector> findByFeaturestore(Featurestore featurestore) {
    TypedQuery<FeaturestoreS3Connector> q = em.createNamedQuery("FeaturestoreS3Connector.findByFeaturestore",
        FeaturestoreS3Connector.class).setParameter("featurestore", featurestore);
    return q.getResultList();
  }

  /**
   * Retrieves a particular s3 connector given its Id and featurestore from the database
   *
   * @param id           id of the s3 connector
   * @param featurestore featurestore of the connector
   * @return a single FeaturestoreS3Connector entity
   */
  public FeaturestoreS3Connector findByIdAndFeaturestore(Integer id, Featurestore featurestore) {
    try {
      return em.createNamedQuery("FeaturestoreS3Connector.findByFeaturestoreAndId", FeaturestoreS3Connector.class)
          .setParameter("featurestore", featurestore)
          .setParameter("id", id)
          .getSingleResult();
    } catch (NoResultException e) {
      return null;
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
