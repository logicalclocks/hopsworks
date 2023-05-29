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

import io.hops.hopsworks.persistence.entity.alertmanager.AlertReceiver;
import org.json.JSONObject;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

@Stateless
public class AlertReceiverFacade {
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  public void save(AlertReceiver entity) {
    em.persist(entity);
  }
  
  public AlertReceiver update(AlertReceiver entity) {
    return em.merge(entity);
  }
  
  public void remove(AlertReceiver entity) {
    if (entity == null) {
      return;
    }
    em.remove(em.merge(entity));
    em.flush();
  }
  
  public AlertReceiver find(Integer id) {
    return em.find(AlertReceiver.class, id);
  }
  
  public List<AlertReceiver> findAll() {
    TypedQuery<AlertReceiver> query = em.createNamedQuery("AlertReceiver.findAll",
        AlertReceiver.class);
    return query.getResultList();
  }
  
  public Optional<AlertReceiver> findByName(String name) {
    TypedQuery<AlertReceiver> query = em.createNamedQuery("AlertReceiver.findByName",
        AlertReceiver.class).setParameter("name", name);
    try {
      return Optional.of(query.getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
  
  public void saveReceiverToDatabase(String oldName, String newName, JSONObject jsonObject) {
    AlertReceiver alertReceiver;
    Optional<AlertReceiver> optionalAlertReceiver = findByName(oldName);
    if (optionalAlertReceiver.isPresent()) {
      alertReceiver = optionalAlertReceiver.get();
      alertReceiver.setName(newName);
      alertReceiver.setConfig(jsonObject);
      update(alertReceiver);
    } else {
      alertReceiver = new AlertReceiver(newName, jsonObject);
      save(alertReceiver);
    }
  }
  
  public void removeReceiverFromDatabase(String name) {
    Optional<AlertReceiver> optionalAlertReceiver = findByName(name);
    optionalAlertReceiver.ifPresent(this::remove);
  }
}
