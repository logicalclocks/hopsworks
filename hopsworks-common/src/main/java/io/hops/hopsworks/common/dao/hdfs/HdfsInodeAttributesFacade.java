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

package io.hops.hopsworks.common.dao.hdfs;

import io.hops.hopsworks.common.dao.AbstractFacade;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class HdfsInodeAttributesFacade extends AbstractFacade<HdfsInodeAttributes> {

  private final static Logger logger = Logger.getLogger(HdfsInodeAttributes.class.getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public HdfsInodeAttributesFacade() { super(HdfsInodeAttributes.class); }

  public HdfsInodeAttributes getInodeAttributes(int inodeId) {
    TypedQuery<HdfsInodeAttributes> query = em.createNamedQuery(
        "HdfsInodeAttributes.findByInodeId", HdfsInodeAttributes.class)
        .setParameter("inodeId", inodeId);
    try {
      return query.getSingleResult();
    } catch (NoResultException ex) {
      logger.log(Level.SEVERE, "Cannot find Inode attributes for inode: " + inodeId);
    }

    return null;
  }

}
