package se.kth.bbc.project.services;

import java.util.ArrayList;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import se.kth.bbc.project.Project;
import se.kth.kthfsdashboard.user.AbstractFacade;

/**
 *
 * @author stig
 */
@Stateless
public class ProjectServiceFacade extends AbstractFacade<ProjectServices> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public ProjectServiceFacade() {
    super(ProjectServices.class);
  }

  public List<ProjectServiceEnum> findEnabledServicesForProject(Project project) {
    //TODO: why does this return String?
    Query q = em.createNamedQuery("ProjectServices.findServicesByProject",
            ProjectServiceEnum.class);
    q.setParameter("project", project);
    return q.getResultList();
  }

  //TODO: write tests for this
  public void persistServicesForProject(Project project,
          ProjectServiceEnum[] services) {
    //TODO: use copy instead
    List<ProjectServices> newSrvs = new ArrayList<>(services.length);
    List<ProjectServices> toPersist = new ArrayList<>(services.length);
    for (ProjectServiceEnum sse : services) {
      ProjectServices c = new ProjectServices(project, sse);
      newSrvs.add(c);
      toPersist.add(c);
    }
    List<ProjectServices> current = getAllProjectServicesForProject(project);

    toPersist.removeAll(current);
    current.removeAll(newSrvs);

    for (ProjectServices se : toPersist) {
      em.persist(se);
    }
    for (ProjectServices se : current) {
      em.remove(se);
    }
  }

  public void addServiceForProject(Project project, ProjectServiceEnum service) {
    if (!findEnabledServicesForProject(project).contains(service)) {
      ProjectServices ss = new ProjectServices(project, service);
      em.persist(ss);
    }
  }

  public void removeServiceForProject(Project project, ProjectServiceEnum service) {
    ProjectServices c = em.find(ProjectServices.class, new ProjectServicePK(project.getId(),
            service));
    if (c != null) {
      em.remove(c);
    }
  }

  public List<ProjectServices> getAllProjectServicesForProject(Project project) {
    Query q = em.createNamedQuery("ProjectServices.findByProject",
            ProjectServices.class);
    q.setParameter("project", project);
    return q.getResultList();
  }

}
