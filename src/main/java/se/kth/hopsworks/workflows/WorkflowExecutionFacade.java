package se.kth.hopsworks.workflows;

import se.kth.bbc.project.Project;
import se.kth.kthfsdashboard.user.AbstractFacade;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;
import java.util.List;

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
        TypedQuery<WorkflowExecution> query = em.createNamedQuery("WorkflowExecution.find",
                WorkflowExecution.class).setParameter(
                "id", id).setParameter("workflowId", workflow.getId());
        return query.getSingleResult();
    }

}
