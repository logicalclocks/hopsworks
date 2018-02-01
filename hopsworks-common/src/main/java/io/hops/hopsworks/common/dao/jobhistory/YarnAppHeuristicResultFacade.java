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
public class YarnAppHeuristicResultFacade extends AbstractFacade<YarnAppHeuristicResult> {

  private static final Logger logger = Logger.getLogger(
          YarnAppHeuristicResultFacade.class.
          getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public YarnAppHeuristicResultFacade() {
    super(YarnAppHeuristicResult.class);
  }

  public Integer searchByIdAndClass(String yarnAppResultId,
          String heuristicClass) {
    TypedQuery<YarnAppHeuristicResult> q = em.createNamedQuery(
            "YarnAppHeuristicResult.findByIdAndHeuristicClass",
            YarnAppHeuristicResult.class);
    q.setParameter("yarnAppResultId", yarnAppResultId);
    q.setParameter("heuristicClass", heuristicClass);

    YarnAppHeuristicResult result = q.getSingleResult();

    return result.getId();

  }

  public String searchForSeverity(String yarnAppResultId, String heuristicClass) {
    try {
      TypedQuery<YarnAppHeuristicResult> q = em.createNamedQuery(
              "YarnAppHeuristicResult.findByIdAndHeuristicClass",
              YarnAppHeuristicResult.class);
      q.setParameter("yarnAppResultId", yarnAppResultId);
      q.setParameter("heuristicClass", heuristicClass);

      YarnAppHeuristicResult result = q.getSingleResult();

      short severity = result.getSeverity();
      switch (severity) {
        case 0:
          return "NONE";
        case 1:
          return "LOW";
        case 2:
          return "MODERATE";
        case 3:
          return "SEVERE";
        case 4:
          return "CRITICAL";
        default:
          return "NONE";
      }
    } catch (NoResultException e) {
      return "UNDEFINED";
    }
  }

}
