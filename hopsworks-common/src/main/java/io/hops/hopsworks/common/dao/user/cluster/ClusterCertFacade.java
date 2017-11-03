package io.hops.hopsworks.common.dao.user.cluster;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.user.Users;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Stateless
public class ClusterCertFacade extends AbstractFacade<ClusterCert> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public ClusterCertFacade() {
    super(ClusterCert.class);
  }

  public ClusterCert getByOrgUnitNameAndOrgName(String orgName, String orgUnitName) {
    TypedQuery<ClusterCert> query = em.createNamedQuery("ClusterCert.findByOrgUnitNameAndOrgName", ClusterCert.class).
        setParameter("organizationName", orgName).
        setParameter("organizationalUnitName", orgUnitName);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public ClusterCert getBySerialNumber(Integer serialNum) {
    TypedQuery<ClusterCert> query = em.createNamedQuery("ClusterCert.findBySerialNumber", ClusterCert.class).
        setParameter("serialNumber", serialNum);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public List<ClusterCert> getByAgent(Users agent) {
    TypedQuery<ClusterCert> query = em.createNamedQuery("ClusterCert.findByAgent", ClusterCert.class).
        setParameter("agentId", agent);
    return query.getResultList();
  }
}
