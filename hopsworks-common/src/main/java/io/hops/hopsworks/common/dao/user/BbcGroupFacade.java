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

package io.hops.hopsworks.common.dao.user;

import java.util.ArrayList;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import io.hops.hopsworks.common.dao.AbstractFacade;
import javax.persistence.Query;

@Stateless
public class BbcGroupFacade extends AbstractFacade<BbcGroup> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public BbcGroupFacade() {
    super(BbcGroup.class);
  }

  public BbcGroup findByGroupName(String name) {
    try {
      return em.createNamedQuery("BbcGroup.findByGroupName", BbcGroup.class)
              .setParameter("groupName", name).getSingleResult();
    } catch (javax.persistence.NoResultException e) {
      return null;
    }
  }

  public List<BbcGroup> findAll() {
    List<BbcGroup> allGroups = em.createNamedQuery("BbcGroup.findAll",
            BbcGroup.class).getResultList();
    List<BbcGroup> updated = new ArrayList<>();
    if (allGroups != null) {
      for (BbcGroup g : allGroups) {
        if (g.getGroupName().compareTo("AGENT") != 0) {
          updated.add(g);
        }
      }
    }
    return updated;
  }
  
  public Integer lastGroupID() {
    Query query = em.createNativeQuery("SELECT MAX(b.gid) FROM hopsworks.bbc_group b");
    Object obj = query.getSingleResult();
    return (Integer) obj;
  }

}
