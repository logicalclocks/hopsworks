package io.hops.hopsworks.common.dao.iot;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.project.Project;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class IotGatewayFacade extends AbstractFacade<IotGateways> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  private static final Logger LOGGER = Logger.getLogger(IotGatewayFacade.class.
    getName());

  public IotGatewayFacade() {
    super(IotGateways.class);
  }

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public List<IotGateways> findByProject(Project project) {
    TypedQuery<IotGateways> query = em.createNamedQuery("IotGateways.findByProject", IotGateways.class);
    query.setParameter("project", project);
    try {
      return query.getResultList();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  public IotGateways findByProjectAndDomainAndPort(Project project, String domain, Integer port) {
    TypedQuery<IotGateways> query = em.createNamedQuery("IotGateways.findByProjectAndDomainAndPort", IotGateways.class);
    query.setParameter("project", project)
      .setParameter("domain", domain)
      .setParameter("port", port);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  public IotGateways findByProjectAndName(Project project, String name) {
    TypedQuery<IotGateways> query = em.createNamedQuery("IotGateways.findByProjectAndName", IotGateways.class);
    query.setParameter("project", project)
      .setParameter("name", name);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  public boolean updateState(String domain, Integer port, IotGatewayState newState) {
    boolean status = false;
    try {
      TypedQuery<IotGateways> query = em.createNamedQuery("IotGateways.updateState", IotGateways.class)
        .setParameter("domain", domain)
        .setParameter("port", port)
        .setParameter("state", newState);
      int result = query.executeUpdate();
      LOGGER.log(Level.INFO, "Updated entity count = {0}", result);
      if (result == 1) {
        status = true;
      }
    } catch (SecurityException | IllegalArgumentException ex) {
      LOGGER.log(Level.SEVERE, "Could not update gateway " + domain + ":" + port);
      throw ex;
    }
    return status;
  }
  
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public IotGateways putIotGateway(IotGateways gateway) {
    gateway = em.merge(gateway);
    em.flush();
    return gateway;
  }
  
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void removeIotGateway(IotGateways gateway) {
    try {
      IotGateways managedGateway = em.find(IotGateways.class, gateway);
      em.remove(em.merge(managedGateway));
      em.flush();
    } catch (SecurityException | IllegalStateException ex) {
      LOGGER.log(Level.SEVERE, "Could not delete gateway: " + gateway.getName());
      throw ex;
    }
  }
}
