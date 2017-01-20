package io.hops.hopsworks.common.dao.zeppelin;

import io.hops.hopsworks.common.dao.AbstractFacade;
import java.util.Date;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Stateless
public class ZeppelinInterpreterConfFacade extends AbstractFacade<ZeppelinInterpreterConfs> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public ZeppelinInterpreterConfFacade() {
    super(ZeppelinInterpreterConfs.class);
  }

  public ZeppelinInterpreterConfs findByName(String projectName) {
    TypedQuery<ZeppelinInterpreterConfs> query = em.createNamedQuery(
            "ZeppelinInterpreterConfs.findByProjectName",
            ZeppelinInterpreterConfs.class);
    query.setParameter("projectName", projectName);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public ZeppelinInterpreterConfs create(String projectName,
          String intrepeterConf) {
    if (projectName == null || intrepeterConf == null) {
      throw new NullPointerException(
              "project and config must be non-null.");
    }
    ZeppelinInterpreterConfs conf = new ZeppelinInterpreterConfs(projectName,
            new Date(System.currentTimeMillis()), intrepeterConf);
    em.persist(conf);
    em.flush();
    return conf;
  }
}
