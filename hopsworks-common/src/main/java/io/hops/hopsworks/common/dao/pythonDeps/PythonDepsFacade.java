package io.hops.hopsworks.common.dao.pythonDeps;

import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.exception.AppException;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import io.hops.hopsworks.common.util.Settings;
import java.util.ArrayList;
import java.util.Collection;
import javax.persistence.TypedQuery;
import javax.ws.rs.core.Response;

@Stateless
public class PythonDepsFacade {

  private final static Logger LOGGER = Logger.getLogger(PythonDepsFacade.class.
          getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @EJB
  Settings settings;
//  @EJB
//  PythonDep pythonDeps;
//  @EJB
//  AnacondaRepo repos;

  protected EntityManager getEntityManager() {
    return em;
  }

  public PythonDepsFacade() throws Exception {
  }

  /**
   * Get all the Python Deps for the given project and channel
   * <p/>
   * @param projectId
   * @return
   */
  public List<PythonDep> findInstalledPythonDepsByCondaChannel(String channelUrl)
          throws AppException {
    TypedQuery<AnacondaRepo> query = em.createNamedQuery(
            "AnacondaRepo.findByUrl",
            AnacondaRepo.class);
    query.setParameter("url", channelUrl);
    List<AnacondaRepo> res = query.getResultList();
    if (res != null && res.size() > 0) {
      // There should only be '1' url
      AnacondaRepo r = res.get(0);
      Collection<PythonDep> c = r.getPythonDepCollection();
      List<PythonDep> list = new ArrayList<PythonDep>(c);
      return list;
    }
    return null;
  }

  public boolean removeLibraryFromProject(Project proj,
          PythonDepJson removeLibrary) throws AppException {
    Collection<PythonDep> c = proj.getPythonDepCollection();
    PythonDep pd = null;
    for (PythonDep p : c) {
      if (removeLibrary.getDependency().compareToIgnoreCase(p.getDependency())
              == 0) {
        pd = p;
      }
    }
    if (pd != null) {
      c.remove(pd);
      proj.setPythonDepCollection(c);

      try {
        em.persist(proj);
        em.flush();
      } catch (Exception ex) {
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                getStatusCode(),
                ex.getMessage());
      }
      return true;
    }
    return false;
  }

  public void installLibraryToProject(Project proj, String channelUrl,
          String dependency, String version) throws AppException {

    // 1. test if anacondaRepoUrl exists. If not, add it.
    TypedQuery<AnacondaRepo> query = em.createNamedQuery(
            "AnacondaRepo.findByUrl", AnacondaRepo.class);
    query.setParameter("url", channelUrl);
    AnacondaRepo repo = query.getSingleResult();
    if (repo == null) {
      repo = new AnacondaRepo();
      repo.setUrl(channelUrl);
      em.persist(proj);
      em.flush();
    }

    // 2. Test if pythonDep exists. If not, add it.
    TypedQuery<PythonDep> deps = em.createNamedQuery(
            "PythonDep.findByDependencyAndVersion", PythonDep.class);
    deps.setParameter("dependency", dependency);
    deps.setParameter("version", version);
    PythonDep dep = deps.getSingleResult();
    if (dep == null) {
      dep = new PythonDep();
      dep.setRepoUrl(repo);
      dep.setDependency(dependency);
      dep.setVersion(version);
      em.persist(dep);
      em.flush();
    }

    // 3. Add the python library to the join table for the project
    Collection<PythonDep> c = proj.getPythonDepCollection();
    if (c.contains(dep)) {
      throw new AppException(Response.Status.NOT_MODIFIED.
              getStatusCode(),
              "This python library is already installed on this project");
    }
    c.add(dep);
    proj.setPythonDepCollection(c);
    try {
      em.persist(proj);
      em.flush();
    } catch (Exception ex) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(),
              ex.getMessage());
    }

  }

}
