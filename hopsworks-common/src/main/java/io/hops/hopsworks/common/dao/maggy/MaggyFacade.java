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
package io.hops.hopsworks.common.dao.maggy;

import io.hops.hopsworks.persistence.entity.maggy.MaggyDriver;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A facade for storing information about Maggy Driver instances running in the cluster.
 */
@Stateless
public class MaggyFacade {
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  public List<MaggyDriver> findByAppId(String appId) {
    TypedQuery<MaggyDriver> query;
    query = em.createNamedQuery("MaggyDriver.findByAppId", MaggyDriver.class).setParameter("appId", appId);
    return query.getResultList();
  }
  
  public List<MaggyDriver> getAllDrivers() {
    List<MaggyDriver> res;
    TypedQuery<MaggyDriver> query = em.createNamedQuery("MaggyDriver.findAll", MaggyDriver.class);
    return query.getResultList();
  }
  
  public void remove(MaggyDriver driver) {
    if (driver != null) {
      em.remove(driver);
    }
  }
  
  public void removeByAppId(String appId) {
    List<MaggyDriver> driversToRemove = findByAppId(appId);
    driversToRemove.forEach(driver -> em.remove(driver));
  }
  
  public void add(MaggyDriver driver) {
    if (driver != null) {
      driver.setCreated(java.sql.Timestamp.valueOf(LocalDateTime.now()));
      em.persist(driver);
    }
  }
  
}
