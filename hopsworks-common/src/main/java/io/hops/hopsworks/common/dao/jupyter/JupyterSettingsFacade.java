/*
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
 *
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
