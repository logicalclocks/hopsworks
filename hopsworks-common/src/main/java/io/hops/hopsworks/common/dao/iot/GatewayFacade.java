package io.hops.hopsworks.common.dao.iot;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.jobs.description.JobFacade;
import io.hops.hopsworks.common.dao.project.Project;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class GatewayFacade extends AbstractFacade<IoTGateways> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  private static final Logger LOGGER = Logger.getLogger(JobFacade.class.
    getName());

  public GatewayFacade() {
    super(IoTGateways.class);
  }

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public List<IoTGateways> findByProject(Project project) {
    TypedQuery<IoTGateways> query = em.createNamedQuery("IoTGateways.findByProject", IoTGateways.class);
    query.setParameter("project", project);
    try {
      return query.getResultList();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  public IoTGateways findByProjectAndId(Project project, int gatewayId) {
    TypedQuery<IoTGateways> query = em.createNamedQuery("IoTGateways.findByProjectAndId", IoTGateways.class);
    query.setParameter("project", project)
      .setParameter("id", gatewayId);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  public boolean updateState(int gatewayId, GatewayState newState) {
    boolean status = false;
    try {
      TypedQuery<IoTGateways> query = em.createNamedQuery("IoTGateways.updateState", IoTGateways.class)
        .setParameter("id", gatewayId)
        .setParameter("state", newState);
      int result = query.executeUpdate();
      LOGGER.log(Level.INFO, "Updated entity count = {0}", result);
      if (result == 1) {
        status = true;
      }
    } catch (SecurityException | IllegalArgumentException ex) {
      LOGGER.log(Level.SEVERE, "Could not update gateway with id:" + gatewayId);
      throw ex;
    }
    return status;
  }
}
