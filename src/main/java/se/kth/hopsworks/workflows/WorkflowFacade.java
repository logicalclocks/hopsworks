package se.kth.hopsworks.workflows;

import se.kth.bbc.project.Project;
import se.kth.kthfsdashboard.user.AbstractFacade;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

@Stateless
public class WorkflowFacade extends AbstractFacade<Workflow> {

    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public WorkflowFacade() {
        super(Workflow.class);
    }

    @Override
    public List<Workflow> findAll() {
        TypedQuery<Workflow> query = em.createNamedQuery("Workflow.findAll",
                Workflow.class);
        return query.getResultList();
    }

    public Workflow find(Integer id, Project project) {
        TypedQuery<Workflow> query = em.createNamedQuery("Workflow.find",
                Workflow.class).setParameter(
                "id", id).setParameter("projectId", project.getId());
        return query.getSingleResult();
    }

    public Workflow findById(Integer id) {
        return em.find(Workflow.class, id);
    }

    public List<Workflow> findByName() {
        TypedQuery<Workflow> query = em.createNamedQuery("Workflow.findByName",
                Workflow.class);
        return query.getResultList();
    }

    public void persist(Workflow workflow) {
        em.persist(workflow);

    }

    public Workflow merge(Workflow workflow) {
        return em.merge(workflow);

    }

    public void remove(Workflow workflow) {
        em.remove(em.merge(workflow));
    }

    public void flush() {
        em.flush();
    }

    public Workflow refresh(Workflow workflow) {
        Workflow w = findById(workflow.getId());
        em.refresh(w);
        return w;
    }
}
