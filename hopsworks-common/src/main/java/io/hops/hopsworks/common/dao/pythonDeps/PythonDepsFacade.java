package io.hops.hopsworks.common.dao.pythonDeps;

import io.hops.hopsworks.common.dao.host.Host;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
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
  @EJB
  ProjectFacade project;

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

  private AnacondaRepo getRepo(Project proj, String channelUrl, boolean create)
          throws
          AppException {
    TypedQuery<AnacondaRepo> query = em.createNamedQuery(
            "AnacondaRepo.findByUrl", AnacondaRepo.class);
    query.setParameter("url", channelUrl);
    AnacondaRepo repo = query.getSingleResult();
    if (repo == null && create) {
      repo = new AnacondaRepo();
      repo.setUrl(channelUrl);
      em.persist(proj);
      em.flush();
    }
    return repo;
  }

  private PythonDep getDep(AnacondaRepo repo, String dependency, String version,
          boolean create) throws AppException {
    TypedQuery<PythonDep> deps = em.createNamedQuery(
            "PythonDep.findByDependencyAndVersion", PythonDep.class);
    deps.setParameter("dependency", dependency);
    deps.setParameter("version", version);
    PythonDep dep = deps.getSingleResult();
    if (dep == null && create) {
      dep = new PythonDep();
      dep.setRepoUrl(repo);
      dep.setDependency(dependency);
      dep.setVersion(version);
      em.persist(dep);
      em.flush();
    }
    return dep;
  }

  public List<Host> installLibrary(Project proj, String channelUrl,
          String dependency, String version) throws AppException {

    try {
      // 1. test if anacondaRepoUrl exists. If not, add it.
      AnacondaRepo repo = getRepo(proj, channelUrl, true);
      // 2. Test if pythonDep exists. If not, add it.
      PythonDep dep = getDep(repo, dependency, version, true);

      // 3. Add the python library to the join table for the project
      Collection<PythonDep> c = proj.getPythonDepCollection();
      if (c.contains(dep)) {
        throw new AppException(Response.Status.NOT_MODIFIED.
                getStatusCode(),
                "This python library is already installed on this project");
      }
      c.add(dep);
      proj.setPythonDepCollection(c);
      em.persist(proj);
      em.flush();

      // 4. Mark that the library is installing at all hosts
      TypedQuery<Host> allHosts = em.createNamedQuery("Host.find", Host.class);
      List<Host> hosts = allHosts.getResultList();
      for (Host h : hosts) {
        PythondepHostStatus phs = new PythondepHostStatus();
        phs.setPythondepHostStatusPK(new PythondepHostStatusPK(proj.getId(),
                dep.getId(), repo.getId(), h.getId()));
        em.persist(phs);
      }
      return hosts;
    } catch (Exception ex) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(),
              ex.getMessage());
    }

    // 5. Send REST calls to all of the kagents using a thread pool - in a different thread
  }

  public List<Host> updateLibrary(Project proj, String channelUrl,
          String dependency, String version) throws AppException {

    try {
      AnacondaRepo repo = getRepo(proj, channelUrl, false);
      PythonDep dep = getDep(repo, dependency, version, false);
      TypedQuery<Host> allHosts = em.createNamedQuery("Host.find", Host.class);
      List<Host> hosts = allHosts.getResultList();
      Collection<PythonDep> c = proj.getPythonDepCollection();
      if (c.contains(dep) == false) {
        throw new AppException(Response.Status.NOT_MODIFIED.
                getStatusCode(),
                "This python library is not installed for this project. Cannot upgrade.");
      }
      return hosts;

    } catch (Exception ex) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(),
              ex.getMessage());
    }
  }

  public void agentResponse(String proj, String op, String channelUrl,
          String dependency, String version, String status, int hostId) {

    PythondepHostStatus.Status s = PythondepHostStatus.Status.valueOf(status);
    try {
      Project p = project.findByName(proj);
      AnacondaRepo repo = getRepo(p, channelUrl, false);
      PythonDep dep = getDep(repo, dependency, version, false);
      PythondepHostStatusPK pk = new PythondepHostStatusPK(p.getId(), dep.getId(), repo.getId(), hostId);
      PythondepHostStatus phs = new PythondepHostStatus(pk, s);
      em.persist(s);
      em.flush();
    } catch (Exception ex) {
      // Do nothing
    }

  }

//  private <T extends MyClass> T persistOrMerge(T t) {
//    if (t.getId() == null) {
//      em.persist(t);
//      return t;
//    } else {
//      return em.merge(t);
//    }
//  }
}
