/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.alert.dao;

import io.hops.hopsworks.persistence.entity.alertmanager.AlertManagerConfigEntity;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

@Stateless
public class AlertManagerConfigFacade {
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public void save(AlertManagerConfigEntity entity) {
    em.persist(entity);
  }

  public AlertManagerConfigEntity update(AlertManagerConfigEntity entity) {
    return em.merge(entity);
  }

  public void remove(AlertManagerConfigEntity entity) {
    if (entity == null) {
      return;
    }
    em.remove(em.merge(entity));
    em.flush();
  }

  public AlertManagerConfigEntity find(Integer id) {
    return em.find(AlertManagerConfigEntity.class, id);
  }

  public List<AlertManagerConfigEntity> findAll() {
    TypedQuery<AlertManagerConfigEntity> query = em.createNamedQuery("AlertManagerConfigEntity.findAll",
        AlertManagerConfigEntity.class);
    return query.getResultList();
  }

  public Optional<AlertManagerConfigEntity> getLatest() {
    List<AlertManagerConfigEntity> alertManagerConfigEntities = findAll();
    if (alertManagerConfigEntities == null || alertManagerConfigEntities.size() < 1) {
      return Optional.empty();
    } else {
      return Optional.of(alertManagerConfigEntities.get(0));
    }
  }
}
