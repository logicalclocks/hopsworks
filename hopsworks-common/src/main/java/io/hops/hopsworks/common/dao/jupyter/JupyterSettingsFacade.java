package io.hops.hopsworks.common.dao.jupyter;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import org.apache.commons.codec.digest.DigestUtils;

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
      // Set default framework to 'sparkDynamic'
      js = new JupyterSettings(pk, secret, "sparkDynamic");
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
