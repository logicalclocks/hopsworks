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
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A facade for storing information about Maggy Driver instances running in the cluster.
 */
@Stateless
public class MaggyFacade {
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  public MaggyDriver findByAppId(String appId) {
    TypedQuery<MaggyDriver> query;
    try {
      query = em.createNamedQuery("MaggyDriver.findByAppId", MaggyDriver.class).setParameter("appId", appId);
    } catch (EntityNotFoundException | NoResultException e) {
      Logger.getLogger(MaggyFacade.class.getName()).log(Level.WARNING, null, e);
      return null;
    } catch (Throwable e) {
      Logger.getLogger(MaggyFacade.class.getName()).log(Level.WARNING, null, e);
      e.printStackTrace();
      return null;
    }
    return query.getSingleResult();
  }
  
  
  public List<MaggyDriver> getAllDrivers() {
    List<MaggyDriver> res;
    try {
      TypedQuery<MaggyDriver> query = em.createNamedQuery("MaggyDriver.findAll", MaggyDriver.class);
      res = query.getResultList();
    } catch (EntityNotFoundException | NoResultException e) {
      Logger.getLogger(MaggyFacade.class.getName()).log(Level.WARNING, null, e);
      return null;
    } catch (Throwable e) {
      Logger.getLogger(MaggyFacade.class.getName()).log(Level.WARNING, null, e);
      e.printStackTrace();
      return null;
    }
    return res;
  }
  
  public void remove(MaggyDriver driver) {
    if (driver != null) {
      em.remove(driver);
    }
  }
  
  public void add(MaggyDriver driver) {
    if (driver != null) {
      driver.setCreated(java.sql.Timestamp.valueOf(LocalDateTime.now()));
      em.persist(driver);
    }
  }
  
}
