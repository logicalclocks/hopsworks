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
package io.hops.hopsworks.common.dao.ndb;

import java.io.Serializable;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.List;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class NdbBackupFacade implements Serializable {

  private static final Logger logger = Logger.getLogger(
          NdbBackupFacade.class.getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public NdbBackup find(Integer id) {
    return em.find(NdbBackup.class, id);
  }

  public List<NdbBackup> findAll() {
    TypedQuery<NdbBackup> query = this.em.
            createNamedQuery("NdbBackup.findAll", NdbBackup.class);

    try {
      return query.getResultList();
    } catch (NoResultException e) {
      return null;
    }
  }

  public void persistBackup(NdbBackup backup) {
    em.persist(backup);
  }

  public void removeBackup(int id) {
    NdbBackup backup = find(id);
    if (backup != null) {
      em.remove(backup);
    }
  }

  public NdbBackup findHighestBackupId() {
    TypedQuery<NdbBackup> query = this.em.
            createNamedQuery("NdbBackup.findHighestBackupId", NdbBackup.class);

    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

}
