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
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class HdfsGroupsFacade extends AbstractFacade<HdfsGroups> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public HdfsGroupsFacade() {
    super(HdfsGroups.class);
  }

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public HdfsGroups findHdfsGroup(byte[] id) {
    return em.find(HdfsGroups.class, id);
  }

  public HdfsGroups findByName(String name) {
    try {
      return em.createNamedQuery("HdfsGroups.findByName", HdfsGroups.class).
              setParameter(
                      "name", name).getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public void persist(HdfsGroups user) {
    Logger.getLogger(HdfsUsersFacade.class.getName()).
                    log(Level.SEVERE, "persist group " + user.getName());
    em.persist(user);
  }

  public void merge(HdfsGroups user) {
    Logger.getLogger(HdfsUsersFacade.class.getName()).
                    log(Level.SEVERE, "merge group " + user.getName());
    em.merge(user);
    em.flush();
  }

  @Override
  public void remove(HdfsGroups group) {
    HdfsGroups g = em.find(HdfsGroups.class, group.getId());
    if (g != null) {
      em.createNamedQuery("HdfsGroups.delete", HdfsGroups.class).
              setParameter("id", group.getId()).executeUpdate();
    }
  }
  
  public List<HdfsGroups> findProjectGroups(String projectName) {
    List<HdfsGroups> groups = null;
    try {
      groups = em.createNamedQuery("HdfsGroups.findProjectGroups", HdfsGroups.class).
              setParameter("name", projectName).
              getResultList();
    } catch (NoResultException e) {
      return null;
    }
    try{
      HdfsGroups group = em.createNamedQuery("HdfsGroups.findByName", HdfsGroups.class).
          setParameter("name", projectName).getSingleResult();
      if(group!=null){
        if(groups==null){
          groups = new ArrayList<>();
        }
        groups.add(group);
      }
    } catch (NoResultException e){ 
    }
    return groups;
  }
}
