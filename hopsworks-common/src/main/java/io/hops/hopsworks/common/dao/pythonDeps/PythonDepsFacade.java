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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

  public static enum CondaOp {
    CLONE,
    CREATE,
    REMOVE,
    LIST,
    INSTALL,
    UNINSTALL,
    UPGRADE;

    public static boolean isEnvOp(CondaOp arg) {
      if (arg.compareTo(CondaOp.CLONE) == 0 || arg.compareTo(CondaOp.CREATE)
              == 0 || arg.compareTo(CondaOp.REMOVE) == 0) {
        return true;
      }
      return false;
    }
  }

  public enum CondaStatus {
    INSTALLED,
    ONGOING,
    FAILED
  }

  public class AnacondaTask implements Runnable {

    @EJB
    private final WebCommunication web;
    private final String proj;
    private final Host host;
    private final CondaOp op;
    private final String arg;

    public AnacondaTask(WebCommunication web, String proj, Host host,
            CondaOp op, String arg) {
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
    private final WebCommunication web;
    private final Project proj;
    private final Host host;
    private final CondaOp op;
    private final PythonDep dep;

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

  public PythonDep findPythonDeps(String lib, String version, boolean pythonKernelEnable) {
    TypedQuery<PythonDep> query = em.createNamedQuery(
            "findByDependencyAndVersion",
            PythonDep.class);
    query.setParameter("lib", lib);
    query.setParameter("version", version);
    return query.getSingleResult();
  }

  public Collection<PythonDep> createProjectInDb(Project project,
          Map<String, String> libs, String pythonVersion, boolean enablePythonKernel) throws AppException {
    if (pythonVersion.compareToIgnoreCase("2.7") != 0 && pythonVersion.
            compareToIgnoreCase("3.5") != 0 && pythonVersion.
            compareToIgnoreCase("3.6") != 0 && pythonVersion.contains("X") == false) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Invalid version of python " + pythonVersion
              + " (valid: '2.7', and '3.5', and '3.6'");
    }
    condaEnvironmentOp(CondaOp.CREATE, project, pythonVersion,  getHosts());

    List<PythonDep> all = new ArrayList<>();
    projectFacade.enableConda(project);
    em.flush();

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
          boolean create, boolean preinstalled) throws AppException {
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
        dep.setPreinstalled(preinstalled);
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
   * @param proj
   * @return
   * @throws AppException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public Map<String, String> getPreInstalledLibs(Project proj) throws
          AppException {

    // First list the libraries already installed and put them in the 
    Map<String, String> depVers = new HashMap<>();
    try {
      String prog = settings.getHopsworksDomainDir() + "/bin/condalist.sh";
      ProcessBuilder pb = new ProcessBuilder(prog);
      Process process = pb.start();
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
   * @param proj
   * @throws AppException
   */
  public void removeProject(Project proj) throws AppException {
    if (proj.getConda()) {
      condaEnvironmentOp(CondaOp.REMOVE, proj, "", getHosts());
    }
  }

  /**
   *
   * @param project
   * @throws AppException
   */
  public void cloneProject(Project srcProject, String destProj) throws
          AppException {
    condaEnvironmentOp(CondaOp.CLONE, srcProject, destProj, getHosts());
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
  public void condaEnvironmentOp(CondaOp op, Project proj, String arg,
          List<Host> hosts) throws AppException {
    for (Host h : hosts) {
      CondaCommands cc = new CondaCommands(h, settings.getAnacondaUser(),
              op, CondaStatus.ONGOING, proj, "", "", "default",
              new Date(), arg);
      em.persist(cc);
    }
  }

  public void blockingCondaEnvironmentOp(CondaOp op, String proj, String arg,
          List<Host> hosts) throws AppException {
    List<Future> waiters = new ArrayList<>();
    for (Host h : hosts) {
      logger.log(Level.INFO, "Create anaconda enviornment for {0} on {1}",
              new Object[]{proj, h.getHostname()});
      Future<?> f = kagentExecutorService.submit(
              new AnacondaTask(this.web, proj, h, op, arg));
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

  // , PythonDep dep
  public List<OpStatus> opStatus(Project proj)
          throws AppException {
    Collection<CondaCommands> commands = proj.getCondaCommandsCollection();
    List<OpStatus> ops = new ArrayList<>();
    Set<CondaOp> uniqueOps = new HashSet<>();
    for (CondaCommands cc : commands) {
      uniqueOps.add(cc.getOp());
    }
    // For every unique operation, iterate through the commands and add an entry for it.
    // Inefficient, O(N^2) - but its small data
    // Set the status for the operation as a whole (on OpStatus) based on the status of
    // the operation on all hosts (if all finished => OpStatus.status = Installed).
    for (CondaOp co : uniqueOps) {
      OpStatus os = new OpStatus();
      os.setOp(co.toString());
      for (CondaCommands cc : commands) {
        boolean failed = false;
        boolean installing = false;
        if (cc.getOp() == co) {
          os.setChannelUrl(cc.getChannelUrl());
          os.setLib(cc.getLib());
          os.setVersion(cc.getVersion());
          Host h = cc.getHostId();
          os.addHost(new HostOpStatus(h.getId(), cc.getStatus().toString()));
          if (cc.getStatus() == CondaStatus.FAILED) {
            failed = true;
          }
          if (cc.getStatus() == CondaStatus.ONGOING) {
            installing = true;
          }
        }
        if (failed) {
          os.setStatus(CondaStatus.FAILED.toString());
        } else if (installing) {
          os.setStatus(CondaStatus.ONGOING.toString());
        }
      }
      ops.add(os);
    }
    return ops;
  }

  private void checkForOngoingEnvOp(Project proj) throws AppException {
    List<OpStatus> ongoingOps = opStatus(proj);
    for (OpStatus os : ongoingOps) {
      if (CondaOp.isEnvOp(CondaOp.valueOf(os.getOp().toUpperCase()))) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                "A conda environment operation is currently "
                + "executing (create/remove/list). Wait for it to finish.");
      }
    }
  }

  public void addLibrary(Project proj, String channelUrl,
          String dependency,
          String version) throws AppException {
    checkForOngoingEnvOp(proj);
    condaOp(CondaOp.INSTALL, proj, channelUrl, dependency, version);
  }

  public void upgradeLibrary(Project proj, String channelUrl,
          String dependency,
          String version) throws AppException {
    checkForOngoingEnvOp(proj);
    condaOp(CondaOp.UPGRADE, proj, channelUrl, dependency, version);
  }

  public void removeLibrary(Project proj, String channelUrl,
          String dependency,
          String version) throws AppException {
    checkForOngoingEnvOp(proj);
    condaOp(CondaOp.UNINSTALL, proj, channelUrl, dependency, version);
  }

  private void condaOp(CondaOp op, Project proj, String channelUrl,
          String lib, String version) throws AppException {

    List<Host> hosts = new ArrayList<>();
    PythonDep dep = null;
    try {
      // 1. test if anacondaRepoUrl exists. If not, add it.
      AnacondaRepo repo = getRepo(proj, channelUrl, true);
      // 2. Test if pythonDep exists. If not, add it.
      dep = getDep(repo, lib, version, true, false);

      // 3. Add the python library to the join table for the project
      Collection<PythonDep> depsInProj = proj.getPythonDepCollection();
      if (depsInProj.contains(dep)) {
        if (op == CondaOp.INSTALL) {
          throw new AppException(Response.Status.NOT_MODIFIED.
                  getStatusCode(),
                  "This python library is already installed on this project");
        }
        depsInProj.remove(dep);
      } else if (op == CondaOp.UNINSTALL || op == CondaOp.UPGRADE) {
        throw new AppException(Response.Status.NOT_MODIFIED.
                getStatusCode(),
                "This python library is not installed for this project. Cannot remove/upgrade "
                + op);
      }
      if (op == CondaOp.INSTALL || op == CondaOp.UPGRADE) {
        depsInProj.add(dep);
      }
      proj.setPythonDepCollection(depsInProj);
      em.merge(proj);
      // This flush keeps the transaction state alive - don't want it to timeout
      em.flush();

      // 4. Mark that the operation is executing at all hosts
      hosts = hostsFacade.find();
      for (Host h : hosts) {
        CondaCommands cc = new CondaCommands(h, settings.getAnacondaUser(),
                op, CondaStatus.ONGOING, proj, lib,
                version, channelUrl, new Date(), "");
        em.persist(cc);
      }
//      kagentCalls(hosts, op, proj, dep);
    } catch (Exception ex) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(),
              ex.getMessage());
    }

  }

  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void blockingCondaOp(int hostId, CondaOp op,
          Project proj, String channelUrl,
          String lib, String version) throws AppException {
    Host host = em.find(Host.class, hostId);

    AnacondaRepo repo = getRepo(proj, channelUrl, false);
    PythonDep dep = getDep(repo, lib, version, false, false);
    Future<?> f = kagentExecutorService.submit(new PythonDepsFacade.CondaTask(
            this.web, proj, host, op, dep));
    try {
      f.get(1000, TimeUnit.SECONDS);
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

  public void agentResponse(int commandId, String status,
          List<CondaCommands> commands) {

    PythonDepsFacade.CondaStatus s = PythonDepsFacade.CondaStatus.valueOf(
            status.toUpperCase());

  }

//  public void agentResponse(String proj, String op, String channelUrl,
//          String dependency, String version, String status, int hostId, String arg) {
//
//    PythonDepsFacade.CondaStatus s = PythonDepsFacade.CondaStatus.
//            valueOf(status);
//    try {
//      Project p = projectFacade.findByName(proj);
//      if (p == null) {
//        CondaCommands kc = new CondaCommands();
//        kc.setCreated(Date.from(Instant.now()));
//        kc.setProj(proj);
//        kc.setOp(CondaOp.REMOVE);
//        kc.setUser(settings.getSparkUser());
//        kc.setArg(arg);
//        em.persist(kc);
//      } else if (isAnacondaOp(op)) {
//
//      } else {
////        AnacondaRepo repo = getRepo(p, channelUrl, false);
////        PythonDep dep = getDep(repo, dependency, version, false);
////        PythonDepHostStatusPK pk = new PythonDepHostStatusPK(p.getId(), dep.
////                getId(), repo.getId(), hostId);
////        PythonDepHostStatus phs = new PythonDepHostStatus(pk, CondaOp.valueOf(
////                op.toUpperCase()), s);
////        em.merge(phs);
//
//      }
////      em.flush();
//    } catch (Exception ex) {
//      // TODO - if i can't find a project, tell the node that there is a problem
//      // and to delete the project locally. Do this by putting a command in an
//      // row in a table (entity bean)
//      logger.log(Level.WARNING,
//              "Problem persisting heartbeat about new python dependencies at kagents.."
//              + ex.getMessage());
//      // Do nothing
//    }
//
//  }
  private boolean isAnacondaOp(String op) {
    CondaOp condaOp = CondaOp.valueOf(op.toUpperCase());
    if (condaOp == CondaOp.CLONE || condaOp == CondaOp.CREATE || condaOp
            == CondaOp.LIST) {
      return true;
    }
    return false;
  }

  public CondaCommands findCondaCommand(int commandId) {
    return em.find(CondaCommands.class, commandId);
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void removeCondaCommand(int commandId) {
    CondaCommands cc = findCondaCommand(commandId);
    if (cc != null) {
      em.remove(cc);
      em.flush();
    } else {
      logger.log(Level.FINE, "Could not remove CondaCommand with id: {0}",
              commandId);
    }
  }

//  public void updateCondaComamandStatus(int commandId, String status, String arg) {
//    PythonDepsFacade.CondaStatus s = PythonDepsFacade.CondaStatus.valueOf(
//            status.toUpperCase());
//    updateCondaComamandStatus(commandId, s, arg);
//  }
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void updateCondaComamandStatus(int commandId, CondaStatus condaStatus,
          String arg, String proj, CondaOp opType, String lib, String version) {
    CondaCommands cc = findCondaCommand(commandId);
    if (cc != null) {
      if (condaStatus == CondaStatus.INSTALLED) {
        // remove completed commands
        em.remove(cc);
        em.flush();
        // Check if this is the last operation for this project. If yes, set
        // the PythonDep to be installed or 
        // the CondaEnv operation is finished implicitly (no condaOperations are 
        // returned => CondaEnv operation is finished).
        if (!CondaOp.isEnvOp(opType)) {
          Project p = projectFacade.findByName(proj);
          Collection<CondaCommands> ongoingCommands = p.
                  getCondaCommandsCollection();
          boolean finished = true;
          for (CondaCommands c : ongoingCommands) {
            if (c.getOp().compareTo(opType) == 0 && c.getLib().compareTo(lib)
                    == 0 && c.getVersion().compareTo(version) == 0) {
              finished = false;
              break;
            }
          }
          if (finished) {
//          findPythonDeps(lib, version);
            Collection<PythonDep> deps = p.getPythonDepCollection();
            for (PythonDep pd : deps) {
              if (pd.getDependency().compareTo(lib) == 0 && pd.getVersion().
                      compareTo(version) == 0) {
                pd.setStatus(condaStatus);
                em.merge(pd);
                break;
              }
            }
          }
        }
      } else {
        cc.setStatus(condaStatus);
        cc.setArg(arg);
        em.merge(cc);
      }
    } else {
      logger.log(Level.FINE, "Could not remove CondaCommand with id: {0}",
              commandId);
    }
  }

}
