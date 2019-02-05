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
 * That is, what version did the user pick (tensorflow, dynamic/static spark, etc)
 * How many executors (CPU/Memory/GPUs), etc...
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
