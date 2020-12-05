/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.dao.featurestore.storageconnectors.redshift;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.redshift.FeatureStoreRedshiftConnector;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

@Stateless
public class FeaturestoreRedshiftConnectorFacade extends AbstractFacade<FeatureStoreRedshiftConnector> {
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  public FeaturestoreRedshiftConnectorFacade() {
    super(FeatureStoreRedshiftConnector.class);
  }
  
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }
  
  public List<FeatureStoreRedshiftConnector> findByFeaturestore(Featurestore featurestore) {
    TypedQuery<FeatureStoreRedshiftConnector> q =
      em.createNamedQuery("FeatureStoreRedshiftConnector.findByFeaturestore", FeatureStoreRedshiftConnector.class)
        .setParameter("featurestore", featurestore);
    return q.getResultList();
  }
  
  public Optional<FeatureStoreRedshiftConnector> findByIdAndFeaturestore(Integer id, Featurestore featurestore) {
    try {
      return Optional.of(em.createNamedQuery("FeatureStoreRedshiftConnector.findByFeaturestoreAndId",
        FeatureStoreRedshiftConnector.class).setParameter("featurestore", featurestore).setParameter("id", id)
        .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
  
  public Optional<FeatureStoreRedshiftConnector> findByNameAndFeaturestore(String name, Featurestore featurestore) {
    try {
      return Optional.of(em.createNamedQuery("FeatureStoreRedshiftConnector.findByNameAndFeaturestore",
        FeatureStoreRedshiftConnector.class).setParameter("name", name).setParameter("featurestore", featurestore)
        .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
}
