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
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.metadata.TupleToFile;
import io.hops.hopsworks.common.metadata.exception.DatabaseException;

@Stateless
public class TupleToFileFacade extends AbstractFacade<TupleToFile> {

  private static final Logger logger = Logger.getLogger(TupleToFileFacade.class.
          getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public TupleToFileFacade() {
    super(TupleToFile.class);
  }

  public TupleToFile getTupletofile(int tupleid) throws DatabaseException {

    return this.em.find(TupleToFile.class, tupleid);
  }

  public List<TupleToFile> getTuplesByInodeId(Integer parentId, String inodeName)
          throws DatabaseException {

    String queryString = "TupleToFile.findByInodeid";

    Query query = this.em.createNamedQuery(queryString);
    query.setParameter("parentid", parentId);
    query.setParameter("name", inodeName);
    return query.getResultList();
  }

  public int addTupleToFile(TupleToFile ttf) throws DatabaseException {

    if (ttf != null && ttf.getId() != -1) {
      this.em.merge(ttf);
    } else {
      this.em.persist(ttf);
    }

    this.em.flush();

    return ttf.getId();
  }

  /**
   * Deletes a tupleToFile entity. If the object is an
   * unmanaged entity it has to be merged to become managed so that delete
   * can cascade down its associations if necessary
   * <p/>
   *
   * @param ttf
   * @throws se.kth.hopsworks.meta.exception.DatabaseException
   */
  public void deleteTTF(TupleToFile ttf) throws DatabaseException {

    TupleToFile tf = this.contains(ttf) ? ttf : this.getTupletofile(ttf.getId());

    if (this.em.contains(tf)) {
      this.em.remove(tf);
    } else {
      //if the object is unmanaged it has to be managed before it is removed
      this.em.remove(this.em.merge(tf));
    }
  }

  /**
   * Checks if a tupleToFile instance is a managed entity
   * <p/>
   * @param ttf
   * @return
   */
  public boolean contains(TupleToFile ttf) {
    return this.em.contains(ttf);
  }
}
