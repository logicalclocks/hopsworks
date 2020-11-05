/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.featurestore;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.integrations.DatabricksInstance;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

@Stateless
public class DatabricksInstanceFacade extends AbstractFacade<DatabricksInstance> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public DatabricksInstanceFacade() {
    super(DatabricksInstance.class);
  }

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public List<DatabricksInstance> getInstances(Users user) {
    return em.createNamedQuery("DatabricksInstance.findAllByUser", DatabricksInstance.class)
        .setParameter("user", user)
        .getResultList();
  }

  public Optional<DatabricksInstance> getInstance(Users user, String instanceUrl) {
    try {
      return Optional.of(em.createNamedQuery("DatabricksInstance.findAllByUserAndUrl", DatabricksInstance.class)
          .setParameter("user", user)
          .setParameter("url", instanceUrl)
          .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public void add(DatabricksInstance dbInstance) {
    em.merge(dbInstance);
  }

  public void delete(DatabricksInstance dbInstance) {
    em.remove(dbInstance);
  }
}
