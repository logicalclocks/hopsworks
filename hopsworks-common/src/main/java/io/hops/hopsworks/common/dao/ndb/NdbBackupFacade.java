/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
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
