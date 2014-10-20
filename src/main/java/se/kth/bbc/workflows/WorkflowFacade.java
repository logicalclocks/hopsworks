package se.kth.bbc.workflows;

import java.util.Date;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.kthfsdashboard.user.AbstractFacade;

/**
 *
 * @author stig
 */
@Stateless(name = "workflowFacade")
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

    /**
     * Get all workflows for study <i>studyname</i>.
     */
    public List<Workflow> findAllForStudy(String studyname) {
        TypedQuery<Workflow> q = em.createNamedQuery("Workflow.findByStudy", Workflow.class);
        q.setParameter("study", studyname);
        return q.getResultList();
    }

    /**
     * Get all workflows for study <i>studyname</i> or public ones.
     */
    public List<Workflow> findAllForStudyOrPublic(String studyname) {
        TypedQuery<Workflow> q = em.createNamedQuery("Workflow.findByStudyOrPublic", Workflow.class);
        q.setParameter("study", studyname);
        return q.getResultList();
    }
    
    @Override
    public void create(Workflow entity) {
        if (entity.getCreationDate() == null) {
            entity.setCreationDate(new Date());
        }
        super.create(entity);
    }
    
}
