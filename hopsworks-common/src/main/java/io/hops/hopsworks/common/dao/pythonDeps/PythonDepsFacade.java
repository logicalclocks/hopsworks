package io.hops.hopsworks.common.dao.pythonDeps;

import io.hops.hopsworks.common.dao.host.Host;
import io.hops.hopsworks.common.dao.host.HostEJB;
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
import io.hops.hopsworks.common.util.WebCommunication;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.persistence.TypedQuery;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
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
  @EJB
  HostEJB hostsFacade;
  @Resource
  private UserTransaction userTransaction;
  @EJB
  private WebCommunication web;
  @Resource(lookup = "concurrent/kagentExecutorService")
  ManagedExecutorService kagentExecutorService;

  public static enum AnacondaOp {
    CLONE,
    CREATE,
    REMOVE,
    LIST;
  }

  public static enum CondaOp {
    INSTALL,
    REMOVE,
    UPGRADE;
  }

  public class AnacondaTask implements Runnable {

    @EJB
    private WebCommunication web;
    private String proj;
    private Host host;
    private AnacondaOp op;
    private String arg;

    public AnacondaTask(String proj, Host host, AnacondaOp op, String arg) {
      this.proj = proj;
      this.host = host;
      this.op = op;
      this.arg = arg;
    }

    @Override
    public void run() {
      try {
        web.anaconda(host.getHostname(), host.
                getAgentPassword(), op.toString(), proj, arg);
      } catch (Exception ex) {
        Logger.getLogger(PythonDepsFacade.class.getName()).log(Level.SEVERE,
                null, ex);
      }
    }
  }

  public class CondaTask implements Runnable {

    @EJB
    private WebCommunication web;
    private Project proj;
    private Host host;
    private CondaOp op;
    private PythonDep dep;

    public CondaTask(Project proj, Host host, CondaOp op, PythonDep dep) {
      this.proj = proj;
      this.host = host;
      this.op = op;
      this.dep = dep;
    }

    @Override
    public void run() {
      try {
        web.conda(host.getHostname(), host.
                getAgentPassword(), op.toString(), proj.getName(), dep.
                getRepoUrl().getUrl(), dep.getDependency(), dep.getVersion());
      } catch (Exception ex) {
        Logger.getLogger(PythonDepsFacade.class.getName()).log(Level.SEVERE,
                null, ex);
      }
    }
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

  public List<PythonDep> listProject(Project proj) throws AppException {
    List<PythonDep> deps = new ArrayList<>();
    deps.addAll(proj.getPythonDepCollection());
    return deps;
  }

  /**
   *
   * @param project
   * @throws AppException
   */
  public void createProject(String proj) throws AppException {
    anaconda(AnacondaOp.CREATE, proj, "");
  }

  /**
   *
   * @param project
   * @throws AppException
   */
  public void removeProject(String proj) throws AppException {
    anaconda(AnacondaOp.REMOVE, proj, "");
  }

  /**
   *
   * @param project
   * @throws AppException
   */
  public void cloneProject(String srcProject, String destProj) throws
          AppException {
    anaconda(AnacondaOp.CLONE, destProj, srcProject);
  }

  /**
   * Launches a thread per kagent (up to the threadpool max-size limit) that
   * send a REST
   * call to the kagent to execute the anaconda command.
   *
   * @param proj
   * @param op
   * @param arg
   * @throws AppException
   */
  private void anaconda(AnacondaOp op, String proj, String arg) throws
          AppException {

    List<Host> hosts = new ArrayList<>();
    try {
      userTransaction.begin();
      hosts = hostsFacade.find();
      userTransaction.commit();
    } catch (Exception ex) {
      Logger.getLogger(PythonDepsFacade.class.getName()).log(Level.SEVERE, null,
              ex);
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), "Problem adding the project python deps.");
    }
    List<Future> waiters = new ArrayList<>();
    for (Host h : hosts) {
      Future<?> f = kagentExecutorService.submit(new AnacondaTask(proj, h,
              op, arg));
      waiters.add(f);
    }
    for (Future f : waiters) {
      try {
        f.get(10, TimeUnit.SECONDS);
      } catch (InterruptedException ex) {
        Logger.getLogger(PythonDepsFacade.class.getName()).
                log(Level.SEVERE, null, ex);
      } catch (ExecutionException ex) {
        Logger.getLogger(PythonDepsFacade.class.getName()).
                log(Level.SEVERE, null, ex);
      } catch (TimeoutException ex) {
        Logger.getLogger(PythonDepsFacade.class.getName()).
                log(Level.SEVERE, null, ex);
      }
    }

  }

  public void addLibrary(Project proj, String channelUrl, String dependency,
          String version) throws AppException {
    condaOp(CondaOp.INSTALL, proj, channelUrl, dependency, version);
  }

  public void upgradeLibrary(Project proj, String channelUrl, String dependency,
          String version) throws AppException {
    condaOp(CondaOp.UPGRADE, proj, channelUrl, dependency, version);
  }

  public void removeLibrary(Project proj, String channelUrl, String dependency,
          String version) throws AppException {
    condaOp(CondaOp.REMOVE, proj, channelUrl, dependency, version);
  }

  private void condaOp(CondaOp op, Project proj, String channelUrl,
          String dependency, String version) throws AppException {

    List<Host> hosts = new ArrayList<>();
    PythonDep dep = null;
    try {
      userTransaction.begin();
      // 1. test if anacondaRepoUrl exists. If not, add it.
      AnacondaRepo repo = getRepo(proj, channelUrl, true);
      // 2. Test if pythonDep exists. If not, add it.
      dep = getDep(repo, dependency, version, true);

      // 3. Add the python library to the join table for the project
      Collection<PythonDep> c = proj.getPythonDepCollection();
      if (c.contains(dep)) {
        if (op == CondaOp.INSTALL) {
          throw new AppException(Response.Status.NOT_MODIFIED.
                  getStatusCode(),
                  "This python library is already installed on this project");
        } else { // remove or upgrade
          c.remove(dep);
          proj.setPythonDepCollection(c);
          em.merge(proj);
          em.flush();
        }
      } else if (op == CondaOp.REMOVE || op == CondaOp.UPGRADE) {
        throw new AppException(Response.Status.NOT_MODIFIED.
                getStatusCode(),
                "This python library is not installed for this project. Cannot execute "
                + op);
      }
      if (op == CondaOp.INSTALL || op == CondaOp.UPGRADE) {
        c.add(dep);
        proj.setPythonDepCollection(c);
        em.persist(proj);
        em.flush();
      }

      // 4. Mark that the operation is executing at all hosts
      hosts = hostsFacade.find();
      for (Host h : hosts) {
        PythondepHostStatus phs = new PythondepHostStatus();
        phs.setPythondepHostStatusPK(new PythondepHostStatusPK(proj.getId(),
                dep.getId(), repo.getId(), h.getId()));
        em.persist(phs);
      }
      userTransaction.commit();
    } catch (NotSupportedException | SystemException | AppException |
            RollbackException | HeuristicMixedException |
            HeuristicRollbackException | SecurityException |
            IllegalStateException ex) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(),
              ex.getMessage());
    }

    // 5. Send REST calls to all of the kagents using a thread pool - in a different thread
    List<Future> waiters = new ArrayList<>();
    for (Host h : hosts) {
      Future<?> f = kagentExecutorService.submit(new CondaTask(proj, h,
              op, dep));
      waiters.add(f);
    }
    for (Future f : waiters) {
      try {
        f.get(10, TimeUnit.SECONDS);
      } catch (InterruptedException ex) {
        Logger.getLogger(PythonDepsFacade.class.getName()).
                log(Level.SEVERE, null, ex);
      } catch (ExecutionException ex) {
        Logger.getLogger(PythonDepsFacade.class.getName()).
                log(Level.SEVERE, null, ex);
      } catch (TimeoutException ex) {
        Logger.getLogger(PythonDepsFacade.class.getName()).
                log(Level.SEVERE, null, ex);
      }
    }
  }

  public void agentResponse(String proj, String op, String channelUrl,
          String dependency, String version, String status, int hostId) {

    PythondepHostStatus.Status s = PythondepHostStatus.Status.valueOf(status);
    try {
      Project p = project.findByName(proj);
      AnacondaRepo repo = getRepo(p, channelUrl, false);
      PythonDep dep = getDep(repo, dependency, version, false);
      PythondepHostStatusPK pk = new PythondepHostStatusPK(p.getId(), dep.
              getId(), repo.getId(), hostId);
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
