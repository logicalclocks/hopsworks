/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.dao.hdfsUser;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import io.hops.hopsworks.common.dao.AbstractFacade;
import java.util.ArrayList;
import java.util.List;

@Stateless
public class HdfsUsersFacade extends AbstractFacade<HdfsUsers> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public HdfsUsersFacade() {
    super(HdfsUsers.class);
  }

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public HdfsUsers findHdfsUser(byte[] id) {
    return em.find(HdfsUsers.class, id);
  }

  public HdfsUsers findByName(String name) {
    try {
      return em.createNamedQuery("HdfsUsers.findByName", HdfsUsers.class).
              setParameter(
                      "name", name).getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public List<HdfsUsers> findProjectUsers(String projectName) {
    List<HdfsUsers> users = null;
    try {
      users = em.createNamedQuery("HdfsUsers.findProjectUsers", HdfsUsers.class).setParameter("name",
          projectName).getResultList();
    } catch (NoResultException e) {
    }
    try {
      HdfsUsers user = em.createNamedQuery("HdfsUsers.findByName", HdfsUsers.class).setParameter("name", projectName).
          getSingleResult();
      if (user != null) {
        if (users == null) {
          users = new ArrayList<>();
        }
        users.add(user);
      }
    } catch (NoResultException e) {
    }
    return users;    
  }
 
  public void persist(HdfsUsers user) {
    em.persist(user);
  }

  public void merge(HdfsUsers user) {
    em.merge(user);
  }

  public void removeHdfsUser(HdfsUsers user) {
    if (user == null) {
      return;
    }
    HdfsUsers u = em.find(HdfsUsers.class, user.getId());
    if (u != null) {
      em.remove(u);
    }
  }
}
