/*
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
 * The jupyter_settings table stores the most recent configuration settings from the user's Hopsworks UI.
 * That is, what version did the user pick (tensorflow, dynamic/static spark, horovod, TfOnSpark, etc)
 * How many executors (CPU/Memory/GPus), etc...
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

  public List<RStudioSettings> findJupyterSettingsByProject(Integer projectId) {
    TypedQuery<RStudioSettings> query = em.createNamedQuery(
            "JupyterSettings.findByProjectId",
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
