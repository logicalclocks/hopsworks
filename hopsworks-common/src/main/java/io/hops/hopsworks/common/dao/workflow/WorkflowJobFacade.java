package io.hops.hopsworks.common.dao.workflow;

import io.hops.hopsworks.common.dao.AbstractFacade;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class WorkflowJobFacade extends AbstractFacade<WorkflowJob> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public WorkflowJobFacade() {
    super(WorkflowJob.class);
  }
}
