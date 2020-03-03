/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.jwt.dao;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Stateless
public class InvalidJwtFacade {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public InvalidJwt find(String id) {
    return em.find(InvalidJwt.class, id);
  }

  public List<InvalidJwt> findAll() {
    TypedQuery<InvalidJwt> query = em.createNamedQuery("InvalidJwt.findAll", InvalidJwt.class);
    return query.getResultList();
  }

  public List<String> findAllJti() {
    TypedQuery<String> query = em.createNamedQuery("InvalidJwt.findAllJti", String.class);
    return query.getResultList();
  }

  public List<InvalidJwt> findExpired() {
    TypedQuery<InvalidJwt> query = em.createNamedQuery("InvalidJwt.findExpired", InvalidJwt.class);
    return query.getResultList();
  }

  public void persist(InvalidJwt invalidJwt) {
    em.persist(invalidJwt);
  }

  public InvalidJwt merge(InvalidJwt invalidJwt) {
    return em.merge(invalidJwt);
  }

  public void remove(InvalidJwt invalidJwt) {
    InvalidJwt ij = find(invalidJwt.getJti());
    if (ij == null) {
      return;
    }
    em.remove(ij);
  }
}
