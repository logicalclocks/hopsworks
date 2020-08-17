/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.common.dao.remote.group;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.remote.group.RemoteGroupProjectMapping;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.util.List;

@Stateless
public class RemoteGroupProjectMappingFacade extends AbstractFacade<RemoteGroupProjectMapping> {
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }
  
  public RemoteGroupProjectMappingFacade() {
    super(RemoteGroupProjectMapping.class);
  }
  
  public RemoteGroupProjectMapping findByGroupAndProject(String group, Project project) {
    try {
      return em.createNamedQuery("RemoteGroupProjectMapping.findByGroupAndProject", RemoteGroupProjectMapping.class)
        .setParameter("remoteGroup", group).setParameter("project", project).getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  public List<RemoteGroupProjectMapping> findByGroup(String group) {
    return em.createNamedQuery("RemoteGroupProjectMapping.findByGroup", RemoteGroupProjectMapping.class)
      .setParameter("remoteGroup", group).getResultList();
  }
  
}
