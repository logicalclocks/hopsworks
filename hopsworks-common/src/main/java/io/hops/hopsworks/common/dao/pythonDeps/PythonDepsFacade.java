package io.hops.hopsworks.common.dao.pythonDeps;

import io.hops.hopsworks.common.dao.host.Host;
import io.hops.hopsworks.common.dao.host.HostEJB;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.util.HopsUtils;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.util.WebCommunication;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import javax.annotation.Resource;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.ws.rs.core.Response;

@Stateless
public class PythonDepsFacade {

  private final static Logger logger = Logger.getLogger(PythonDepsFacade.class.
          getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @EJB
  Settings settings;
  @EJB
  ProjectFacade projectFacade;
  @EJB
  HostEJB hostsFacade;
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

    public AnacondaTask(WebCommunication web, String proj, Host host,
            AnacondaOp op, String arg) {
      this.web = web;
      this.proj = proj;
      this.host = host;
      this.op = op;
      this.arg = arg == null ? "" : arg;
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

    public CondaTask(WebCommunication web, Project proj, Host host, CondaOp op,
            PythonDep dep) {
      this.web = web;
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

  public PythonDep findPythonDeps(String lib, String version) {
    TypedQuery<PythonDep> query = em.createNamedQuery(
            "findByDependencyAndVersion",
            PythonDep.class);
    query.setParameter("lib", lib);
    query.setParameter("version", version);
    return query.getSingleResult();
  }

//  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public Collection<PythonDep> createProjectInDb(Project project,
          Map<String, String> libs) throws AppException {
    List<PythonDep> all = new ArrayList<>();
    AnacondaRepo repoUrl = getRepo(project, settings.getCondaChannelUrl(), true);
    for (String k : libs.keySet()) {
      PythonDep pd = getDep(repoUrl, k, libs.get(k), true);
      pd.setStatus(PythonDep.Status.INSTALLED);
      Collection<Project> projs = pd.getProjectCollection();
      projs.add(project);
      all.add(pd);
    }
    Collection<PythonDep> projDeps = project.getPythonDepCollection();
    projDeps.addAll(all);
    for (PythonDep p : all) {
      em.merge(project);
      em.persist(p);
      em.flush();
    }

    return all;
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

  public AnacondaRepo getRepo(Project proj, String channelUrl, boolean create)
          throws
          AppException {
    TypedQuery<AnacondaRepo> query = em.createNamedQuery(
            "AnacondaRepo.findByUrl", AnacondaRepo.class);
    query.setParameter("url", channelUrl);
    AnacondaRepo repo = null;
    try {
      repo = query.getSingleResult();
    } catch (NoResultException ex) {
      if (create) {
        repo = new AnacondaRepo();
        repo.setUrl(channelUrl);
        em.persist(repo);
        em.flush();
      }

    }
    if (repo == null) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), "Problem adding the repo.");
    }
    return repo;
  }

