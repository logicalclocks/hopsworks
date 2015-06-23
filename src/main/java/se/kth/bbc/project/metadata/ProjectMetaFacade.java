package se.kth.bbc.project.metadata;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.bbc.project.Project;
import se.kth.kthfsdashboard.user.AbstractFacade;

/**
 *
 * @author stig
 */
@Stateless
public class ProjectMetaFacade extends AbstractFacade<ProjectMeta> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  public ProjectMetaFacade() {
    super(ProjectMeta.class);
  }

  /**
   * Find the ProjectMeta object associated with the given Project.
   * <p>
   * @param project
   * @return The associated ProjectMeta object, or null if there is none.
   */
  public ProjectMeta findByProject(Project project) {
    TypedQuery<ProjectMeta> q = em.createNamedQuery("ProjectMeta.findByProject",
            ProjectMeta.class);
    q.setParameter("project", project);
    try {
      return q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /*
   * TODO: this must be the most idiotic update function ever written... But:
   * Writing
   * em.merge(meta);
   * for some reason fails. The resulting queries insert project_id null in the
   * PROJECT_DESIGN table.
   * I have not yet figured out why...
   */
  public void update(ProjectMeta meta) {
    ProjectMeta old = findByProject(meta.getProject());
    if (old != null) {
      em.remove(old);
      em.flush();
    }
    em.persist(meta);
  }

}
