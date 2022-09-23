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

import io.hops.hopsworks.persistence.entity.pki.CAType;
import io.hops.hopsworks.persistence.entity.pki.PKICrl;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class CRLFacade {

  private static final Logger LOGGER = Logger.getLogger(CRLFacade.class.getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public boolean exist(CAType type) {
    try {
      em.createNamedQuery("PKICrl.forCAType", PKICrl.class)
          .setParameter("type", type)
          .getSingleResult();
      return true;
    } catch (NoResultException ex) {
      return false;
    }
  }

  public Optional<PKICrl> getCRL(CAType type) {
    try {
      return Optional.of(em.createNamedQuery("PKICrl.forCAType", PKICrl.class)
          .setParameter("type", type)
          .getSingleResult());
    } catch (NoResultException ex) {
      LOGGER.log(Level.SEVERE, "Could not find CRL for CA type: " + type);
      return Optional.empty();
    }
  }

  public void init(PKICrl crl) {
    em.persist(crl);
  }

  public void update(PKICrl crl) {
    em.merge(crl);
  }
}
