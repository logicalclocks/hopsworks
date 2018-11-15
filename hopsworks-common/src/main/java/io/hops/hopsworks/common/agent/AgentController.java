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
import io.hops.hopsworks.common.dao.alert.Alert;
import io.hops.hopsworks.common.dao.alert.AlertEJB;
import io.hops.hopsworks.common.dao.command.HeartbeatReplyDTO;
import io.hops.hopsworks.common.dao.command.SystemCommand;
import io.hops.hopsworks.common.dao.command.SystemCommandFacade;
import io.hops.hopsworks.common.dao.host.Health;
import io.hops.hopsworks.common.dao.host.Hosts;
import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.common.dao.host.Status;
import io.hops.hopsworks.common.dao.kagent.HostServices;
import io.hops.hopsworks.common.dao.kagent.HostServicesFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.pythonDeps.AnacondaRepo;
import io.hops.hopsworks.common.dao.pythonDeps.CondaCommands;
import io.hops.hopsworks.common.dao.pythonDeps.PythonDep;
import io.hops.hopsworks.common.dao.pythonDeps.PythonDepsFacade;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountsEmailMessages;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.exception.ServiceException;
import io.hops.hopsworks.common.util.EmailBean;
import io.hops.hopsworks.common.util.Settings;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.mail.MessagingException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
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
  private EmailBean emailBean;
  @EJB
  private Settings settings;
  @EJB
  private HostServicesFacade hostServicesFacade;
  @EJB
  private PythonDepsFacade pythonDepsFacade;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private SystemCommandFacade systemCommandFacade;
  @EJB
  private AlertEJB alertFacade;
  
  public String register(String hostId, String password) {
    Hosts host = hostsFacade.findByHostname(hostId);
    host.setAgentPassword(password);
    host.setRegistered(true);
    host.setHostname(hostId);
    // Jim: We set the hostname as hopsworks::default pre-populates with the hostname,
    // but it's not the correct hostname for GCE.
    hostsFacade.storeHost(host);
    return settings.getHadoopVersionedDir();
  }
  
  public HeartbeatReplyDTO heartbeat(AgentHeartbeatDTO heartbeat) throws ServiceException {
    Hosts host = hostsFacade.findByHostname(heartbeat.hostId);
    if (host == null) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.HOST_NOT_FOUND, Level.WARNING,
        "hostId: " + heartbeat.hostId);
    }
    if (!host.isRegistered()) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.HOST_NOT_REGISTERED, Level.WARNING,
        "hostId: " + heartbeat.hostId);
    }
    
    updateHostMetrics(host, heartbeat);
    updateServices(heartbeat);
    processCondaCommands(heartbeat);
    processSystemCommands(heartbeat);
    if (heartbeat.condaReport != null && !heartbeat.condaReport.isEmpty()) {
      issueCondaEnvsGCCommand(host, heartbeat.condaReport);
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
    SystemCommand gcCommand = new SystemCommand(host, SystemCommandFacade.OP.CONDA_GC);
    gcCommand.setPriority(Integer.MIN_VALUE);
    gcCommand.setExecUser(settings.getAnacondaUser());
    gcCommand.setCommandArgumentsAsString(projectNamesStr);
    systemCommandFacade.persist(gcCommand);
  }
  
  public void alert(Alert alert, String hostId) throws ServiceException {
    Hosts host = hostsFacade.findByHostname(hostId);
    alert.setHost(host);
    alertFacade.persistAlert(alert);
    if (!settings.getAlertEmailAddrs().isEmpty()) {
      emailAlert(UserAccountsEmailMessages.ALERT_SERVICE_DOWN, alert.toString());
    }
  }
  
  private void addNewCommandsToResponse(final Hosts host, final HeartbeatReplyDTO response) {
    final List<CondaCommands> newCondaCommands = new ArrayList<>();
    final List<CondaCommands> allCondaCommands = pythonDepsFacade.findByHost(host);
    for (final CondaCommands cc : allCondaCommands) {
      if (cc.getStatus().equals(PythonDepsFacade.CondaStatus.NEW)) {
        newCondaCommands.add(cc);
        cc.setHostId(host);
      }
    }
    
    final List<SystemCommand> newSystemCommands = new ArrayList<>();
    final List<SystemCommand> allSystemCommands = systemCommandFacade.findByHost(host);
    for (final SystemCommand sc : allSystemCommands) {
      if (sc.getStatus().equals(SystemCommandFacade.STATUS.NEW)) {
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
    host.setLoad1(heartbeat.load1);
    host.setLoad5(heartbeat.load5);
    host.setLoad15(heartbeat.load15);
    host.setNumGpus(heartbeat.numGpus);
    Long previousDiskUsed = host.getDiskUsed() == null ? 0L : host.getDiskUsed();
    host.setDiskUsed(heartbeat.diskUsed);
    host.setDiskCapacity(heartbeat.diskCapacity);
  
    if (((float) previousDiskUsed) / host.getDiskCapacity() < 0.8 && ((float) host.getDiskUsed()) / host.
        getDiskCapacity() > 0.8) {
      String subject = "alert: hard drive full on " + host.getHostname();
      String body = host.getHostname() + " hard drive utilisation is " + host.getDiskUsageInfo();
      emailAlert(subject, body);
    }
  
    host.setMemoryUsed(heartbeat.memoryUsed);
    host.setMemoryCapacity(heartbeat.memoryCapacity);
    host.setPrivateIp(heartbeat.privateIp);
    host.setCores(heartbeat.cores);
    hostsFacade.storeHost(host);
  }
  
  private void updateServices(AgentHeartbeatDTO heartbeat) throws ServiceException {
    List<HostServices> updatedHostServices = hostServicesFacade.updateHostServices(heartbeat);
    for (HostServices updatedHostService : updatedHostServices) {
      notifyHostServiceHealth(updatedHostService);
    }
  }
  
  private void notifyHostServiceHealth(HostServices hostService) throws ServiceException {
    final Health previousHealthReport = hostService.getHealth();
    if (!hostService.getHealth().equals(previousHealthReport)
        && hostService.getHealth().equals(Health.Bad)) {
      final String subject = "alert: " + hostService.getGroup() + "." + hostService.getService() + "@" + hostService.
          getHost().getHostname();
      final String body = hostService.getGroup() + "." + hostService.getService() + "@" + hostService.getHost().
          getHostname() + " transitioned from state " + previousHealthReport + " to " + hostService.getHealth();
      emailAlert(subject, body);
    }
  }
  
  private void processCondaCommands(AgentHeartbeatDTO heartbeatDTO) throws ServiceException {
    if (heartbeatDTO.condaCommands == null) {
      return;
    }
    for (CondaCommands cc : heartbeatDTO.condaCommands) {
      final String projectName = cc.getProj();
      final PythonDepsFacade.CondaOp opType = cc.getOp();
      final String channelUrl = cc.getChannelUrl();
      final String lib = cc.getLib() != null ? cc.getLib() : "";
      final String version = cc.getVersion() != null ? cc.getVersion() : "";
      final String args = cc.getArg() != null ? cc.getArg() : "";
      final PythonDepsFacade.CondaStatus status = cc.getStatus();
      Integer commandId = cc.getId();
      
      CondaCommands command = pythonDepsFacade.findCondaCommand(commandId);
      // If the command object does not exist, then the project
      // has probably been removed. We needed to send a compensating action if
      // this action was successful.
  
      // Command would be null when we are deleting a Project and kagent reports that the
      // REMOVE operation has changed state from ONGOING to SUCCESS
      if (command != null) {
        pythonDepsFacade.updateCondaCommandStatus(
          commandId, status, command.getInstallType(), command.getMachineType(),
          args, projectName, opType, lib, version, channelUrl);
        
        if ((command.getOp().equals(PythonDepsFacade.CondaOp.CREATE)
              || command.getOp().equals(PythonDepsFacade.CondaOp.YML))
            && (status.equals(PythonDepsFacade.CondaStatus.SUCCESS)
              || status.equals(PythonDepsFacade.CondaStatus.FAILED))) {
          // Sync only on Hopsworks server
          if (settings.getHopsworksIp().equals(command.getHostId().getHostIp())) {
            final Project projectId = command.getProjectId();
            final String envStr = listCondaEnvironment(projectName);
            final Collection<PythonDep> pythonDeps = synchronizeDependencies(
                projectId, envStr, projectId.getPythonDepCollection(), status);
            // Insert all deps in current listing
            pythonDepsFacade.addPythonDepsForProject(projectId, pythonDeps);
          }
        }
        
        // An upgrade results in an unknown version installed, query local conda
        // env to figure it out
        if (command.getOp().equals(PythonDepsFacade.CondaOp.UPGRADE)) {
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
                      PythonDep newDep = pythonDepsFacade.getDep(dep.getRepoUrl(), dep.getMachineType(),
                          command.getInstallType(), command.getLib(), localVersion, true, false,
                          PythonDepsFacade.CondaStatus.FAILED);
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
   * @param project
   * @return
   */
  public String listCondaEnvironment(String project) {
    final String prog = settings.getHopsworksDomainDir() + "/bin/list_environment.sh";
    final ProcessBuilder pb = new ProcessBuilder(prog, project);
    final StringBuilder sb = new StringBuilder();
    try {
      final Process process = pb.start();
      final BufferedReader br = new BufferedReader(new InputStreamReader(
          process.getInputStream(), Charset.forName("UTF8")));
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line + System.getProperty("line.separator"));
      }
      process.waitFor();
    } catch (IOException | InterruptedException ex) {
      LOG.log(Level.SEVERE, "Problem listing conda environment: {0}",
          ex.toString());
    }
    return sb.toString();
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
  public Collection<PythonDep> synchronizeDependencies(Project project, String condaListStr,
      Collection<PythonDep> currentlyInstalledPyDeps, PythonDepsFacade.CondaStatus status) throws ServiceException {
    
    Collection<PythonDep> deps = new ArrayList();
    
    String[] lines = condaListStr.split(System.getProperty("line.separator"));
    
    for (int i = 3; i < lines.length; i++) {
      
      String line = lines[i];
      
      String[] split = line.split(" +");
      
      String libraryName = split[0];
      String version = split[1];
      
      if (settings.getPreinstalledPythonLibraryNames().contains(libraryName)) {
        AnacondaRepo repo = pythonDepsFacade.getRepo("PyPi", true);
        
        //Special case for tensorflow
        if (libraryName.equals("tensorflow")) {
          PythonDep tensorflowCPU = pythonDepsFacade.getDep(repo, PythonDepsFacade.MachineType.CPU,
              PythonDepsFacade.CondaInstallType.PIP, libraryName, version, true, true, status);
          deps.add(tensorflowCPU);
          PythonDep tensorflowGPU = pythonDepsFacade.getDep(repo, PythonDepsFacade.MachineType.GPU,
              PythonDepsFacade.CondaInstallType.PIP, libraryName + "-gpu", version, true, true, status);
          deps.add(tensorflowGPU);
          continue;
        }
        
        PythonDep pyDep = pythonDepsFacade.getDep(repo, PythonDepsFacade.MachineType.ALL,
            PythonDepsFacade.CondaInstallType.PIP, libraryName, version, true, true, status);
        deps.add(pyDep);
        continue;
      }
      
      if (settings.getProvidedPythonLibraryNames().contains(libraryName)) {
        AnacondaRepo repo = pythonDepsFacade.getRepo("PyPi", true);
        PythonDep pyDep = pythonDepsFacade.getDep(repo, PythonDepsFacade.MachineType.ALL,
            PythonDepsFacade.CondaInstallType.PIP, libraryName, version, true, false, status);
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
      final SystemCommandFacade.OP op = sc.getOp();
      final SystemCommandFacade.STATUS status = sc.getStatus();
      final SystemCommand systemCommand = systemCommandFacade.findById(id);
      if (systemCommand == null) {
        throw new IllegalArgumentException("System command with ID: " + id + " is not in the system");
      }
      genericProcessSystemCommand(systemCommand, status);
    }
  }
  
  private void genericProcessSystemCommand(final SystemCommand command, final SystemCommandFacade.STATUS status) {
    if (status.equals(SystemCommandFacade.STATUS.FINISHED)) {
      systemCommandFacade.delete(command);
    } else {
      command.setStatus(status);
      systemCommandFacade.update(command);
    }
  }
  
  private void emailAlert(String subject, String body) throws ServiceException {
    try {
      emailBean.sendEmails(settings.getAlertEmailAddrs(), subject, body);
    } catch (MessagingException e) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.EMAIL_SENDING_FAILURE, Level.SEVERE, null, e.getMessage(),
        e);
    }
  }
  
  public static class AgentHeartbeatDTO {
    private final String hostId;
    private final Long agentTime;
    private final Double load1;
    private final Double load5;
    private final Double load15;
    private final Integer numGpus;
    private final Long diskUsed;
    private final Long diskCapacity;
    private final Long memoryUsed;
    private final Long memoryCapacity;
    private final Integer cores;
    private final String privateIp;
    private final List<AgentServiceDTO> services;
    private final List<SystemCommand> systemCommands;
    private final List<CondaCommands> condaCommands;
    private final List<String> condaReport;
    
    public AgentHeartbeatDTO(final String hostId, final Long agentTime, final Double load1, final Double load5,
        final Double load15, final Integer numGpus, final Long diskUsed, final Long diskCapacity,
        final Long memoryUsed, final Long memoryCapacity, final Integer cores, final String privateIp,
        final List<AgentServiceDTO> services, final List<SystemCommand> systemCommands,
        final List<CondaCommands> condaCommands, final List<String> condaReport) {
      this.hostId = hostId;
      this.agentTime = agentTime;
      this.load1 = load1;
      this.load5 = load5;
      this.load15 = load15;
      this.numGpus = numGpus;
      this.diskUsed = diskUsed;
      this.diskCapacity = diskCapacity;
      this.memoryUsed = memoryUsed;
      this.memoryCapacity = memoryCapacity;
      this.cores = cores;
      this.privateIp = privateIp;
      this.services = services;
      this.systemCommands = systemCommands;
      this.condaCommands = condaCommands;
      this.condaReport = condaReport;
    }
  
    public String getHostId() {
      return hostId;
    }
  
    public Long getAgentTime() {
      return agentTime;
    }
  
    public Double getLoad1() {
      return load1;
    }
  
    public Double getLoad5() {
      return load5;
    }
  
    public Double getLoad15() {
      return load15;
    }
  
    public Integer getNumGpus() {
      return numGpus;
    }
  
    public Long getDiskUsed() {
      return diskUsed;
    }
  
    public Long getDiskCapacity() {
      return diskCapacity;
    }
  
    public Long getMemoryUsed() {
      return memoryUsed;
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
  }
  
  public static class AgentServiceDTO {
    private final String cluster;
    private final String service;
    private final String group;
    private final String webPort;
    private final Integer pid;
    private final Status status;
    
    public AgentServiceDTO(final String cluster, final String service, final String group, final String webPort,
        final Integer pid, final Status status) {
      this.cluster = cluster;
      this.service = service;
      this.group = group;
      this.webPort = webPort;
      this.pid = pid;
      this.status = status;
    }
  
    public String getCluster() {
      return cluster;
    }
  
    public String getService() {
      return service;
    }
  
    public String getGroup() {
      return group;
    }
  
    public String getWebPort() {
      return webPort;
    }
  
    public Integer getPid() {
      return pid;
    }
  
    public Status getStatus() {
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
