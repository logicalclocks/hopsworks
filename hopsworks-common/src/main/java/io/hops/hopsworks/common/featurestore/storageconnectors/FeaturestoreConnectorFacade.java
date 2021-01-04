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

package io.hops.hopsworks.common.featurestore.storageconnectors;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnectorType;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class FeaturestoreConnectorFacade extends AbstractFacade<FeaturestoreConnector> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public FeaturestoreConnectorFacade() {
    super(FeaturestoreConnector.class);
  }

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public Optional<FeaturestoreConnector> findById(Integer id) {
    try {
      return Optional.of(em.createNamedQuery("FeaturestoreConnector.findById", FeaturestoreConnector.class)
          .setParameter("id", id)
          .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public Optional<FeaturestoreConnector> findByIdType(Integer id, FeaturestoreConnectorType type) {
    try {
      return Optional.of(em.createNamedQuery("FeaturestoreConnector.findByIdType", FeaturestoreConnector.class)
          .setParameter("id", id)
          .setParameter("type", type)
          .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public List<FeaturestoreConnector> findByFeaturestore(Featurestore featurestore) {
    return em.createNamedQuery("FeaturestoreConnector.findByFeaturestore", FeaturestoreConnector.class)
        .setParameter("featurestore", featurestore)
        .getResultList();
  }

  public Optional<FeaturestoreConnector> findByFeaturestoreId(Featurestore featurestore, Integer id) {
    try {
      return Optional.of(em.createNamedQuery("FeaturestoreConnector.findByFeaturestoreId", FeaturestoreConnector.class)
          .setParameter("featurestore", featurestore)
          .setParameter("id", id)
          .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public Optional<FeaturestoreConnector> findByFeaturestoreName(Featurestore featurestore, String name) {
    try {
      return Optional.of(em.createNamedQuery("FeaturestoreConnector.findByFeaturestoreName",
          FeaturestoreConnector.class)
          .setParameter("featurestore", featurestore)
          .setParameter("name", name)
          .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public void deleteByFeaturestoreName(Featurestore featurestore, String name) {
    findByFeaturestoreName(featurestore, name).ifPresent(this::remove);
  }

  public Long countByFeaturestore(Featurestore featurestore) {
    return em.createNamedQuery("FeaturestoreConnector.countByFeaturestore", Long.class)
        .setParameter("featurestore", featurestore)
        .getSingleResult();
  }
}
