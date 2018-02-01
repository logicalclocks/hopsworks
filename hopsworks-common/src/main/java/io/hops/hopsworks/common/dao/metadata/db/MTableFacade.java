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

import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.metadata.MTable;
import io.hops.hopsworks.common.metadata.exception.DatabaseException;

@Stateless
public class MTableFacade extends AbstractFacade<MTable> {

  private static final Logger logger = Logger.getLogger(MTableFacade.class.
          getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  public EntityManager getEntityManager() {
    return em;
  }

  public MTableFacade() {
    super(MTable.class);
  }

  public MTable getTable(int tableid) throws DatabaseException {
    return this.em.find(MTable.class, tableid);
  }

  /**
   * adds a new record into 'tables' table. Represents a new metadata template
   * <p/>
   *
   * @param table
   * @return the id of the newly inserted table or -1 in case of error
   * @throws se.kth.hopsworks.meta.exception.DatabaseException
   */
  public int addTable(MTable table) throws DatabaseException {

    try {

      MTable t = this.getTable(table.getId());

      if (t != null && t.getId() != -1) {
        /*
         * if the table exists just update it, along with its corresponding
         * child fields.
         * Merge and cascade type ALL takes care of it, but first we need to
         * copy the incoming table object into the managed object t
         */
        t.copy(table);
        this.em.merge(t);
      } else {
        /*
         * if the table is new then jpa cannot cascade insert to the child
         * fields.
         * we need to remove the fields in order for the table to be inserted
         * first and acquire an id
         */
        t = table;
        t.resetFields();
        this.em.persist(t);
      }

      this.em.flush();
      this.em.clear();
      return t.getId();
    } catch (IllegalStateException | SecurityException e) {

      throw new DatabaseException("Could not add table " + table, e);
    }
  }

  /**
   * Deletes a tables entity from meta_tables table. If the object is an
   * unmanaged entity it has to be merged to become managed so that delete
   * can cascade down its associations if necessary
   * <p/>
   *
   * @param table The table object that's going to be removed
   * @throws se.kth.hopsworks.meta.exception.DatabaseException
   */
  public void deleteTable(MTable table) throws DatabaseException {

    try {
      MTable t = this.contains(table) ? table : this.getTable(table.getId());

      //remove the table
      if (this.em.contains(t)) {
        this.em.remove(t);
      } else {
        //if the object is unmanaged it has to be managed before it is removed
        this.em.remove(this.em.merge(t));
      }

    } catch (SecurityException | IllegalStateException ex) {
      throw new DatabaseException("Could not delete table " + table, ex);
    }
  }

  /**
   * Checks if a table instance is a managed entity
   * <p/>
   * @param table
   * @return
   */
  public boolean contains(MTable table) {
    return this.em.contains(table);
  }
}
