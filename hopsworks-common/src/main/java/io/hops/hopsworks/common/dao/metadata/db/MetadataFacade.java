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

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.metadata.Metadata;
import io.hops.hopsworks.common.dao.metadata.MetadataPK;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class MetadataFacade extends AbstractFacade<Metadata> {

  private static final Logger LOGGER = Logger.getLogger(MetadataFacade.class.
          getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public MetadataFacade() {
    super(Metadata.class);
  }

  public Metadata getMetadata(MetadataPK metadataPK) {

    TypedQuery<Metadata> q = this.em.createNamedQuery(
            "Metadata.findByPrimaryKey",
            Metadata.class);
    q.setParameter("metadataPK", metadataPK);

    try {
      return q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public Metadata getMetadataById(int id) {

    TypedQuery<Metadata> q = this.em.createNamedQuery(
            "Metadata.findById", Metadata.class);
    q.setParameter("id", id);

    try {
      return q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * adds a new record into 'meta_data' table. MetaData is the object that's
   * going to be persisted/updated in the database
   * <p/>
   *
   * @param metadata
   */
  public void addMetadata(Metadata metadata) {

    try {
      Metadata m = this.contains(metadata) ? metadata : this.getMetadata(
              metadata.getMetadataPK());

      if (m != null && m.getMetadataPK().getTupleid() != -1
              && m.getMetadataPK().getFieldid() != -1) {
        /*
         * if the row exists just update it.
         */
        m.copy(metadata);
        this.em.merge(m);
      } else {
        /*
         * if the row is new then just persist it
         */
        m = metadata;
        this.em.persist(m);
      }

      this.em.flush();
      this.em.clear();
    } catch (IllegalStateException | SecurityException ex) {
      LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
      throw ex;
    }
  }

  /**
   * Delete a record from 'meta_data' table.
   * <p/>
   *
   * @param metadata
   */
  public void removeMetadata(Metadata metadata) {
    try {
      Metadata m = this.contains(metadata) ? metadata : this.getMetadata(
              metadata.getMetadataPK());

      if (m != null && m.getMetadataPK().getTupleid() != -1
              && m.getMetadataPK().getFieldid() != -1) {
        /*
         * if the row exists just delete it.
         */
        m.copy(metadata);
        this.em.remove(m);
      }

      this.em.flush();
      this.em.clear();
    } catch (IllegalStateException | SecurityException ex) {
      LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
      throw ex;
    }
  }

  /**
   * Checks if a raw data instance is a managed entity
   * <p/>
   *
   * @param metadata
   * @return
   */
  public boolean contains(Metadata metadata) {
    return this.em.contains(metadata);
  }
}
