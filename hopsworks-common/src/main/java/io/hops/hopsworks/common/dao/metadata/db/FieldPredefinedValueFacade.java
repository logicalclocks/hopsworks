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
import io.hops.hopsworks.common.dao.metadata.FieldPredefinedValue;
import io.hops.hopsworks.common.metadata.exception.DatabaseException;

@Stateless
public class FieldPredefinedValueFacade extends AbstractFacade<FieldPredefinedValue> {

  private static final Logger logger = Logger.getLogger(
          FieldPredefinedValueFacade.class.getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public FieldPredefinedValueFacade() {
    super(FieldPredefinedValue.class);
  }

  public void addFieldPredefinedValue(FieldPredefinedValue value) throws
          DatabaseException {
    try {
      this.em.persist(value);
    } catch (IllegalStateException | SecurityException e) {

      throw new DatabaseException("Could not add predefined value " + value, e);
    }
  }

  /**
   * Deletes a field's predefined values. When a field modification happens
   * all its previously defined values need to be purged before the new
   * ones take their place i.e. a field gets its type changed from a dropdown
   * list to true/false, or to plain text
   * <p/>
   *
   * @param fieldid
   * @throws se.kth.hopsworks.meta.exception.DatabaseException when an error
   * happens
   */
  public void deleteFieldPredefinedValues(int fieldid) throws DatabaseException {

    Query query = this.em.
            createNamedQuery("FieldPredefinedValue.findByFieldid");
    query.setParameter("fieldid", fieldid);
    List<FieldPredefinedValue> valueList = query.getResultList();

    for (FieldPredefinedValue value : valueList) {
      if (this.em.contains(value)) {
        this.em.remove(value);
      } else {
        //if the object is unmanaged it has to be managed before it is removed
        this.em.remove(this.em.merge(value));
      }
    }
  }

}
