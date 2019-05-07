/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
 */
package io.hops.hopsworks.common.python.commands;

import io.hops.hopsworks.common.dao.host.Hosts;
import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.python.AnacondaRepo;
import io.hops.hopsworks.common.dao.python.CondaCommandFacade;
import io.hops.hopsworks.common.dao.python.CondaCommands;
import io.hops.hopsworks.common.dao.python.LibraryFacade;
import io.hops.hopsworks.common.dao.python.PythonDep;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.util.WebCommunication;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.concurrent.ManagedExecutorService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class CommandsController {
  
  private static final Logger LOGGER = Logger.getLogger(CommandsController.class.getName());
  @EJB
  private CondaCommandFacade condaCommandFacade;
  @EJB
  private Settings settings;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private HostsFacade hostsFacade;
  @EJB
  private WebCommunication web;
  @EJB
  private LibraryFacade libraryFacade;
  
  @Resource(lookup = "concurrent/kagentExecutorService")
  ManagedExecutorService kagentExecutorService;
  
  public void deleteCommands(Project project, String lib) {
    condaCommandFacade.deleteCommandsForLibrary(project, lib);
  }
  
  public void deleteCommands(Project project) {
    condaCommandFacade.deleteCommandsForEnvironment(project);
  }
  
  public List<CondaCommands> retryFailedCondaOps(Project project) {
    List<CondaCommands> commands = condaCommandFacade.getFailedCommandsForProject(project);
    for (CondaCommands cc : commands) {
      cc.setStatus(CondaCommandFacade.CondaStatus.NEW);
      condaCommandFacade.update(cc);
    }
    return commands;
  }
  
  public List<CondaCommands> retryFailedCondaOps(Project project, String library) {
    List<CondaCommands> commands = condaCommandFacade.getFailedCommandsForProjectAndLib(project, library);
    for (CondaCommands cc : commands) {
      cc.setStatus(CondaCommandFacade.CondaStatus.NEW);
      condaCommandFacade.update(cc);
    }
    return commands;
  }
  
  public void deleteCommandsForProject(Project proj) {
    List<CondaCommands> commands = condaCommandFacade.getCommandsForProject(proj);
    if (commands == null) {
      return;
    }
    for (CondaCommands cc : commands) {
      // First, remove any old commands for the project in conda_commands
      condaCommandFacade.remove(cc);
    }
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
  public void blockingCondaEnvironmentOp(CondaCommandFacade.CondaOp op, String proj, String arg, List<Hosts> hosts) {
    List<Future> waiters = new ArrayList<>();
    for (Hosts h : hosts) {
      LOGGER.log(Level.INFO, "Create anaconda enviornment for {0} on {1}", new Object[]{proj, h.getHostIp()});
      Future<?> f = kagentExecutorService.submit(new AnacondaTask(this.web, proj, h, op, arg));
      waiters.add(f);
    }
    for (Future f : waiters) {
      try {
        f.get(10, TimeUnit.SECONDS);
      } catch (InterruptedException | ExecutionException | TimeoutException ex) {
        LOGGER.log(Level.SEVERE, null, ex);
      }
    }
    
  }
  
  public void blockingCondaOp(int hostId, CondaCommandFacade.CondaOp op,
    CondaCommandFacade.CondaInstallType condaInstallType,
    LibraryFacade.MachineType machineType, Project proj, String channelUrl,
    String lib, String version) throws ServiceException {
    Hosts host = hostsFacade.find(hostId);
    
    AnacondaRepo repo = libraryFacade.getRepo(channelUrl, false);
    PythonDep dep = libraryFacade.getDep(repo, machineType, condaInstallType, lib, version, false,
      false, CondaCommandFacade.CondaStatus.SUCCESS);
    Future<?> f = kagentExecutorService.submit(new CondaTask(this.web, proj, host, op, dep));
    try {
      f.get(100, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
  }
  
  public PythonDep condaOp(CondaCommandFacade.CondaOp op, CondaCommandFacade.CondaInstallType installType,
    LibraryFacade.MachineType machineType, Project proj, String channelUrl, String lib, String version)
    throws ServiceException, GenericException {
    
    List<Hosts> hosts = hostsFacade.getCondaHosts(machineType);
    if (hosts.size() == 0) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.HOST_TYPE_NOT_FOUND, Level.INFO,
        "capability:" + machineType.name());
    }
    PythonDep dep;
    try {
      // 1. test if anacondaRepoUrl exists. If not, add it.
      AnacondaRepo repo = libraryFacade.getRepo(channelUrl, true);
      // 2. Test if pythonDep exists. If not, add it.
      dep = libraryFacade.getDep(repo, machineType, installType, lib, version, true, false,
        CondaCommandFacade.CondaStatus.ONGOING);
      
      // 3. Add the python library to the join table for the project
      Collection<PythonDep> depsInProj = proj.getPythonDepCollection();
      if (depsInProj.contains(dep) && op == CondaCommandFacade.CondaOp.INSTALL) {
        throw new ProjectException(RESTCodes.ProjectErrorCode.PYTHON_LIB_ALREADY_INSTALLED, Level.FINE,
          "dep: " + dep.getDependency());
      }
      if (op == CondaCommandFacade.CondaOp.INSTALL || op == CondaCommandFacade.CondaOp.UPGRADE) {
        depsInProj.remove(dep);// if upgrade
        depsInProj.add(dep);
      }
      proj.setPythonDepCollection(depsInProj);
      projectFacade.update(proj);
  
      for (Hosts h : hosts) {
        CondaCommands cc = new CondaCommands(h, settings.getAnacondaUser(), op, CondaCommandFacade.CondaStatus.NEW,
          installType, machineType, proj, lib, version, channelUrl, new Date(), "", null, false);
        condaCommandFacade.save(cc);
      }
    } catch (Exception ex) {
      throw new GenericException(RESTCodes.GenericErrorCode.UNKNOWN_ERROR, Level.SEVERE, "condaOp failed",
        ex.getMessage(), ex);
    }
    return dep;
  }
  
  public void updateCondaCommandStatus(Integer commandID, CondaCommandFacade.CondaStatus status, String arguments)
    throws ServiceException {
    updateCondaCommandStatus(commandID, status,
      null, null, arguments, null, null, null, null, null);
  }
  
  public void updateCondaCommandStatus(int commandId, CondaCommandFacade.CondaStatus condaStatus,
    CondaCommandFacade.CondaInstallType installType, LibraryFacade.MachineType machineType, String arg, String proj,
    CondaCommandFacade.CondaOp opType, String lib, String version, String channel) throws ServiceException {
    CondaCommands cc = condaCommandFacade.findCondaCommand(commandId);
    if (cc != null) {
      if (condaStatus == CondaCommandFacade.CondaStatus.SUCCESS) {
        // remove completed commands
        condaCommandFacade.remove(cc);
        // Check if this is the last operation for this project. If yes, set
        // the PythonDep to be installed or
        // the CondaEnv operation is finished implicitly (no condaOperations are
        // returned => CondaEnv operation is finished).
        if (!CondaCommandFacade.CondaOp.isEnvOp(opType)) {
          Project p = projectFacade.findByName(proj);
          Collection<CondaCommands> ongoingCommands = p.getCondaCommandsCollection();
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
            PythonDep dep = libraryFacade.getDep(libraryFacade.getRepo(cc.getChannelUrl(), false),
              cc.getMachineType(), cc.getInstallType(), cc.getLib(), cc.getVersion(), true, false,
              condaStatus);
            Collection<PythonDep> deps = cc.getProjectId().getPythonDepCollection();
            if (opType.equals(CondaCommandFacade.CondaOp.INSTALL)) {
              deps.remove(dep);
              deps.add(dep);
            } else if (opType.equals(CondaCommandFacade.CondaOp.UNINSTALL)) {
              deps.remove(dep);
            }
            cc.getProjectId().setPythonDepCollection(deps);
            projectFacade.update(cc.getProjectId());
          }
        }
      } else {
        if (CondaCommandFacade.CondaStatus.FAILED.equals(condaStatus)) {
          libraryFacade.getDep(libraryFacade.getRepo(cc.getChannelUrl(), false), cc.getMachineType(),
            cc.getInstallType(), cc.getLib(), cc.getVersion(), false, false, condaStatus);
        }
        cc.setStatus(condaStatus);
        cc.setArg(arg);
        condaCommandFacade.update(cc);
      }
    } else {
      LOGGER.log(Level.FINE, "Could not remove CondaCommand with id: {0}", commandId);
    }
  }
  
  public class AnacondaTask implements Runnable {
    
    private final WebCommunication web;
    private final String proj;
    private final Hosts host;
    private final CondaCommandFacade.CondaOp op;
    private final String arg;
    private Object entity;
    
    public AnacondaTask(WebCommunication web, String proj, Hosts host, CondaCommandFacade.CondaOp op, String arg) {
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
        Logger.getLogger(CondaCommandFacade.class.getName()).log(Level.SEVERE,
          null, ex);
      }
    }
    
    public Object getEntity() {
      return entity;
    }
    
  }
  
  public class CondaTask implements Runnable {
    
    private final WebCommunication web;
    private final Project proj;
    private final Hosts host;
    private final CondaCommandFacade.CondaOp op;
    private final PythonDep dep;
    private Object entity;
    
    public CondaTask(WebCommunication web, Project proj, Hosts host, CondaCommandFacade.CondaOp op,
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
        Logger.getLogger(CondaCommandFacade.class.getName()).log(Level.SEVERE,
          null, ex);
      }
    }
    
    public Object getEntity() {
      return entity;
    }
    
  }
}
