package io.hops.hopsworks.common.dao.workflow;

import io.hops.hopsworks.common.dao.AbstractFacade;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Stateless
public class WorkflowExecutionFacade extends AbstractFacade<WorkflowExecution> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public WorkflowExecutionFacade() {
    super(WorkflowExecution.class);
  }

  public void flush() {
    em.flush();
  }

  public WorkflowExecution find(Integer id, Workflow workflow) {
    TypedQuery<WorkflowExecution> query = em.createNamedQuery(
            "WorkflowExecution.find",
            WorkflowExecution.class).setParameter(
                    "id", id).setParameter("workflowId", workflow.getId());
    return query.getSingleResult();
  }

}
