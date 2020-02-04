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

import com.google.gson.Gson;
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
import io.hops.hopsworks.common.util.ProcessDescriptor;
import io.hops.hopsworks.common.util.ProcessResult;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.command.CommandStatus;
import io.hops.hopsworks.persistence.entity.command.Operation;
import io.hops.hopsworks.persistence.entity.command.SystemCommand;
import io.hops.hopsworks.persistence.entity.host.Hosts;
import io.hops.hopsworks.persistence.entity.host.ServiceStatus;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.python.AnacondaRepo;
import io.hops.hopsworks.persistence.entity.python.CondaCommands;
import io.hops.hopsworks.persistence.entity.python.CondaInstallType;
import io.hops.hopsworks.persistence.entity.python.CondaOp;
import io.hops.hopsworks.persistence.entity.python.CondaStatus;
import io.hops.hopsworks.persistence.entity.python.MachineType;
import io.hops.hopsworks.persistence.entity.python.PythonDep;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
    processCondaCommands(heartbeat);
    processSystemCommands(heartbeat);
    if (heartbeat.condaReport != null && !heartbeat.condaReport.isEmpty()) {
      issueCondaEnvsGCCommand(host, heartbeat.condaReport);
    }

    if (heartbeat.recover != null && heartbeat.recover) {
      recoverUnfinishedCommands(host);
    }

    final HeartbeatReplyDTO response = new HeartbeatReplyDTO();
    addNewCommandsToResponse(host, response);
    return response;
  }

  private void issueCondaEnvsGCCommand(final Hosts host, final List<String> envsToCheck) {
    List<String> envsToDelete = envsToCheck.stream()
        .filter(p -> {
          Project project = projectFacade.findByName(p);
          // Project does not exist any longer
          // OR Project does not have a CoW (CopyOnWrite) environment
          // OR does not have Conda enabled at all (really for safety reasons)
          return project == null || !project.getCondaEnv() || !project.getConda();
        }).collect(Collectors.toList());

    String projectNamesStr = new Gson().toJson(envsToDelete);
    SystemCommand gcCommand = new SystemCommand(host, Operation.CONDA_GC);
    gcCommand.setPriority(Integer.MIN_VALUE);
    gcCommand.setExecUser(settings.getAnacondaUser());
    gcCommand.setCommandArgumentsAsString(projectNamesStr);
    systemCommandFacade.persist(gcCommand);
  }

  private void recoverUnfinishedCommands(final Hosts host) {
    recoverCondaCommands(host);
    recoverSystemCommands(host);
  }

  private void recoverCondaCommands(Hosts host) {
    final List<CondaCommands> allUnfinished = condaCommandFacade.findUnfinishedByHost(host);
    for (CondaCommands command : allUnfinished) {
      try {
        commandsController
            .updateCondaCommandStatus(command.getId(), CondaStatus.NEW, command.getArg());
      } catch (ServiceException ex) {
        LOG.log(Level.WARNING, "Could not recover command with ID: " + command.getId() + " for host " + host);
      }
    }
  }

  private void recoverSystemCommands(Hosts host) {
    final List<SystemCommand> allUnfinished = systemCommandFacade.findUnfinishedByHost(host);
    for (SystemCommand command : allUnfinished) {
      command.setCommandStatus(CommandStatus.NEW);
      systemCommandFacade.update(command);
    }
  }

  private void addNewCommandsToResponse(final Hosts host, final HeartbeatReplyDTO response) {
    final List<CondaCommands> newCondaCommands = new ArrayList<>();
    final List<CondaCommands> allCondaCommands = condaCommandFacade.findByHost(host);
    for (final CondaCommands cc : allCondaCommands) {
      if (cc.getStatus().equals(CondaStatus.NEW)) {
        newCondaCommands.add(cc);
        cc.setHostId(host);
      }
    }

    final List<SystemCommand> newSystemCommands = new ArrayList<>();
    final List<SystemCommand> allSystemCommands = systemCommandFacade.findByHost(host);
    for (final SystemCommand sc : allSystemCommands) {
      if (sc.getCommandStatus().equals(CommandStatus.NEW)) {
        newSystemCommands.add(sc);
      }
    }

    newCondaCommands.sort(ASC_COMPARATOR);
    newSystemCommands.sort(ASC_COMPARATOR);
    response.setCondaCommands(newCondaCommands);
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

  private void processCondaCommands(AgentHeartbeatDTO heartbeatDTO) throws ServiceException {
    if (heartbeatDTO.condaCommands == null) {
      return;
    }
    for (CondaCommands cc : heartbeatDTO.condaCommands) {
      final String projectName = cc.getProj();
      final CondaOp opType = cc.getOp();
      final String channelUrl = cc.getChannelUrl();
      final String lib = cc.getLib() != null ? cc.getLib() : "";
      final String version = cc.getVersion() != null ? cc.getVersion() : "";
      final String args = cc.getArg() != null ? cc.getArg() : "";
      final CondaStatus status = cc.getStatus();
      Integer commandId = cc.getId();

      CondaCommands command = condaCommandFacade.findCondaCommand(commandId);
      // If the command object does not exist, then the project
      // has probably been removed. We needed to send a compensating action if
      // this action was successful.

      // Command would be null when we are deleting a Project and kagent reports that the
      // REMOVE operation has changed state from ONGOING to SUCCESS
      if (command != null) {
        commandsController.updateCondaCommandStatus(
          commandId, status, command.getInstallType(), command.getMachineType(),
          args, projectName, command.getUserId(), opType, lib, version, channelUrl);
        
        if (command.getOp().equals(CondaOp.YML) &&
            settings.getHopsworksIp().equals(command.getHostId().getHostIp()) &&
            (status.equals(CondaStatus.SUCCESS) ||
                status.equals(CondaStatus.FAILED))) {
          // Sync only on Hopsworks server
          final Project projectId = command.getProjectId();
          final String envStr = listCondaEnvironment(projectName);
          final Collection<PythonDep> pythonDeps = synchronizeDependencies(envStr,
              projectId.getPythonDepCollection());
          // Insert all deps in current listing
          libraryController.addPythonDepsForProject(projectId, pythonDeps);
        }

        if (command.getOp().equals(CondaOp.EXPORT) &&
            status.equals(CondaStatus.SUCCESS)) {
          environmentController.uploadYmlInProject(command.getProjectId(), command.getUserId(),
              cc.getEnvironmentYml(), command.getArg() );
        }

        // An upgrade results in an unknown version installed, query local conda
        // env to figure it out
        if (command.getOp().equals(CondaOp.UPGRADE)) {
          command.setVersion(getLocalLibraryVersion(command.getLib(),
              command.getVersion(), projectName));
          if (settings.getHopsworksIp().equals(command.getHostId().getHostIp())) {
            final Project projectId = command.getProjectId();
            for (final PythonDep pythonDep : projectId.getPythonDepCollection()) {
              if (pythonDep.getDependency().equals(command.getLib())
                  && pythonDep.getVersion().equals(command.getVersion())) {
                final String localVersion = getLocalLibraryVersion(command.getLib(),
                    command.getVersion(), projectName);
                if (!localVersion.equals(command.getVersion())) {
                  final Collection<PythonDep> deps = projectId.getPythonDepCollection();

                  for (final PythonDep dep : deps) {
                    if (dep.getDependency().equals(command.getLib())) {
                      PythonDep newDep = libraryFacade.getOrCreateDep(dep.getRepoUrl(), dep.getMachineType(),
                          command.getInstallType(), command.getLib(), localVersion, true, false);
                      deps.remove(dep);
                      deps.add(newDep);
                      projectFacade.update(projectId);
                      break;
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  //SAMPLE OUTPUT
  /*
   * # packages in environment at /srv/hops/anaconda/anaconda-2-5.0.1/envs/demo_tensorflow_admin000:
   * #
   * # Name Version Build Channel
   * absl-py 0.1.10 <pip>
   * backports-abc 0.5 <pip>
   * backports.shutil-get-terminal-size 1.0.0 <pip>
   * backports.weakref 1.0.post1 <pip>
   * bleach 2.1.2 <pip>
   * ca-certificates 2017.08.26 h1d4fec5_0
   * certifi 2018.1.18 py27_0
   *
   */

  /**
   * List installed libraries in the anaconda environment for the project
   *
   * @param project
   * @return
   */
  public String listCondaEnvironment(String project) {
    final String prog = settings.getHopsworksDomainDir() + "/bin/list_environment.sh";

    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand(prog)
        .addCommand(project)
        .build();

    try {
      ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
      if (processResult.processExited()) {
        return processResult.getStdout();
      }
    } catch (IOException ex) {
      LOG.log(Level.SEVERE, "Problem listing conda environment: {0}",
          ex.toString());
    }
    return "";
  }

  //since we only want to show certain predefined libs or those user have installed we need to be selective about
  //which python deps should be put in the database
  //check that library is part of preinstalled libs OR in provided library list, only then add it

  /**
   * For each locally installed library in the conda environment on the hopsworks server, figure out the version
   * if it is listed as a preinstalled or provided library. A preinstalled libary can't be modified once it has been
   * installed, whereas a provided can.
   *
   * @param condaListStr
   * @param currentlyInstalledPyDeps
   * @return
   */
  public Collection<PythonDep> synchronizeDependencies(String condaListStr,
                                                       Collection<PythonDep> currentlyInstalledPyDeps)
      throws ServiceException {
    Collection<PythonDep> deps = new ArrayList();

    String[] lines = condaListStr.split(System.getProperty("line.separator"));

    for (int i = 3; i < lines.length; i++) {

      String line = lines[i];

      String[] split = line.split(" +");

      String libraryName = split[0];
      String version = split[1];

      if (settings.getPreinstalledPythonLibraryNames().contains(libraryName)) {
        AnacondaRepo repo = libraryFacade.getRepo("PyPi", true);

        PythonDep pyDep = libraryFacade.getOrCreateDep(repo, MachineType.ALL,
          CondaInstallType.PIP, libraryName, version, true, true);
        deps.add(pyDep);
        continue;
      }

      if (settings.getProvidedPythonLibraryNames().contains(libraryName)) {
        AnacondaRepo repo = libraryFacade.getRepo("PyPi", true);
        PythonDep pyDep = libraryFacade.getOrCreateDep(repo, MachineType.ALL,
          CondaInstallType.PIP, libraryName, version, true, false);
        deps.add(pyDep);
      } else {
        for (PythonDep pyDep : currentlyInstalledPyDeps) {
          if (libraryName.equals(pyDep.getDependency())) {
            pyDep.setVersion(split[1]);
            deps.add(pyDep);
          }
        }
      }
    }
    return deps;
  }

  /**
   * Get the version of a library installed on the same server as hopsworks
   *
   * @param library
   * @param currentVersion
   * @param projName
   * @return
   */
  private String getLocalLibraryVersion(String library, String currentVersion, String projName) {
    String condaListStr = listCondaEnvironment(projName);

    String[] lines = condaListStr.split(System.getProperty("line.separator"));

    for (int i = 3; i < lines.length; i++) {

      String line = lines[i];

      String[] split = line.split(" +");
      String localLib = split[0];

      if (localLib.equals(library)) {
        return split[1];
      }
    }
    return currentVersion;
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
