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

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import io.hops.hopsworks.common.dao.AbstractFacade;

@Stateless
public class YarnApplicationAttemptStateFacade extends AbstractFacade<YarnApplicationattemptstate> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public YarnApplicationAttemptStateFacade() {
    super(YarnApplicationattemptstate.class);
  }

  public String findTrackingUrlByAppId(String applicationid) {
    if (applicationid == null) {
      return "";
    }
    TypedQuery<YarnApplicationattemptstate> query = em.createNamedQuery(
            "YarnApplicationattemptstate.findByApplicationid",
            YarnApplicationattemptstate.class).setParameter(
                    "applicationid", applicationid);
    List<YarnApplicationattemptstate> appAttempts = query.getResultList();
    if (appAttempts != null) {
      Integer highestAttemptId = 0;
      String trackingUrl = "";
      for (YarnApplicationattemptstate a : appAttempts) {
        try {
          String attemptId = a.getYarnApplicationattemptstatePK().
                  getApplicationattemptid();
          // attemptIds look like 'application12133_1000032423423_0001'
          // Only the last chars after '_' contain the actual attempt ID.
          attemptId = attemptId.substring(attemptId.lastIndexOf("_") + 1,
                  attemptId.length());
          Integer attempt = Integer.parseInt(attemptId);
          if (attempt > highestAttemptId) {
            highestAttemptId = attempt;
            trackingUrl = a.getApplicationattempttrakingurl();
          }

        } catch (NumberFormatException e) {
          return "";
        }
      }
      return trackingUrl;
    }
    return "";
  }

}
