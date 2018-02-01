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

package io.hops.hopsworks.common.dao.hdfs.inode;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import io.hops.hopsworks.common.dao.AbstractFacade;

public class InodeEncodingStatusFacade extends AbstractFacade<InodeEncodingStatus> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return this.em;
  }

  public InodeEncodingStatusFacade() {
    super(InodeEncodingStatus.class);
  }

  public InodeEncodingStatus findById(Integer inodeid) {
    TypedQuery<InodeEncodingStatus> q = em.createNamedQuery(
            "InodeEncodingStatus.findById",
            InodeEncodingStatus.class);
    q.setParameter("id", inodeid);

    try {
      return q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
}
