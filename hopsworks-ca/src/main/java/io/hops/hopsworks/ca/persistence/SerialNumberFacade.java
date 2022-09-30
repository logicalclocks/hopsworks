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
import io.hops.hopsworks.persistence.entity.pki.SerialNumber;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class SerialNumberFacade {
  private static final Logger LOGGER = Logger.getLogger(SerialNumberFacade.class.getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public void initialize(CAType type) {
    LOGGER.log(Level.INFO, "Initializing serial number for CA " + type);
    SerialNumber sn = new SerialNumber(type, 0L);
    em.persist(sn);
    em.flush();
  }

  public boolean isInitialized(CAType type) {
    try {
      em.createNamedQuery("SerialNumber.forCAType", SerialNumber.class)
          .setParameter("type", type)
          .getSingleResult();
      return true;
    } catch (NoResultException ex) {
      return false;
    }
  }

  public Long nextSerialNumber(CAType type) {
    TypedQuery<SerialNumber> query = em.createNamedQuery("SerialNumber.forCAType", SerialNumber.class)
        .setParameter("type", type);
    SerialNumber sn = query.getSingleResult();
    Long current = sn.getNumber();
    sn.setNumber(current + 1);
    LOGGER.log(Level.FINE, "Next serial number for CA " + type + " is " + sn.getNumber());
    em.merge(sn);
    return current;
  }
}
