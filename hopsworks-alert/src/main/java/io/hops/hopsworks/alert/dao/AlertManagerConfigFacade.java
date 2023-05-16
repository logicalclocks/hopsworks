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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hops.hopsworks.alerting.config.dto.AlertManagerConfig;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigReadException;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertManagerConfigEntity;
import org.json.JSONObject;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.Date;
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

  public List<AlertManagerConfigEntity> findAllSortedByCreated() {
    TypedQuery<AlertManagerConfigEntity> query = em.createNamedQuery("AlertManagerConfigEntity.findAllSortedByCreated",
      AlertManagerConfigEntity.class);
    return query.getResultList();
  }

  public Optional<AlertManagerConfigEntity> getLatest() {
    List<AlertManagerConfigEntity> alertManagerConfigEntities = findAllSortedByCreated();
    if (alertManagerConfigEntities == null || alertManagerConfigEntities.size() < 1) {
      return Optional.empty();
    } else {
      return Optional.of(alertManagerConfigEntities.get(0));
    }
  }

  public Optional<AlertManagerConfig> read(ObjectMapper objectMapper) throws AlertManagerConfigReadException {
    //First read from database
    Optional<AlertManagerConfigEntity> optionalAlertManagerConfigEntity = getLatest();
    if (optionalAlertManagerConfigEntity.isPresent()) {
      try {
        JSONObject jsonAlertManagerConfigBackup = optionalAlertManagerConfigEntity.get().getContent();
        return Optional.of(objectMapper.readValue(jsonAlertManagerConfigBackup.toString(), AlertManagerConfig.class));
      } catch (Exception e) {
        throw new AlertManagerConfigReadException(
          "Failed to revert alert manger config from database. " + e.getMessage(), e);
      }
    }
    return Optional.empty();
  }

  // This should be globally locked by transaction or Write lock
  public void saveToDatabase(JSONObject jsonObject) {
    Optional<AlertManagerConfigEntity> optionalAlertManagerConfigEntity = getLatest();
    AlertManagerConfigEntity alertManagerConfigEntity;
    if (!optionalAlertManagerConfigEntity.isPresent()) {
      alertManagerConfigEntity = new AlertManagerConfigEntity();
      alertManagerConfigEntity.setContent(jsonObject);
      alertManagerConfigEntity.setCreated(new Date());
      save(alertManagerConfigEntity);
    } else {
      alertManagerConfigEntity = optionalAlertManagerConfigEntity.get();
      alertManagerConfigEntity.setContent(jsonObject);
      alertManagerConfigEntity.setCreated(new Date());
      update(alertManagerConfigEntity);
    }
  }

}