  private PythonDep getDep(AnacondaRepo repo, String dependency, String version,
          boolean create) throws AppException {
    TypedQuery<PythonDep> deps = em.createNamedQuery(
            "PythonDep.findByDependencyAndVersion", PythonDep.class);
    deps.setParameter("dependency", dependency);
    deps.setParameter("version", version);
    PythonDep dep = null;
    try {
      dep = deps.getSingleResult();
    } catch (NoResultException ex) {
      if (create) {
        dep = new PythonDep();
        dep.setRepoUrl(repo);
        dep.setDependency(dependency);
        dep.setVersion(version);
        em.persist(dep);
        em.flush();
      }
    }
    if (dep == null) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), "Problem adding the repo.");
    }
    return dep;
  }

  public List<PythonDep> listProject(Project proj) throws AppException {
    List<PythonDep> libs = new ArrayList<>();
    Collection<PythonDep> objs = proj.getPythonDepCollection();
    if (objs != null) {
      libs.addAll(objs);
    }
    return libs;
  }

  private List<Host> getHosts() throws AppException {
    List<Host> hosts = new ArrayList<>();
    try {
      hosts = hostsFacade.find();
    } catch (Exception ex) {
      Logger.getLogger(PythonDepsFacade.class.getName()).log(Level.SEVERE, null,
              ex);
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), "Problem adding the project python deps.");
    }
    return hosts;
  }

  /**
   *
   * @param project
   * @throws AppException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public Map<String, String> createProject(Project proj) throws AppException {
    anaconda(AnacondaOp.CREATE, proj.getName(), "", getHosts());

    // First list the libraries already installed and put them in the 
    Map<String, String> depVers = new HashMap<String, String>();
    try {
      String prog = settings.getHopsworksDomainDir() + "/bin/condalist.sh";
      ProcessBuilder pb = new ProcessBuilder(prog);
      Process process = pb.start();
      StringBuilder sb = new StringBuilder();
      BufferedReader br = new BufferedReader(new InputStreamReader(process.
              getInputStream()));
      String line;

      while ((line = br.readLine()) != null) {
        // returns key,value  pairs
        String[] libVersion = line.split(",");
        if (libVersion.length != 2) {
          throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                  getStatusCode(),
                  "Problem listing libraries. Did conda get upgraded and change "
                  + "its output format?");
        }
        // Format is:
        // mkl,2017.0.1
        // numpy,1.11.3
        // openssl,1.0.2k

        String key = libVersion[0];
        String value = libVersion[1];
        depVers.put(key, value);
      }
      int errCode = process.waitFor();
      if (errCode == 2) {
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                getStatusCode(),
                "Problem listing libraries with conda - report a bug.");
      } else if (errCode == 1) {
        throw new AppException(Response.Status.NO_CONTENT.
                getStatusCode(),
                "No results found.");
      }

    } catch (IOException | InterruptedException ex) {
      Logger.getLogger(HopsUtils.class
              .getName()).log(Level.SEVERE, null, ex);
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(),
              "Problem listing libraries, conda interrupted on this webserver.");

    }

    return depVers;
  }

  /**
   *
   * @param project
   * @throws AppException
   */
  public void removeProject(String proj) throws AppException {
    anaconda(AnacondaOp.REMOVE, proj, "", getHosts());
  }

  /**
   *
   * @param project
   * @throws AppException
   */
  public void cloneProject(String srcProject, String destProj) throws
          AppException {
    anaconda(AnacondaOp.CLONE, destProj, srcProject, getHosts());
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
  private void anaconda(AnacondaOp op, String proj, String arg, List<Host> hosts)
          throws
          AppException {

    List<Future> waiters = new ArrayList<>();
    for (Host h : hosts) {
      logger.info("Create anaconda enviornment for " + proj + " on " + h.
              getHostname());
      Future<?> f = kagentExecutorService.submit(
              new AnacondaTask(this.web, proj, h,
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

  public LibStatus libStatus(Project proj, PythonDep dep)
          throws AppException {
    LibStatus found = new LibStatus(dep.getRepoUrl().getUrl(), dep.
            getDependency(), dep.getVersion());
    List<HostLibStatus> hosts = found.getHosts();

    TypedQuery<PythonDepHostStatus> query = em.createNamedQuery(
            "PythonDepHostStatus.findByProjectId",
            PythonDepHostStatus.class);
    query.setParameter("projectId", proj.getId());
    List<PythonDepHostStatus> res = query.getResultList();

    for (PythonDepHostStatus p : res) {
      if (p.getPythonDepHostStatusPK().getDepId() == dep.getId() && p.
              getPythonDepHostStatusPK().getProjectId() == proj.getId() && p.
              getPythonDepHostStatusPK().getRepoId() == dep.getRepoUrl().getId()) {
        HostLibStatus hls = new HostLibStatus(p.getPythonDepHostStatusPK().
                getHostId(), p.getStatus().toString());
        hosts.add(hls);
      }
    }
    return found;
  }

  public void addLibrary(Project proj, String channelUrl,
          String dependency,
          String version) throws AppException {
    condaOp(CondaOp.INSTALL, proj, channelUrl, dependency, version);
  }

  public void upgradeLibrary(Project proj, String channelUrl,
          String dependency,
          String version) throws AppException {
    condaOp(CondaOp.UPGRADE, proj, channelUrl, dependency, version);
  }

  public void removeLibrary(Project proj, String channelUrl,
          String dependency,
          String version) throws AppException {
    condaOp(CondaOp.REMOVE, proj, channelUrl, dependency, version);
  }

  private void condaOp(CondaOp op, Project proj, String channelUrl,
          String dependency, String version) throws AppException {

    List<Host> hosts = new ArrayList<>();
    PythonDep dep = null;
    try {
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
        }
        c.remove(dep);
      } else if (op == CondaOp.REMOVE || op == CondaOp.UPGRADE) {
        throw new AppException(Response.Status.NOT_MODIFIED.
                getStatusCode(),
                "This python library is not installed for this project. Cannot execute "
                + op);
      }
      if (op == CondaOp.INSTALL || op == CondaOp.UPGRADE) {
        c.add(dep);
      }
      proj.setPythonDepCollection(c);
      em.merge(proj);
      // This flush keeps the transaction state alive - don't want it to timeout
      em.flush();

      // 4. Mark that the operation is executing at all hosts
      hosts = hostsFacade.find();
      for (Host h : hosts) {
        PythonDepHostStatus phs = new PythonDepHostStatus(proj.getId(),
                dep.getId(), repo.getId(), h.getId(), op);
        em.persist(phs);
        em.flush();
      }
      kagentCalls(hosts, op, proj, dep);
    } catch (Exception ex) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(),
              ex.getMessage());
    }

  }

  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void kagentCalls(List<Host> hosts,
          CondaOp op, Project proj, PythonDep dep) {
    List<Future> waiters = new ArrayList<>();
    for (Host h : hosts) {
      Future<?> f = kagentExecutorService.submit(new PythonDepsFacade.CondaTask(
              this.web, proj, h, op, dep));
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

    PythonDep.Status s = PythonDep.Status.valueOf(status);
    try {
      Project p = projectFacade.findByName(proj);
      AnacondaRepo repo = getRepo(p, channelUrl, false);
      PythonDep dep = getDep(repo, dependency, version, false);
      PythonDepHostStatusPK pk = new PythonDepHostStatusPK(p.getId(), dep.
              getId(), repo.getId(), hostId);
      PythonDepHostStatus phs = new PythonDepHostStatus(pk, CondaOp.valueOf(op.toUpperCase()), s);
      em.merge(s);
      em.flush();
    } catch (Exception ex) {
      logger.log(Level.WARNING,
              "Problem persisting heartbeat about new python dependencies at kagents.."
              + ex.getMessage());
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
