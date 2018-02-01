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

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.metadata.Metadata;
import io.hops.hopsworks.common.dao.metadata.MetadataPK;
import io.hops.hopsworks.common.metadata.exception.DatabaseException;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.logging.Logger;

@Stateless
public class MetadataFacade extends AbstractFacade<Metadata> {

  private static final Logger logger = Logger.getLogger(MetadataFacade.class.
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

  public Metadata getMetadata(MetadataPK metadataPK) throws DatabaseException {

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
   * @throws se.kth.hopsworks.meta.exception.DatabaseException
   */
  public void addMetadata(Metadata metadata) throws DatabaseException {

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
    } catch (IllegalStateException | SecurityException e) {

      throw new DatabaseException(e.getMessage(), e);
    }
  }

  /**
   * Delete a record from 'meta_data' table.
   * <p/>
   *
   * @param metadata
   * @throws se.kth.hopsworks.meta.exception.DatabaseException
   */
  public void removeMetadata(Metadata metadata) throws DatabaseException {
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
    } catch (IllegalStateException | SecurityException e) {

      throw new DatabaseException(e.getMessage(), e);
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
