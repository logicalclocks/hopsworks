/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 */
package io.hops.hopsworks.common.dao.pythonDeps;

import io.hops.hopsworks.common.dao.host.Hosts;
import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.exception.GenericException;
import io.hops.hopsworks.common.exception.ProjectException;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.exception.ServiceException;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.util.WebCommunication;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterProcessMgr;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.util.ProjectUtils;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class PythonDepsFacade {

  private static final Logger LOGGER = Logger.getLogger(PythonDepsFacade.class.
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
  UserFacade userFacade;
  @EJB
  private WebCommunication web;
  @EJB
  private ProjectTeamFacade projectTeamFacade;
  @EJB
  private JupyterProcessMgr jupyterProcessMgr;
  @EJB
  private ProjectUtils projectUtils;

  @Resource(lookup = "concurrent/kagentExecutorService")
  ManagedExecutorService kagentExecutorService;

  public boolean isEnvironmentReady(Project project) {
    CondaOp operation;
    CondaStatus status;
    List<CondaCommands> ops = getCommandsForProject(project);
    for (CondaCommands condaCommand : ops) {
      operation = condaCommand.getOp();
      if (operation.equals(CondaOp.CREATE) || operation.equals(CondaOp.YML)) {
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
    UPGRADE,
    CLEAN,
    YML;

    public boolean isEnvOp() {
      return CondaOp.isEnvOp(this);
    }

    public static boolean isEnvOp(CondaOp arg) {
      if (arg.compareTo(CondaOp.CLONE) == 0 || arg.compareTo(CondaOp.CREATE)
          == 0 || arg.compareTo(CondaOp.YML) == 0 || arg.compareTo(CondaOp.REMOVE) == 0
          || arg.compareTo(CondaOp.BACKUP) == 0 || arg.compareTo(CondaOp.CLEAN) == 0) {
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

  public PythonDepsFacade() {
  }

  public Collection<PythonDep> createProjectInDb(Project project,
      String pythonVersion, MachineType machineType,
      String environmentYml) throws ServiceException {

    if (environmentYml == null && pythonVersion.compareToIgnoreCase("2.7") != 0 && pythonVersion.
        compareToIgnoreCase("3.5") != 0 && pythonVersion.
        compareToIgnoreCase("3.6") != 0 && !pythonVersion.contains("X")) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.PYTHON_INVALID_VERSION,
          Level.INFO, "pythonVersion: " + pythonVersion);
    }

    if (environmentYml != null) {
      condaEnvironmentOp(CondaOp.YML, pythonVersion, project, pythonVersion, machineType, environmentYml);
      setCondaEnv(project, true);
    } else {
      validateCondaHosts(machineType);
    }

    List<PythonDep> all = new ArrayList<>();
    projectFacade.enableConda(project);
    em.flush();

    return all;
  }

  private void setCondaEnv(Project project, boolean condaEnv) {
    project.setCondaEnv(condaEnv);
    projectFacade.mergeProject(project);
  }

  private List<Hosts> validateCondaHosts(MachineType machineType) throws ServiceException {
    List<Hosts> hosts = hostsFacade.getCondaHosts(machineType);
    if (hosts.isEmpty()) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_NODES_UNAVAILABLE, Level.WARNING);
    }
    return hosts;
  }
  
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void recreateAllPythonKernels(Project project) {
    // Update all the python kernels for all users in the project
    if (settings.isPythonKernelEnabled()) {
      List<ProjectTeam> projectTeams = projectTeamFacade.findMembersByProject(project);
      for (ProjectTeam projectTeam : projectTeams) {
        Users newMember = userFacade.findByEmail(projectTeam.getProjectTeamPK().getTeamMember());
        jupyterProcessMgr.createPythonKernelForProjectUser(project, newMember);
      }
    }
  }

  public void copyOnWriteCondaEnv(Project project) throws ServiceException {
    condaEnvironmentOp(CondaOp.CREATE, project.getPythonVersion(), project, project.getPythonVersion(),
        MachineType.ALL, null);
    setCondaEnv(project, true);
  }

  /**
   * Get all the Python Deps for the given project and channel
   *
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

  public AnacondaRepo getRepo(String channelUrl, boolean create)
      throws ServiceException {
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
      throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_REPO_ERROR, Level.SEVERE);
    }
    return repo;
  }

  public PythonDep getDep(AnacondaRepo repo, MachineType machineType, CondaInstallType installType, String dependency,
      String version, boolean create, boolean preinstalled, CondaStatus status) throws ServiceException {
    TypedQuery<PythonDep> deps = em.createNamedQuery(
        "PythonDep.findUniqueDependency", PythonDep.class);
    deps.setParameter("dependency", dependency);
    deps.setParameter("version", version);
    deps.setParameter("installType", installType);
    deps.setParameter("repoUrl", repo);
    deps.setParameter("machineType", machineType);
    deps.setParameter("status", status);
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
        dep.setStatus(status);
        em.persist(dep);
        em.flush();
      }
    }
    if (dep == null && create) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_REPO_ERROR, Level.SEVERE);
    }
    return dep;
  }

  public List<PythonDep> listProject(Project proj) {
    List<PythonDep> libs = new ArrayList<>();
    Collection<PythonDep> objs = proj.getPythonDepCollection();
    if (objs != null) {
      libs.addAll(objs);
    }
    return libs;
  }

  public List<OpStatus> getFailedCondaOpsProject(Project proj) {
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

  public void retryFailedCondaOpsProject(Project proj) {
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
   * @return
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public Map<String, String> getPreInstalledLibs() throws ServiceException, ProjectException {

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
          throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_LIST_LIB_FORMAT_ERROR, Level.WARNING);
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
        throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_LIST_LIB_ERROR, Level.SEVERE,
            "errCode: " + errCode);
      } else if (errCode == 1) {
        throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_CONDA_LIBS_NOT_FOUND, Level.SEVERE,
            "errCode: " + errCode);
      }

    } catch (IOException | InterruptedException ex) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_LIST_LIB_ERROR, Level.SEVERE,
          "Could not get pre-installed conda libraries", ex.getMessage(), ex);

    }

    return depVers;
  }

  public void removePythonDepsForProject(Project proj) {
    Collection<PythonDep> deps = new ArrayList();
    proj.setPythonDepCollection(deps);
    projectFacade.update(proj);
  }

  public void addPythonDepsForProject(Project proj, Collection<PythonDep> pythonDeps) {
    proj.setPythonDepCollection(pythonDeps);
    projectFacade.update(proj);
  }

  private void removePythonForProject(Project proj) {
    proj.setPythonDepCollection(new ArrayList<PythonDep>());
    proj.setPythonVersion("");
    proj.setConda(false);
    projectFacade.update(proj);
  }

  public void deleteCommandsForProject(Project proj) {
    List<CondaCommands> commands = getCommandsForProject(proj);
    if (commands == null) {
      return;
    }
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
   */
  public void removeProject(Project proj) throws ServiceException {

    this.jupyterProcessMgr.stopProject(proj);
    deleteCommandsForProject(proj);
    if (proj.getCondaEnv()) {
      condaEnvironmentRemove(proj);
      setCondaEnv(proj, false);
    }
    removePythonForProject(proj);
  }

  /**
   * @param srcProject
   */
  public void cloneProject(Project srcProject, Project destProj) throws ServiceException {
    condaEnvironmentClone(srcProject, destProj);
  }
  /**
   * Asynchronous execution of conda operations
   *
   * @param op
   * @param proj
   * @param pythonVersion
   * @param arg
   */
  private void condaEnvironmentOp(CondaOp op, String pythonVersion, Project proj,
      String arg, MachineType machineType, String environmentYml) throws ServiceException {

    if (projectUtils.isReservedProjectName(proj.getName())) {
      throw new IllegalStateException("Tried to execute a conda env op on a reserved project name");
    }
    List<Hosts> hosts = validateCondaHosts(machineType);
    for (Hosts h : hosts) {
      // For environment operations, we don't care about the Conda Channel, so we just pick 'defaults'
      CondaCommands cc = new CondaCommands(h, settings.getAnacondaUser(),
          op, CondaStatus.NEW, CondaInstallType.ENVIRONMENT, machineType, proj, pythonVersion, "",
          "defaults", new Date(), arg, environmentYml);
      em.persist(cc);
    }
  }

  private void condaEnvironmentRemove(Project proj) throws ServiceException {
    condaEnvironmentOp(CondaOp.REMOVE, "", proj, "", MachineType.ALL, null);
  }

  private void condaEnvironmentClone(Project srcProj, Project destProj) throws ServiceException {
    condaEnvironmentOp(CondaOp.CLONE, "", srcProj, destProj.getName(), MachineType.ALL, null);
  }

  public CondaCommands getOngoingEnvCreation(Project proj) {
    List<CondaCommands> commands = getCommandsForProject(proj);
    for (CondaCommands command : commands) {
      if ((command.getOp().equals(CondaOp.YML) || command.getOp().equals(CondaOp.CREATE)) && (command.getStatus().
          equals(CondaStatus.NEW) || command.getStatus().equals(CondaStatus.ONGOING))) {
        return command;
      }
    }
    return null;
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
   */
  public void blockingCondaEnvironmentOp(CondaOp op, String proj, String arg, List<Hosts> hosts) {
    List<Future> waiters = new ArrayList<>();
    for (Hosts h : hosts) {
      LOGGER.log(Level.INFO, "Create anaconda enviornment for {0} on {1}",
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

  public List<OpStatus> opStatus(Project proj) {
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

  private void checkForOngoingEnvOp(Project proj) throws ServiceException {
    List<OpStatus> ongoingOps = opStatus(proj);
    for (OpStatus os : ongoingOps) {
      if (CondaOp.isEnvOp(CondaOp.valueOf(os.getOp().toUpperCase()))) {
        throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_OP_IN_PROGRESS, Level.INFO);
      }
    }
  }

  public void addLibrary(Project proj, CondaInstallType installType, MachineType machineType,
      String channelUrl, String dependency, String version) throws ServiceException, GenericException {
    condaOp(CondaOp.INSTALL, installType, machineType, proj, channelUrl, dependency, version);
  }

  public void upgradeLibrary(Project proj, CondaInstallType installType, MachineType machineType, String channelUrl,
      String dependency,
      String version) throws ServiceException, GenericException {
    condaOp(CondaOp.UPGRADE, installType, machineType, proj, channelUrl, dependency, version);
  }

  public void clearCondaOps(Project proj, String dependency) {
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
      String version) throws ServiceException, GenericException {
    condaOp(CondaOp.UNINSTALL, installType, machineType, proj, channelUrl, dependency, version);
  }

  private void condaOp(CondaOp op, CondaInstallType installType, MachineType machineType, Project proj,
      String channelUrl, String lib, String version) throws ServiceException, GenericException {

    List<Hosts> hosts = hostsFacade.getCondaHosts(machineType);
    if (hosts.size() == 0) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.HOST_TYPE_NOT_FOUND, Level.INFO,
          "capability:" + machineType.name());
    }

    try {
      // 1. test if anacondaRepoUrl exists. If not, add it.
      AnacondaRepo repo = getRepo(channelUrl, true);
      // 2. Test if pythonDep exists. If not, add it.
      PythonDep dep = getDep(repo, machineType, installType, lib, version, false, false, CondaStatus.SUCCESS);

      // 3. Add the python library to the join table for the project
      Collection<PythonDep> depsInProj = proj.getPythonDepCollection();
      if (depsInProj.contains(dep)) {
        if (op == CondaOp.INSTALL) {
          throw new ProjectException(RESTCodes.ProjectErrorCode.PYTHON_LIB_ALREADY_INSTALLED, Level.FINE,
              "dep: " + dep.getDependency());
        }
        depsInProj.remove(dep);
      }
      if (op == CondaOp.INSTALL || op == CondaOp.UPGRADE) {
        depsInProj.add(dep);
      }
      /*proj.setPythonDepCollection(depsInProj);
      em.merge(proj);
      // This flush keeps the transaction state alive - don't want it to timeout
      em.flush();*/

      for (Hosts h : hosts) {
        CondaCommands cc = new CondaCommands(h, settings.getAnacondaUser(),
            op, CondaStatus.NEW, installType, machineType, proj, lib,
            version, channelUrl, new Date(), "", null);
        em.persist(cc);
      }
      //      kagentCalls(hosts, op, proj, dep);
    } catch (Exception ex) {
      throw new GenericException(RESTCodes.GenericErrorCode.UNKNOWN_ERROR, Level.SEVERE, "condaOp failed",
          ex.getMessage(), ex);
    }
  }

  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void blockingCondaOp(int hostId, CondaOp op, CondaInstallType condaInstallType,
      MachineType machineType, Project proj, String channelUrl, String lib, String version) throws ServiceException {
    Hosts host = em.find(Hosts.class, hostId);

    AnacondaRepo repo = getRepo(channelUrl, false);
    PythonDep dep = getDep(repo, machineType, condaInstallType, lib, version, false, false, CondaStatus.SUCCESS);
    Future<?> f = kagentExecutorService.submit(new PythonDepsFacade.CondaTask(
        this.web, proj, host, op, dep));
    try {
      f.get(100, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException ex) {
      Logger.getLogger(PythonDepsFacade.class.getName()).
          log(Level.SEVERE, null, ex);
    }
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
      LOGGER.log(Level.FINE, "Could not remove CondaCommand with id: {0}",
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
      String channel) throws ServiceException {
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
            PythonDep dep = getDep(getRepo(cc.getChannelUrl(), false), cc.getMachineType(),
                cc.getInstallType(), cc.getLib(), cc.getVersion(), true, false,
                condaStatus);
            Collection<PythonDep> deps = cc.getProjectId().getPythonDepCollection();
            if (opType.equals(CondaOp.INSTALL)) {
              deps.add(dep);
            } else if (opType.equals(CondaOp.UNINSTALL)) {
              deps.remove(dep);
            }
            cc.getProjectId().setPythonDepCollection(deps);
            projectFacade.update(cc.getProjectId());
          }
        }
      } else {
        cc.setStatus(condaStatus);
        cc.setArg(arg);
        em.merge(cc);
      }
    } else {
      LOGGER.log(Level.FINE, "Could not remove CondaCommand with id: {0}",
          commandId);
    }
  }

  public void cleanupConda() throws ServiceException {
    List<Project> projects = projectFacade.findAll();
    if (projects != null && !projects.isEmpty()) {
      Project project = projects.get(0);
      condaEnvironmentOp(CondaOp.CLEAN, "", project, "", MachineType.ALL, "");
    }
  }

}
