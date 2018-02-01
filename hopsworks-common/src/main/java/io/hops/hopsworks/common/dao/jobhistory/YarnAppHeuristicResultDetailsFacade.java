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

package io.hops.hopsworks.common.dao.jobhistory;

import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import io.hops.hopsworks.common.dao.AbstractFacade;

@Stateless
public class YarnAppHeuristicResultDetailsFacade extends AbstractFacade<YarnAppHeuristicResultDetails> {

  private static final Logger logger = Logger.getLogger(
          YarnAppHeuristicResultDetailsFacade.class.
          getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public YarnAppHeuristicResultDetailsFacade() {
    super(YarnAppHeuristicResultDetails.class);
  }

  public String searchByIdAndName(int yarnAppHeuristicResultId, String name) {

    try {
      TypedQuery<YarnAppHeuristicResultDetails> q = em.createNamedQuery(
              "YarnAppHeuristicResultDetails.findByIdAndName",
              YarnAppHeuristicResultDetails.class);
      q.setParameter("yarnAppHeuristicResultId", yarnAppHeuristicResultId);
      q.setParameter("name", name);

      YarnAppHeuristicResultDetails result = q.getSingleResult();
      return result.getValue();

    } catch (NoResultException e) {
      return "UNDEFINED";
    }
  }
}
