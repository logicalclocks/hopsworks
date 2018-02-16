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
