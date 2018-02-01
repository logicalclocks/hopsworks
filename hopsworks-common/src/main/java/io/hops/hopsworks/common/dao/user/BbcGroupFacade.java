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
