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
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.metadata.SchemalessMetadata;
import io.hops.hopsworks.common.dao.AbstractFacade;

@Stateless
public class SchemalessMetadataFacade extends AbstractFacade<SchemalessMetadata> {

  private static final Logger logger = Logger.getLogger(
          SchemalessMetadataFacade.class.
          getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public SchemalessMetadataFacade() {
    super(SchemalessMetadata.class);
  }

  public SchemalessMetadata findByInode(Inode inode) {
    TypedQuery<SchemalessMetadata> query = em.createNamedQuery(
            "SchemalessMetadata.findByInode",
            SchemalessMetadata.class);
    query.setParameter("inode", inode);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public void persist(SchemalessMetadata metadata) {
    em.persist(metadata);
  }

  public void merge(SchemalessMetadata metadata) {
    em.merge(metadata);
    em.flush();
  }

  public void remove(SchemalessMetadata metadata) {
    em.remove(metadata);
  }

}
