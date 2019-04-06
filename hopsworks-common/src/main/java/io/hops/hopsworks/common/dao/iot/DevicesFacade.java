package io.hops.hopsworks.common.dao.iot;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.jobs.description.JobFacade;
import io.hops.hopsworks.common.dao.project.Project;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.logging.Logger;

@Stateless
public class DevicesFacade extends AbstractFacade<IoTGateways> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  private static final Logger LOGGER = Logger.getLogger(JobFacade.class.
    getName());

  public DevicesFacade() {
    super(IoTGateways.class);
  }

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

//  public CollectionInfo findByProject(Project project) {
//    String queryStr = buildQuery("SELECT i FROM IoTGateways i ", null, null, /*"i.project_id = :project "*/"");
//    String queryCountStr =
//      buildQuery("SELECT COUNT(DISTINCT i.name) FROM IoTGateways i ", null, null, /*"i.project_id = :project "*/"");
//    Query query = em.createQuery(queryStr, IoTGateways.class);//.setParameter("project", project);
//    Query queryCount = em.createQuery(queryCountStr, IoTGateways.class);//.setParameter("project", project);
//    return new CollectionInfo((Long) queryCount.getSingleResult(), query.getResultList());
//  }

  public CollectionInfo findByProject(Project project) {
    String queryStr = buildQuery("SELECT i FROM IoTGateways i ", null, null,"");
    String queryCountStr =
      buildQuery("SELECT COUNT(DISTINCT i.name) FROM IoTGateways i ", null, null,"");
    Query query = em.createQuery(queryStr, IoTGateways.class);
    Query queryCount = em.createQuery(queryCountStr, IoTGateways.class);
    return new CollectionInfo((Long) queryCount.getSingleResult(), query.getResultList());
  }

}
