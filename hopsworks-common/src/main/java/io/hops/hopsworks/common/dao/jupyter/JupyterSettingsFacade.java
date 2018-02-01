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

package io.hops.hopsworks.common.dao.jupyter;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * The jupyter_settings table stores the most recent configuration settings from the user's Hopsworks UI.
 * That is, what version did the user pick (tensorflow, dynamic/static spark, horovod, TfOnSpark, etc)
 * How many executors (CPU/Memory/GPus), etc...
 */
@Stateless
public class JupyterSettingsFacade {

  private static final Logger logger = Logger.getLogger(
          JupyterSettingsFacade.class.
                  getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public JupyterSettingsFacade() {
  }

  protected EntityManager getEntityManager() {
    return em;
  }

  public List<JupyterSettings> findJupyterSettingsByProject(Integer projectId) {
    TypedQuery<JupyterSettings> query = em.createNamedQuery(
            "JupyterSettings.findByProjectId",
            JupyterSettings.class);
    query.setParameter("projectId", projectId);
    return query.getResultList();
  }

  public JupyterSettings findByProjectUser(int projectId, String email) {

    JupyterSettingsPK pk = new JupyterSettingsPK(projectId, email);
    JupyterSettings js = null;
    js = em.find(JupyterSettings.class, pk);
    if (js == null) {
      String secret = DigestUtils.sha256Hex(Integer.toString(
              ThreadLocalRandom.current().nextInt()));
      js = new JupyterSettings(pk);
      js.setSecret(secret);
      js.setMode("sparkDynamic");
      persist(js);
    }
    return js;

  }

  private void persist(JupyterSettings js) {
    if (js != null) {
      em.persist(js);
    }
  }

  public void update(JupyterSettings js) {
    if (js != null) {
      em.merge(js);
    }
  }

  private void remove(JupyterSettings js) {
    if (js != null) {
      em.remove(js);
    }
  }

}
