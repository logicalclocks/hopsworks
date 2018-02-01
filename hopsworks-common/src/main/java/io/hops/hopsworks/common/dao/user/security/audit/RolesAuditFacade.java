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

package io.hops.hopsworks.common.dao.user.security.audit;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.user.Users;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Stateless
public class RolesAuditFacade extends AbstractFacade<RolesAudit> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public RolesAuditFacade() {
    super(RolesAudit.class);
  }

  public List<RolesAudit> findByInitiator(Users user) {
    TypedQuery<RolesAudit> query = em.createNamedQuery("RolesAudit.findByInitiator", RolesAudit.class);
    query.setParameter("initiator", user);

    return query.getResultList();
  }

  public List<RolesAudit> findByTarget(Users user) {
    TypedQuery<RolesAudit> query = em.createNamedQuery("RolesAudit.findByTarget", RolesAudit.class);
    query.setParameter("target", user);

    return query.getResultList();
  }

}
