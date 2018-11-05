/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 */

package io.hops.hopsworks.common.dao.metadata.db;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.metadata.Field;

@Stateless
public class FieldFacade extends AbstractFacade<Field> {

  private static final Logger LOGGER = Logger.getLogger(FieldFacade.class.
          getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public FieldFacade() {
    super(Field.class);
  }

  public Field getField(int fieldid) {

    return this.em.find(Field.class, fieldid);
  }

  /**
   * adds a new record into 'fields' table. Each record represents a table
   * column with its attributes (searchable, required, maxsize) and it is
   * associated with the relevant table.
   *
   * @param field the field with its corresponding attributes
   * @return
   */
  public int addField(Field field) {

    try {
      Field f = this.getField(field.getId());

      if (f != null && f.getId() != -1) {
        f.copy(field);
        this.em.merge(f);
      } else {
        f = field;
        /*
         * The field is a new one so it has id -1; all the child entities
         * (predefined values) are pointing to their parent (field) with id -1.
         * Database throws exception because there is no field with id -1.
         * So remove all the child entities from the field before persisting it
         * and handle the predefined values later
         */
        f.resetFieldPredefinedValues();
        this.em.persist(f);
      }

      this.em.flush();
      this.em.clear();
      return f.getId();
    } catch (IllegalStateException | SecurityException e) {
      LOGGER.log(Level.SEVERE, "Could not add field " + field, e);
      throw e;
    }
  }

  /**
   * Deletes a fields entity. If the object is an
   * unmanaged entity it has to be merged to become managed so that the delete
   * can cascade down its associations if necessary
   * <p/>
   *
   * @param field the field object that's going to be re
   */
  public void deleteField(Field field) {

    Field f = this.contains(field) ? field : this.getField(field.getId());

    if (this.em.contains(f)) {
      this.em.remove(f);
    } else {
      //if the object is unmanaged it has to be managed before it is removed
      this.em.remove(this.em.merge(f));
    }
    //persist the changes to the database immediately
    this.em.flush();
  }

  /**
   * Checks if a field instance is a managed entity
   * <p/>
   * @param field
   * @return
   */
  public boolean contains(Field field) {
    return this.em.contains(field);
  }
}
