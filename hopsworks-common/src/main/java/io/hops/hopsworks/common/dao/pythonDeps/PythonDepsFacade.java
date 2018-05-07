/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package io.hops.hopsworks.common.dao.pythonDeps;

import io.hops.hopsworks.common.dao.host.Hosts;
import io.hops.hopsworks.common.dao.host.HostsFacade;
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
  HostsFacade hostsFacade;
  @EJB
  private WebCommunication web;
  @Resource(lookup = "concurrent/kagentExecutorService")
  ManagedExecutorService kagentExecutorService;

  public boolean isEnvironmentReady(Project project) {
    CondaOp operation = null;
    CondaStatus status = null;
    List<CondaCommands> ops = getCommandsForProject(project);
    for (CondaCommands condaCommand : ops) {
      operation = condaCommand.getOp();
      if (operation.equals(CondaOp.CREATE)) {
        status = condaCommand.getStatus();
        if (status.equals(CondaStatus.NEW) || status.equals(CondaStatus.ONGOING)) {
          return false;
        }
      }
    }
    return true;
  }

  public enum CondaOp {
    CLONE,
    CREATE,
    BACKUP,
    REMOVE,
    LIST,
    INSTALL,
    UNINSTALL,
    UPGRADE;

    public boolean isEnvOp() {
      return CondaOp.isEnvOp(this);
    }
    public static boolean isEnvOp(CondaOp arg) {
      if (arg.compareTo(CondaOp.CLONE) == 0 || arg.compareTo(CondaOp.CREATE)
          == 0 || arg.compareTo(CondaOp.REMOVE) == 0 || arg.compareTo(CondaOp.BACKUP) == 0) {
        return true;
      }
      return false;
    }
  }

  public enum CondaInstallType {
    ENVIRONMENT,
    CONDA,
    PIP
  }

  public enum CondaStatus {
    NEW,
    SUCCESS,
    ONGOING,
    FAILED
  }

  public enum MachineType {
    ALL,
    CPU,
    GPU
  }

  public class AnacondaTask implements Runnable {

    @EJB
    private final WebCommunication web;
    private final String proj;
    private final Hosts host;
    private final CondaOp op;
    private final String arg;
    private Object entity;

    public AnacondaTask(WebCommunication web, String proj, Hosts host, CondaOp op, String arg) {
      this.web = web;
      this.proj = proj;
      this.host = host;
      this.op = op;
      this.arg = arg == null ? "" : arg;
    }

    @Override
    public void run() {
      try {
        entity = web.anaconda(host.getHostIp(), host.
            getAgentPassword(), op.toString(), proj, arg);
      } catch (Exception ex) {
        Logger.getLogger(PythonDepsFacade.class.getName()).log(Level.SEVERE,
            null, ex);
      }
    }

    public Object getEntity() {
      return entity;
    }

  }

  public class CondaTask implements Runnable {

    @EJB
    private final WebCommunication web;
    private final Project proj;
    private final Hosts host;
    private final CondaOp op;
    private final PythonDep dep;
    private Object entity;

    public CondaTask(WebCommunication web, Project proj, Hosts host, CondaOp op,
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
        this.entity = web.conda(host.getHostIp(), host.
            getAgentPassword(), op.toString(), proj.getName(), dep.
            getRepoUrl().getUrl(), dep.getDependency(), dep.getVersion());
      } catch (Exception ex) {
        Logger.getLogger(PythonDepsFacade.class.getName()).log(Level.SEVERE,
            null, ex);
      }
    }

    public Object getEntity() {
      return entity;
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
    condaEnvironmentOp(CondaOp.CREATE, pythonVersion, project, pythonVersion);

    List<PythonDep> all = new ArrayList<>();
    projectFacade.enableConda(project);
    em.flush();

    return all;
  }

  /**
   * Get all the Python Deps for the given project and channel
   * @param channelUrl
   * @return
   */
  public List<PythonDep> findInstalledPythonDepsByCondaChannel(String channelUrl) {
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

  public PythonDep getDep(AnacondaRepo repo, MachineType machineType, CondaInstallType installType, String dependency,
      String version, boolean create, boolean preinstalled) throws AppException {
    TypedQuery<PythonDep> deps = em.createNamedQuery(
        "PythonDep.findUniqueDependency", PythonDep.class);
    deps.setParameter("dependency", dependency);
    deps.setParameter("version", version);
    deps.setParameter("installType", installType);
    deps.setParameter("repoUrl", repo);
    deps.setParameter("machineType", machineType);
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
        dep.setInstallType(installType);
        dep.setMachineType(machineType);
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

  public List<OpStatus> getFailedCondaOpsProject(Project proj) throws AppException {
    List<OpStatus> libs = new ArrayList<>();
    Collection<CondaCommands> objs = proj.getCondaCommandsCollection();
    if (objs != null) {
      for (CondaCommands cc : objs) {
        if (cc.getStatus() == PythonDepsFacade.CondaStatus.FAILED) {
          String libName = cc.getLib();
          String version = cc.getVersion();
          boolean alreadyAdded = false;
          for (OpStatus os : libs) {
            if (os.getLib().compareToIgnoreCase(libName) == 0) {
              alreadyAdded = true;
              os.addHost(new HostOpStatus(cc.getHostId().getHostname(),
                  PythonDepsFacade.CondaStatus.FAILED.toString()));
              break;
            }
          }
          if (!alreadyAdded) {
            libs.add(new OpStatus(cc.getOp().toString(), cc.getInstallType().name(),
                cc.getMachineType().name(), cc.getChannelUrl(), libName, version));
          }
        }
      }
    }
    return libs;
  }

  public void retryFailedCondaOpsProject(Project proj) throws AppException {
    Collection<CondaCommands> objs = proj.getCondaCommandsCollection();
    List<CondaCommands> failedCCs = new ArrayList<>();
    if (objs != null) {
      for (CondaCommands cc : objs) {
        if (cc.getStatus() == PythonDepsFacade.CondaStatus.FAILED) {
          failedCCs.add(cc);
        }
      }
      for (CondaCommands cc : failedCCs) {
        cc.setStatus(CondaStatus.NEW);
        em.merge(cc);
      }
    }
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

  public void removePythonDepsForProject(Project proj) throws AppException {
    Collection<PythonDep> deps = new ArrayList();
    proj.setPythonDepCollection(deps);
    projectFacade.update(proj);
  }

  public void addPythonDepsForProject(Project proj, Collection<PythonDep> pythonDeps) throws AppException {
    proj.setPythonDepCollection(pythonDeps);
    projectFacade.update(proj);
  }

  private void removePythonForProject(Project proj) {
    Collection<PythonDep> deps = proj.getPythonDepCollection();
    proj.setPythonDepCollection(new ArrayList<PythonDep>());
    proj.setPythonVersion("");
    proj.setConda(false);
    projectFacade.update(proj);
  }

  public void deleteCommandsForProject(Project proj) {
    List<CondaCommands> commands = getCommandsForProject(proj);
    for (CondaCommands cc : commands) {
      // First, remove any old commands for the project in conda_commands
      em.remove(cc);
    }
  }

  public int deleteAllCommandsByStatus(CondaStatus status) {
    TypedQuery<CondaCommands> query = em.createNamedQuery("CondaCommands.deleteAllFailedCommands", CondaCommands.class);
    query.setParameter("status", status);
    return query.executeUpdate();
  }

  public List<CondaCommands> getCommandsForProject(Project proj) {
    TypedQuery<CondaCommands> query = em.createNamedQuery("CondaCommands.findByProj", CondaCommands.class);
    query.setParameter("projectId", proj);
    return query.getResultList();
  }

  /**
   *
   * @param proj
   * @throws AppException
   */
  public void removeProject(Project proj) throws AppException {
    deleteCommandsForProject(proj);
    if (proj.getConda()) {
      condaEnvironmentRemove(proj);
    }
    removePythonForProject(proj);
  }

  /**
   * @param srcProject
   * @throws AppException
   */
  public void cloneProject(Project srcProject, Project destProj) throws
      AppException {
    condaEnvironmentClone(srcProject, destProj);
  }

  /**
   * Asynchronous execution of conda operations
   *
   * @param op
   * @param proj
   * @param pythonVersion
   * @param arg
   * @throws AppException
   */
  private void condaEnvironmentOp(CondaOp op, String pythonVersion, Project proj, String arg) throws AppException {
    List<Hosts> hosts = hostsFacade.getCondaHosts(MachineType.ALL);
    if (hosts.size() == 0) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR, "No conda machine enabled. Contact the admin.");
    }

    for (Hosts h : hosts) {
      // For environment operations, we don't care about the Conda Channel, so we just pick 'defaults'
      CondaCommands cc = new CondaCommands(h, settings.getAnacondaUser(),
          op, CondaStatus.NEW, CondaInstallType.ENVIRONMENT, MachineType.ALL, proj, pythonVersion, "",
          "defaults", new Date(), arg);
      em.persist(cc);
    }
  }

  private void condaEnvironmentRemove(Project proj) throws AppException {
    condaEnvironmentOp(CondaOp.REMOVE, "", proj, "");
  }

  private void condaEnvironmentClone(Project srcProj, Project destProj) throws AppException {
    condaEnvironmentOp(CondaOp.CLONE, "", srcProj, destProj.getName());
  }

  /**
   * Launches a thread per kagent (up to the threadpool max-size limit) that
   * send a REST
   * call to the kagent to execute the anaconda command.
   *
   * @param op
   * @param proj
   * @param arg
   * @param hosts
   * @throws AppException
   */
  public void blockingCondaEnvironmentOp(CondaOp op, String proj, String arg, List<Hosts> hosts) throws AppException {
    List<Future> waiters = new ArrayList<>();
    for (Hosts h : hosts) {
      logger.log(Level.INFO, "Create anaconda enviornment for {0} on {1}",
          new Object[]{proj, h.getHostIp()});
      Future<?> f = kagentExecutorService.submit(
          new AnacondaTask(this.web, proj, h, op, arg));
      waiters.add(f);
    }
    for (Future f : waiters) {
      try {
        f.get(10, TimeUnit.SECONDS);
      } catch (InterruptedException | ExecutionException | TimeoutException ex) {
        Logger.getLogger(PythonDepsFacade.class.getName()).
            log(Level.SEVERE, null, ex);
      }
    }

  }

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
          os.setInstallType(cc.getInstallType().name());
          os.setMachineType(cc.getMachineType().name());
          Hosts h = cc.getHostId();
          os.addHost(new HostOpStatus(h.getHostname(), cc.getStatus().toString()));
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
            + "executing (create/remove/list). Wait for it to finish or clear it first..");
      }
    }
  }

  public void addLibrary(Project proj, CondaInstallType installType, MachineType machineType,
      String channelUrl, String dependency, String version) throws AppException {
    checkForOngoingEnvOp(proj);
    condaOp(CondaOp.INSTALL, installType, machineType, proj, channelUrl, dependency, version);
  }

  public void upgradeLibrary(Project proj, CondaInstallType installType, MachineType machineType, String channelUrl,
      String dependency,
      String version) throws AppException {
    checkForOngoingEnvOp(proj);
    condaOp(CondaOp.UPGRADE, installType, machineType, proj, channelUrl, dependency, version);
  }

  public void clearCondaOps(Project proj, String channelUrl,
      String dependency, String version) throws AppException {
    List<CondaCommands> commands = getCommandsForProject(proj);
    for (CondaCommands cc : commands) {
      // delete the conda library command if it has the same name as the input library name
      if (cc.getLib().compareToIgnoreCase(dependency) == 0) {
        em.remove(cc);
      }
    }
  }

  public void uninstallLibrary(Project proj, CondaInstallType installType, MachineType machineType, String channelUrl,
      String dependency,
      String version) throws AppException {
    checkForOngoingEnvOp(proj);
    try {
      condaOp(CondaOp.UNINSTALL, installType, machineType, proj, channelUrl, dependency, version);
    } catch (AppException ex) {
      // do nothing - already uninstalled
    }
  }

  private void condaOp(CondaOp op, CondaInstallType installType, MachineType machineType, Project proj,
      String channelUrl, String lib, String version) throws AppException {

    List<Hosts> hosts = hostsFacade.getCondaHosts(machineType);
    if(hosts.size() == 0) {
      throw new AppException(Response.Status.NOT_FOUND,
          "No hosts with the desired capability: " + machineType.name());
    }

    try {
      // 1. test if anacondaRepoUrl exists. If not, add it.
      AnacondaRepo repo = getRepo(proj, channelUrl, true);
      // 2. Test if pythonDep exists. If not, add it.
      PythonDep dep = getDep(repo, machineType, installType, lib, version, true, false);

      // 3. Add the python library to the join table for the project
      Collection<PythonDep> depsInProj = proj.getPythonDepCollection();
      if (depsInProj.contains(dep)) {
        if (op == CondaOp.INSTALL) {
          throw new AppException(Response.Status.NOT_MODIFIED.getStatusCode(),
              "This python library is already installed on this project");
        }
        depsInProj.remove(dep);
      } else if (op == CondaOp.UNINSTALL || op == CondaOp.UPGRADE) {
        throw new AppException(Response.Status.NOT_MODIFIED.getStatusCode(),
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

      for (Hosts h : hosts) {
        CondaCommands cc = new CondaCommands(h, settings.getAnacondaUser(),
            op, CondaStatus.NEW, installType, machineType, proj, lib,
            version, channelUrl, new Date(), "");
        em.persist(cc);
      }
//      kagentCalls(hosts, op, proj, dep);
    } catch (Exception ex) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
          ex.getMessage());
    }
  }

  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void blockingCondaOp(int hostId, CondaOp op, CondaInstallType condaInstallType,
      MachineType machineType, Project proj, String channelUrl, String lib, String version) throws AppException {
    Hosts host = em.find(Hosts.class, hostId);

    AnacondaRepo repo = getRepo(proj, channelUrl, false);
    PythonDep dep = getDep(repo, machineType, condaInstallType, lib, version, false, false);
    Future<?> f = kagentExecutorService.submit(new PythonDepsFacade.CondaTask(
        this.web, proj, host, op, dep));
    try {
      f.get(100, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException ex) {
      Logger.getLogger(PythonDepsFacade.class.getName()).
          log(Level.SEVERE, null, ex);
    }
  }

  public void agentResponse(int commandId, String status,
      List<CondaCommands> commands) {

    PythonDepsFacade.CondaStatus s = PythonDepsFacade.CondaStatus.valueOf(
        status.toUpperCase());

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

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public List<CondaCommands> findByHost(Hosts host) {
    TypedQuery<CondaCommands> query = em.createNamedQuery("CondaCommands.findByHost",
        CondaCommands.class);
    query.setParameter("host", host);
    return query.getResultList();
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public List<CondaCommands> findByStatus(PythonDepsFacade.CondaStatus status) {
    TypedQuery<CondaCommands> query = em.createNamedQuery("CondaCommands.findByStatus",
        CondaCommands.class);
    query.setParameter("status", status);
    return query.getResultList();
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void updateCondaCommandStatus(int commandId, CondaStatus condaStatus, CondaInstallType installType,
      MachineType machineType, String arg, String proj, CondaOp opType, String lib, String version,
      String channel) throws AppException {
    CondaCommands cc = findCondaCommand(commandId);
    if (cc != null) {
      if (condaStatus == CondaStatus.SUCCESS) {
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
            if (c.getOp().compareTo(opType) == 0
                && c.getLib().compareTo(lib) == 0
                && c.getVersion().compareTo(version) == 0
                && c.getInstallType().name().compareTo(installType.name()) == 0
                && c.getChannelUrl().compareTo(channel) == 0
                && c.getMachineType().name().compareTo(machineType.name()) == 0) {
              finished = false;
              break;
            }
          }
          if (finished) {
//          findPythonDeps(lib, version);
            Collection<PythonDep> deps = p.getPythonDepCollection();
            for (PythonDep pd : deps) {
              if (pd.getDependency().compareTo(lib) == 0
                  && pd.getVersion().compareTo(version) == 0
                  && pd.getInstallType().name().compareTo(installType.name()) == 0
                  && pd.getRepoUrl().getUrl().compareTo(channel) == 0
                  && pd.getMachineType().name().compareTo(machineType.name()) == 0) {
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
