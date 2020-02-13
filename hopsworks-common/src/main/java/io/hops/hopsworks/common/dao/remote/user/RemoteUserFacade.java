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
package io.hops.hopsworks.common.dao.remote.user;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.remote.user.RemoteUser;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

@Stateless
public class RemoteUserFacade extends AbstractFacade<RemoteUser> {
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public RemoteUserFacade() {
    super(RemoteUser.class);
  }

  public RemoteUser findByUUID(String uuid) {
    try {
      return em.createNamedQuery("RemoteUser.findByUuid", RemoteUser.class).setParameter("uuid", uuid)
          .getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public RemoteUser findByUsers(Users user) {
    try {
      return em.createNamedQuery("RemoteUser.findByUid", RemoteUser.class).setParameter("uid", user).getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

}
