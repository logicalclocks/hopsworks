package io.hops.hopsworks.common.dao.project.management;

import io.hops.hopsworks.common.dao.AbstractFacade;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

@Stateless
public class ProjectsManagementFacade extends AbstractFacade<ProjectsManagement> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public ProjectsManagementFacade() {
    super(ProjectsManagement.class);
  }

  @Override
  public List<ProjectsManagement> findAll() {
    TypedQuery<ProjectsManagement> query = em.createNamedQuery(
            "ProjectsManagement.findAll",
            ProjectsManagement.class);
    return query.getResultList();
  }
}
