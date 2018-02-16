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
