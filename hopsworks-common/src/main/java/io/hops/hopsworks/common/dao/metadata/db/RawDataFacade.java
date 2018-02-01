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

package io.hops.hopsworks.common.dao.metadata.db;

import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.metadata.RawData;
import io.hops.hopsworks.common.dao.metadata.RawDataPK;
import io.hops.hopsworks.common.metadata.exception.DatabaseException;

@Stateless
public class RawDataFacade extends AbstractFacade<RawData> {

  private static final Logger logger = Logger.getLogger(RawDataFacade.class.
          getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public RawDataFacade() {
    super(RawData.class);
  }

  public RawData getRawData(RawDataPK rawdataPK) throws DatabaseException {

    TypedQuery<RawData> q = this.em.createNamedQuery("RawData.findByPrimaryKey",
            RawData.class);
    q.setParameter("rawdataPK", rawdataPK);

    try {
      return q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * adds a new record into 'raw_data' table. RawData is the object that's
   * going to be persisted/updated in the database
   * <p/>
   * @param raw
   * @throws se.kth.hopsworks.meta.exception.DatabaseException
   */
  public void addRawData(RawData raw) throws DatabaseException {

    try {
      RawData r = this.contains(raw) ? raw : this.getRawData(raw.getRawdataPK());

      if (r != null && r.getRawdataPK().getTupleid() != -1 && r.getRawdataPK().
              getFieldid() != -1) {
        /*
         * if the row exists just update it.
         */
        r.copy(raw);
        this.em.merge(r);
      } else {
        /*
         * if the row is new then just persist it
         */
        r = raw;
        this.em.persist(r);
      }

      this.em.flush();
      this.em.clear();
    } catch (IllegalStateException | SecurityException e) {

      throw new DatabaseException("Could not add raw data " + raw, e);
    }
  }

  public int getLastInsertedTupleId() throws DatabaseException {

    String queryString = "RawData.lastInsertedTupleId";

    Query query = this.em.createNamedQuery(queryString);
    List<RawData> list = query.getResultList();

    return (!list.isEmpty()) ? list.get(0).getId() : 0;
  }

  /**
   * Checks if a raw data instance is a managed entity
   * <p/>
   * @param rawdata
   * @return
   */
  public boolean contains(RawData rawdata) {
    return this.em.contains(rawdata);
  }
}
