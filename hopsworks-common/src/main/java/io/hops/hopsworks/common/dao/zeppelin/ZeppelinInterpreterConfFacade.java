package io.hops.hopsworks.common.dao.zeppelin;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.project.Project;
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

  public ZeppelinInterpreterConfs findByProject(Project project) {
    TypedQuery<ZeppelinInterpreterConfs> query = em.createNamedQuery("ZeppelinInterpreterConfs.findByProject",
            ZeppelinInterpreterConfs.class);
    query.setParameter("projectId", project);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public ZeppelinInterpreterConfs create(Project project, String intrepeterConf) {
    if (project == null || intrepeterConf == null) {
      throw new NullPointerException("project and config must be non-null.");
    }
    ZeppelinInterpreterConfs conf = findByProject(project);
    if (conf == null) {
      conf = new ZeppelinInterpreterConfs(project, intrepeterConf);
      em.persist(conf);
    } else {
      conf.setInterpreterConf(intrepeterConf);
      em.merge(conf);
    }
    em.flush();
    return conf;
  }
}
