/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
 */

package io.hops.hopsworks.common.dao.rstudio;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import org.apache.commons.codec.digest.DigestUtils;

/**
 */
@Stateless
public class RStudioSettingsFacade {

  private static final Logger logger = Logger.getLogger(RStudioSettingsFacade.class.
                  getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public RStudioSettingsFacade() {
  }

  protected EntityManager getEntityManager() {
    return em;
  }

  public List<RStudioSettings> findRStudioSettingsByProject(Integer projectId) {
    TypedQuery<RStudioSettings> query = em.createNamedQuery(
            "RStudioSettings.findByProjectId",
            RStudioSettings.class);
    query.setParameter("projectId", projectId);
    return query.getResultList();
  }

  public RStudioSettings findByProjectUser(int projectId, String email) {

    RStudioSettingsPK pk = new RStudioSettingsPK(projectId, email);
    RStudioSettings js = null;
    js = em.find(RStudioSettings.class, pk);
    if (js == null) {
      String secret = DigestUtils.sha256Hex(Integer.toString(
              ThreadLocalRandom.current().nextInt()));
      js = new RStudioSettings(pk);
      js.setSecret(secret);
      js.setMode("sparkDynamic");
      persist(js);
    }
    return js;

  }

  private void persist(RStudioSettings js) {
    if (js != null) {
      em.persist(js);
    }
  }

  public void update(RStudioSettings js) {
    if (js != null) {
      em.merge(js);
    }
  }

  private void remove(RStudioSettings js) {
    if (js != null) {
      em.remove(js);
    }
  }

}
