/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
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
