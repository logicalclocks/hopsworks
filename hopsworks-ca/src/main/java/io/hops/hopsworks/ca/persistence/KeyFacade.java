/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.ca.persistence;

import io.hops.hopsworks.persistence.entity.pki.PKIKey;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class KeyFacade {
  private static final Logger LOGGER = Logger.getLogger(KeyFacade.class.getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;


  public byte[] getEncodedKey(String owner, PKIKey.Type type) {
    try {
      PKIKey key = em.createNamedQuery("PKIKey.findById", PKIKey.class)
          .setParameter("owner", owner)
          .setParameter("type", type)
          .getSingleResult();
      return key.getKey();
    } catch (NoResultException ex) {
      LOGGER.log(Level.INFO, "There is no " + type + " key for " + owner);
      return null;
    }
  }

  public void saveKey(PKIKey key) {
    em.persist(key);
  }
}
