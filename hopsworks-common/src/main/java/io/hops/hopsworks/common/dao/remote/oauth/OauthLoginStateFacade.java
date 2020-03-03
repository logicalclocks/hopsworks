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
package io.hops.hopsworks.common.dao.remote.oauth;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.remote.oauth.OauthLoginState;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.util.Date;
import java.util.List;


@Stateless
public class OauthLoginStateFacade extends AbstractFacade<OauthLoginState> {
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }
  
  public OauthLoginStateFacade() {
    super(OauthLoginState.class);
  }
  
  public OauthLoginState findByState(String state) {
    try {
      return em.createNamedQuery("OauthLoginState.findByState", OauthLoginState.class).setParameter("state", state)
        .getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  public List<OauthLoginState> findByLoginTimeBefore(Date time) {
    return em.createNamedQuery("OauthLoginState.findByLoginTimeBefore", OauthLoginState.class).setParameter(
      "loginTime", time).getResultList();
  }
  
}
