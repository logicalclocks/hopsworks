/*
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
 */

package io.hops.hopsworks.common.agent;

import io.hops.hopsworks.common.admin.services.HostServicesController;
import io.hops.hopsworks.common.dao.command.HeartbeatReplyDTO;
import io.hops.hopsworks.common.dao.command.SystemCommandFacade;
import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.common.dao.kagent.HostServicesFacade;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.python.CondaCommandFacade;
import io.hops.hopsworks.common.dao.python.LibraryFacade;
import io.hops.hopsworks.common.hosts.HostsController;
import io.hops.hopsworks.common.python.commands.CommandsController;
import io.hops.hopsworks.common.python.environment.EnvironmentController;
import io.hops.hopsworks.common.python.library.LibraryController;
import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.command.CommandStatus;
import io.hops.hopsworks.persistence.entity.command.SystemCommand;
import io.hops.hopsworks.persistence.entity.host.Hosts;
import io.hops.hopsworks.persistence.entity.host.ServiceStatus;
import io.hops.hopsworks.persistence.entity.python.CondaCommands;
import io.hops.hopsworks.persistence.entity.python.MachineType;
import io.hops.hopsworks.persistence.entity.python.PythonDep;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class AgentController {
  private static final Logger LOG = Logger.getLogger(AgentController.class.getName());
  private static final Comparator ASC_COMPARATOR = new CommandsComparator();

  @EJB
  private HostsFacade hostsFacade;
  @EJB
  private Settings settings;
  @EJB
  private HostServicesFacade hostServicesFacade;
  @EJB
  private CondaCommandFacade condaCommandFacade;
  @EJB
  private CommandsController commandsController;
  @EJB
  private LibraryFacade libraryFacade;
  @EJB
  private LibraryController libraryController;
  @EJB
  private EnvironmentController environmentController;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private SystemCommandFacade systemCommandFacade;
  @EJB
  private OSProcessExecutor osProcessExecutor;
  @EJB
  private AgentLivenessMonitor agentLivenessMonitor;
  @EJB
  private HostsController hostsController;
  @EJB
  private HostServicesController hostServicesController;


  public void register(String hostId, String password) throws ServiceException {
    Hosts host = hostsController.findByHostname(hostId);
    host.setAgentPassword(password);
    host.setRegistered(true);
    host.setHostname(hostId);
    // Jim: We set the hostname as hopsworks::default pre-populates with the hostname,
    // but it's not the correct hostname for GCE.
    hostsFacade.update(host);
  }

  public HeartbeatReplyDTO heartbeat(AgentHeartbeatDTO heartbeat) throws ServiceException {
    Hosts host = hostsController.findByHostname(heartbeat.hostId);
    if (!host.getRegistered()) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.HOST_NOT_REGISTERED, Level.WARNING,
          "hostId: " + heartbeat.hostId);
    }

    agentLivenessMonitor.alive(host);
    updateHostMetrics(host, heartbeat);
    updateServices(heartbeat);
    processSystemCommands(heartbeat);

    if (heartbeat.recover != null && heartbeat.recover) {
      recoverUnfinishedCommands(host);
    }

    final HeartbeatReplyDTO response = new HeartbeatReplyDTO();
    addNewCommandsToResponse(host, response);
    return response;
  }

  private void recoverUnfinishedCommands(final Hosts host) {
    recoverSystemCommands(host);
  }

  private void recoverSystemCommands(Hosts host) {
    final List<SystemCommand> allUnfinished = systemCommandFacade.findUnfinishedByHost(host);
    for (SystemCommand command : allUnfinished) {
      command.setCommandStatus(CommandStatus.NEW);
      systemCommandFacade.update(command);
    }
  }

  private void addNewCommandsToResponse(final Hosts host, final HeartbeatReplyDTO response) {
    final List<SystemCommand> newSystemCommands = new ArrayList<>();
    final List<SystemCommand> allSystemCommands = systemCommandFacade.findByHost(host);
    for (final SystemCommand sc : allSystemCommands) {
      if (sc.getCommandStatus().equals(CommandStatus.NEW)) {
        newSystemCommands.add(sc);
      }
    }

    newSystemCommands.sort(ASC_COMPARATOR);
    response.setSystemCommands(newSystemCommands);
  }

  private void updateHostMetrics(final Hosts host, final AgentHeartbeatDTO heartbeat) throws ServiceException {
    host.setLastHeartbeat(new Date().getTime());
    host.setNumGpus(heartbeat.numGpus);
    host.setPrivateIp(heartbeat.privateIp);
    host.setCores(heartbeat.cores);
    host.setMemoryCapacity(heartbeat.memoryCapacity);
    hostsFacade.update(host);
  }

  private void updateServices(AgentHeartbeatDTO heartbeat) throws ServiceException {
    hostServicesController.updateHostServices(heartbeat);
  }

  //since we only want to show certain predefined libs or those user have installed we need to be selective about
  //which python deps should be put in the database
  //check that library is part of preinstalled libs OR in provided library list, only then add it

  /**
   * For each library in the conda environment figure out if it should be marked as unmutable.
   *
   * @param condaListStr
   * @param pyDepsInDB
   * @return
   */
  public Collection<PythonDep> persistAndMarkUnmutable(Collection<PythonDep> pyDepsInImage)
      throws ServiceException {
    Collection<PythonDep> deps = new ArrayList();

    for (PythonDep dep: pyDepsInImage) {

      String libraryName = dep.getDependency();

      if (settings.getUnmutablePythonLibraryNames().contains(libraryName)) {
        PythonDep pyDep = libraryFacade.getOrCreateDep(dep.getRepoUrl(), MachineType.ALL,
          dep.getInstallType(), libraryName, dep.getVersion(), true, true, dep.getBaseEnv());
        deps.add(pyDep);
      } else {
        PythonDep pyDep = libraryFacade.getOrCreateDep(dep);
        deps.add(pyDep);
      }
    }
    return deps;
  }


  private void processSystemCommands(AgentHeartbeatDTO heartbeat) {
    if (heartbeat.systemCommands == null) {
      return;
    }
    for (final SystemCommand sc : heartbeat.systemCommands) {
      final Integer id = sc.getId();
      final CommandStatus status = sc.getCommandStatus();
      final SystemCommand systemCommand = systemCommandFacade.findById(id);
      if (systemCommand == null) {
        throw new IllegalArgumentException("System command with ID: " + id + " is not in the system");
      }
      genericProcessSystemCommand(systemCommand, status);
    }
  }

  private void genericProcessSystemCommand(final SystemCommand command, final CommandStatus commandStatus) {
    if (commandStatus.equals(CommandStatus.FINISHED)) {
      systemCommandFacade.delete(command);
    } else {
      command.setCommandStatus(commandStatus);
      systemCommandFacade.update(command);
    }
  }

  public static class AgentHeartbeatDTO {
    private final String hostId;
    private final Long agentTime;
    private final Integer numGpus;
    private final Long memoryCapacity;
    private final Integer cores;
    private final String privateIp;
    private final List<AgentServiceDTO> services;
    private final List<SystemCommand> systemCommands;
    private final List<CondaCommands> condaCommands;
    private final List<String> condaReport;
    private final Boolean recover;

    public AgentHeartbeatDTO(final String hostId, final Long agentTime, final Integer numGpus,
                             final Long memoryCapacity, final Integer cores, final String privateIp,
                             final List<AgentServiceDTO> services, final List<SystemCommand> systemCommands,
                             final List<CondaCommands> condaCommands, final List<String> condaReport, Boolean recover) {
      this.hostId = hostId;
      this.agentTime = agentTime;
      this.numGpus = numGpus;
      this.memoryCapacity = memoryCapacity;
      this.cores = cores;
      this.privateIp = privateIp;
      this.services = services;
      this.systemCommands = systemCommands;
      this.condaCommands = condaCommands;
      this.condaReport = condaReport;
      this.recover = recover;
    }

    public String getHostId() {
      return hostId;
    }

    public Long getAgentTime() {
      return agentTime;
    }

    public Integer getNumGpus() {
      return numGpus;
    }

    public Long getMemoryCapacity() {
      return memoryCapacity;
    }

    public Integer getCores() {
      return cores;
    }

    public String getPrivateIp() {
      return privateIp;
    }

    public List<AgentServiceDTO> getServices() {
      return services;
    }

    public List<SystemCommand> getSystemCommands() {
      return systemCommands;
    }

    public List<CondaCommands> getCondaCommands() {
      return condaCommands;
    }

    public Boolean getRecover() {
      return recover;
    }
  }

  public static class AgentServiceDTO {
    private final String name;
    private final String group;
    private final Integer pid;
    private final ServiceStatus status;

    public AgentServiceDTO(final String name, final String group,
                           final Integer pid, final ServiceStatus status) {
      this.name = name;
      this.group = group;
      this.pid = pid;
      this.status = status;
    }

    public String getName() {
      return name;
    }

    public String getGroup() {
      return group;
    }

    public Integer getPid() {
      return pid;
    }

    public ServiceStatus getStatus() {
      return status;
    }
  }

  private static class CommandsComparator<T> implements Comparator<T> {

    @Override
    public int compare(T t, T t1) {

      if ((t instanceof CondaCommands) && (t1 instanceof CondaCommands)) {
        return condaCommandCompare((CondaCommands) t, (CondaCommands) t1);
      } else if ((t instanceof SystemCommand) && (t1 instanceof SystemCommand)) {
        return systemCommandCompare((SystemCommand) t, (SystemCommand) t1);
      } else {
        return 0;
      }
    }

    private int condaCommandCompare(final CondaCommands t, final CondaCommands t1) {
      if (t.getId() > t1.getId()) {
        return 1;
      } else if (t.getId() < t1.getId()) {
        return -1;
      } else {
        return 0;
      }
    }

    private int systemCommandCompare(final SystemCommand t, final SystemCommand t1) {
      if (t.getId() > t1.getId()) {
        return 1;
      } else if (t.getId() < t1.getId()) {
        return -1;
      } else {
        return 0;
      }
    }
  }
}
