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
package io.hops.hopsworks.common.dao.jobhistory;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import io.hops.hopsworks.common.dao.AbstractFacade;

@Stateless
public class YarnApplicationstateFacade extends AbstractFacade<YarnApplicationstate> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public YarnApplicationstateFacade() {
    super(YarnApplicationstate.class);
  }

  @Override
  public List<YarnApplicationstate> findAll() {
    TypedQuery<YarnApplicationstate> query = em.createNamedQuery(
            "YarnApplicationstate.findAll",
            YarnApplicationstate.class);
    return query.getResultList();
  }

  public List<YarnApplicationstate> findByAppname(String appname) {
    TypedQuery<YarnApplicationstate> query = em.createNamedQuery(
            "YarnApplicationstate.findByAppname",
            YarnApplicationstate.class).setParameter(
                    "appname", appname);
    return query.getResultList();
  }

  public List<YarnApplicationstate> findByAppuserAndAppState(String appUser,
          String appState) {
    TypedQuery<YarnApplicationstate> query = em.createNamedQuery(
            "YarnApplicationstate.findByAppuserAndAppsmstate",
            YarnApplicationstate.class).setParameter("appuser", appUser).
            setParameter("appsmstate", appState);
    return query.getResultList();
  }

  public YarnApplicationstate findByAppId(String appId) {
    try {
      return em.createNamedQuery("YarnApplicationstate.findByApplicationid",
              YarnApplicationstate.class).setParameter(
                      "applicationid", appId).getSingleResult();
    } catch (NoResultException e) {
      return null;
    }

  }
}
