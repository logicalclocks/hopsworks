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

package io.hops.hopsworks.api.agent;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.pythonDeps.PythonDepsService;
import io.hops.hopsworks.common.dao.alert.Alert;
import io.hops.hopsworks.common.dao.alert.AlertEJB;
import io.hops.hopsworks.common.dao.command.KagentCommands;
import io.hops.hopsworks.common.dao.command.SystemCommand;
import io.hops.hopsworks.common.dao.command.SystemCommandFacade;
import io.hops.hopsworks.common.dao.host.Health;


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import io.hops.hopsworks.common.dao.host.Hosts;
import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.common.dao.kagent.HostServices;
import io.hops.hopsworks.common.dao.kagent.HostServicesFacade;
import io.hops.hopsworks.common.dao.host.Status;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.pythonDeps.BlockReport;
import io.hops.hopsworks.common.dao.pythonDeps.CondaCommands;
import io.hops.hopsworks.common.dao.pythonDeps.PythonDep;
import io.hops.hopsworks.common.dao.pythonDeps.PythonDepsFacade;
import io.hops.hopsworks.common.dao.pythonDeps.PythonDepsFacade.CondaOp;
import io.hops.hopsworks.common.dao.pythonDeps.PythonDepsFacade.CondaStatus;
import io.hops.hopsworks.common.dao.pythonDeps.AnacondaRepo;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountsEmailMessages;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.util.EmailBean;
import io.hops.hopsworks.common.util.Settings;
import io.swagger.annotations.Api;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.mail.MessagingException;
import javax.ws.rs.POST;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;

@Path("/agentresource")
@Stateless
@RolesAllowed({"HOPS_ADMIN", "AGENT"})
@Api(value = "Agent Service",
    description = "Agent Service")
public class AgentResource {

  @EJB
  private HostsFacade hostFacade;
  @EJB
  private HostServicesFacade hostServiceFacade;
  @EJB
  private AlertEJB alertFacade;
  @EJB
  private PythonDepsFacade pythonDepsFacade;
  @EJB
  private ProjectFacade projFacade;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private Settings settings;
  @EJB
  private EmailBean emailBean;
  @EJB
  private SystemCommandFacade systemCommandFacade;

  final static Logger logger = Logger.getLogger(AgentResource.class.getName());

  public class CondaCommandsComparator implements Comparator<CondaCommands> {

    @Override
    public int compare(CondaCommands c1, CondaCommands c2) {
      if (c1.getId() > c2.getId()) {
        return 1;
      } else if (c1.getId() < c2.getId()) {
        return -1;
      } else {
        return 0;
      }
    }
  }

  @GET
  @Path("ping")
  @Produces(MediaType.TEXT_PLAIN)
  public String ping() {
    return "Kmon: Pong";
  }

  @POST
  @Path("/heartbeat")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response heartbeat(@Context SecurityContext sc,
      @Context HttpServletRequest req,
      @Context HttpHeaders httpHeaders, String jsonHb) {
    // Commands are sent back to the kagent as a response to this heartbeat.
    // Kagent then executes the commands received in order.
    List<CondaCommands> commands = new ArrayList<>();
    List<SystemCommand> systemCommands = new ArrayList<>();

    try {

      InputStream stream = new ByteArrayInputStream(jsonHb.getBytes(
          StandardCharsets.UTF_8));
      JsonObject json = Json.createReader(stream).readObject();
      long agentTime = json.getJsonNumber("agent-time").longValue();
      String hostname = json.getString("host-id");
      Hosts host = hostFacade.findByHostname(hostname);
      if (host == null) {
        logger.log(Level.WARNING, "Host with id {0} not found.", hostname);
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      if (!host.isRegistered()) {
        logger.log(Level.WARNING, "Host with id {0} is not registered.", hostname);
        return Response.status(Response.Status.NOT_ACCEPTABLE).build();
      }
      host.setLastHeartbeat((new Date()).getTime());
      host.setLoad1(json.getJsonNumber("load1").doubleValue());
      host.setLoad5(json.getJsonNumber("load5").doubleValue());
      host.setLoad15(json.getJsonNumber("load15").doubleValue());
      Integer numGpus = json.getJsonNumber("num-gpus").intValue();
      host.setNumGpus( numGpus);  // '1' means has a GPU, '0' means doesn't have one.
      Long previousDiskUsed = host.getDiskUsed() == null ? 0l : host.getDiskUsed();
      host.setDiskUsed(json.getJsonNumber("disk-used").longValue());
      host.setMemoryUsed(json.getJsonNumber("memory-used").longValue());
      host.setPrivateIp(json.getString("private-ip"));
      host.setDiskCapacity(json.getJsonNumber("disk-capacity").longValue());
      if (((float) previousDiskUsed) / host.getDiskCapacity() < 0.8 && ((float) host.getDiskUsed()) / host.
          getDiskCapacity() > 0.8) {
        String subject = "alert: hard drive full on " + host.getHostname();
        String body = host.getHostname() + " hard drive utilisation is " + host.getDiskUsageInfo();
        emailAlert(subject, body);
      }
      host.setMemoryCapacity(json.getJsonNumber("memory-capacity").longValue());
      host.setCores(json.getInt("cores"));
      hostFacade.storeHost(host, false);

      JsonArray roles = json.getJsonArray("services");
      for (int i = 0; i < roles.size(); i++) {
        JsonObject s = roles.getJsonObject(i);

        if (!s.containsKey("cluster") || !s.containsKey("group") || !s.
            containsKey("service")) {
          logger.warning("Badly formed JSON object describing a service.");
          continue;
        }
        String cluster = s.getString("cluster");
        String serviceName = s.getString("service");
        String group = s.getString("group");
        HostServices hostService = null;
        try {
          hostService = hostServiceFacade.find(hostname, cluster, group, serviceName);
        } catch (Exception ex) {
          logger.log(Level.FINE, "Could not find a service for the kagent heartbeat.");
          continue;
        }

        if (hostService == null) {
          hostService = new HostServices();
          hostService.setHost(host);
          hostService.setCluster(cluster);
          hostService.setGroup(group);
          hostService.setService(serviceName);
          hostService.setStartTime(agentTime);
        }

        String webPort = s.containsKey("web-port") ? s.getString("web-port")
            : "0";
        String pid = s.containsKey("pid") ? s.getString("pid") : "-1";
        try {
//          role.setWebPort(Integer.parseInt(webPort));
          hostService.setPid(Integer.parseInt(pid));
        } catch (NumberFormatException ex) {
          logger.log(Level.WARNING, "Invalid webport or pid - not a number for: {0}", hostService);
          continue;
        }
        Health previousHealthOfService = hostService.getHealth();
        if (s.containsKey("status")) {
          if ((hostService.getStatus() == null || !hostService.getStatus().equals(Status.Started)) && Status.valueOf(s.
              getString(
                  "status")).equals(Status.Started)) {
            hostService.setStartTime(agentTime);
          }
          hostService.setStatus(Status.valueOf(s.getString("status")));
        } else {
          hostService.setStatus(Status.None);
        }

        Long startTime = hostService.getStartTime();
        Status status = Status.valueOf(s.getString("status"));
        if (status.equals(Status.Started)) {
          hostService.setStopTime(agentTime);
        }
        Long stopTime = hostService.getStopTime();

        if (startTime != null && stopTime != null) {
          hostService.setUptime(stopTime - startTime);
        } else {
          hostService.setUptime(0);
        }
        hostServiceFacade.store(hostService);
        if (!hostService.getHealth().equals(previousHealthOfService) && hostService.getHealth().equals(Health.Bad)) {
          String subject = "alert: " + hostService.getGroup() + "." + hostService.getService() + "@" + hostService.
              getHost().getHostname();
          String body = hostService.getGroup() + "." + hostService.getService() + "@" + hostService.getHost().
              getHostname() + " transitioned from state " + previousHealthOfService + " to " + hostService.getHealth();
          emailAlert(subject, body);
        }

      }

      if (json.containsKey("commands-reply")) {
        JsonObject commandsReply = json.getJsonObject("commands-reply");
        JsonArray condaOps = commandsReply.getJsonArray("condaCommands");
        JsonArray systemOps = commandsReply.getJsonArray("systemCommands");
        
        processCondaCommands(condaOps);
        processSystemCommands(systemOps);
      }

      List<CondaCommands> differenceList = new ArrayList<>();

      if (json.containsKey("block-report")) {
        // Map<'project', 'installed-libs'>
        Map<String, BlockReport> mapReports = new HashMap<>();

        JsonObject envs = json.getJsonObject("block-report");
        for (String s : envs.keySet()) {
          JsonArray installedLibs = envs.getJsonArray(s);

          String projName = s;
          BlockReport br = new BlockReport();
          mapReports.put(projName, br);
          br.setProject(projName);
          for (int k = 0; k < installedLibs.size(); k++) {
            JsonObject libObj = installedLibs.getJsonObject(k);
            String libName = libObj.getString("name");
            String libUrl = libObj.getString("channel");
            String libInstallType = libObj.getString("installtype");
            String libVersion = libObj.getString("version");
            br.addLib(libName, libUrl, libInstallType, libVersion);
          }
        }
      }

      Collection<CondaCommands> allCommands = host.
          getCondaCommandsCollection();

      Collection<CondaCommands> commandsToExec = new ArrayList<>();
      for (CondaCommands cc : allCommands) {
        if (cc.getStatus() != PythonDepsFacade.CondaStatus.FAILED) {
          commandsToExec.add(cc);
          cc.setHostId(host);
        }
      }
      commands.addAll(commandsToExec);
      commands.addAll(differenceList);
      
      List<SystemCommand> pendingCommands = systemCommandFacade.findByHost(host);
      for (SystemCommand pendingCommand : pendingCommands) {
        if (pendingCommand.getStatus().equals(SystemCommandFacade.STATUS.ONGOING)) {
          systemCommands.add(pendingCommand);
        }
      }
      
    } catch (Exception ex) {
      logger.log(Level.SEVERE, ex.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }

    Collections.sort(commands, new CondaCommandsComparator());
    Collections.sort(systemCommands, new Comparator<SystemCommand>() {
      @Override
      public int compare(SystemCommand command0, SystemCommand command1) {
        if (command0.getId() > command1.getId()) {
          return 1;
        } else if (command0.getId() < command1.getId()) {
          return -1;
        }
        return 0;
      }
    });
    
    KagentCommands kagentCommands = new KagentCommands(systemCommands, commands);
    
    GenericEntity<KagentCommands> kcs = new GenericEntity<KagentCommands>(kagentCommands) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        kcs).build();
  }

  private void processSystemCommands(JsonArray systemOps) {
    for (int i = 0; i < systemOps.size(); ++i) {
      JsonObject jsonCommand = systemOps.getJsonObject(i);
      Integer id = jsonCommand.getInt("id");
      String opStr = jsonCommand.getString("op");
      String statusStr = jsonCommand.getString("status");
      
      SystemCommandFacade.OP op = SystemCommandFacade.OP.valueOf(opStr.toUpperCase());
      SystemCommandFacade.STATUS status = SystemCommandFacade.STATUS.valueOf(statusStr.toUpperCase());
      
      SystemCommand systemCommand = systemCommandFacade.findById(id);
      if (systemCommand == null) {
        throw new IllegalArgumentException("System command with ID: " + id + " is not in the system");
      }
      
      if (op.equals(SystemCommandFacade.OP.SERVICE_KEY_ROTATION)) {
        processServiceKeyRotationCommand(systemCommand, status);
      }
    }
  }
  
  private void processServiceKeyRotationCommand(SystemCommand command, SystemCommandFacade.STATUS status) {
    if (status.equals(SystemCommandFacade.STATUS.FINISHED)) {
      systemCommandFacade.delete(command);
    } else if (status.equals(SystemCommandFacade.STATUS.FAILED)) {
      command.setStatus(SystemCommandFacade.STATUS.FAILED);
      systemCommandFacade.update(command);
    }
  }
  
  private void processCondaCommands(JsonArray condaOps) throws AppException {
    for (int j = 0; j < condaOps.size(); j++) {
      JsonObject entry = condaOps.getJsonObject(j);
    
      String projName = entry.getString("proj");
      String op = entry.getString("op");
      PythonDepsFacade.CondaOp opType = PythonDepsFacade.CondaOp.valueOf(op.toUpperCase());
      String channelurl = entry.getString("channelurl");
      String lib = entry.containsKey("lib") ? entry.getString("lib") : "";
      String version = entry.containsKey("version") ? entry.getString("version") : "";
      String arg = entry.containsKey("arg") ? entry.getString("arg") : "";
      String status = entry.getString("status");
      PythonDepsFacade.CondaStatus agentStatus = PythonDepsFacade.CondaStatus.valueOf(status.toUpperCase());
      int commmandId = Integer.parseInt(entry.getString("id"));
    
      CondaCommands command = pythonDepsFacade.
          findCondaCommand(commmandId);
      // If the command object does not exist, then the project
      // has probably been removed. We needed to send a compensating action if
      // this action was successful.
      if (command != null) {
        if (agentStatus == PythonDepsFacade.CondaStatus.SUCCESS) {
          // remove command from the DB
          pythonDepsFacade.
              updateCondaCommandStatus(commmandId, agentStatus, command.getInstallType(),
                  command.getMachineType(), arg, projName, opType, lib, version, channelurl);
        } else {
          pythonDepsFacade.
              updateCondaCommandStatus(commmandId, agentStatus, command.getInstallType(),
                  command.getMachineType(), arg, projName, opType, lib, version, channelurl);
        }
      }
    
    
      //sync local libs as the ones installed
      if(command.getOp().equals(CondaOp.CREATE)) {
      
        //only sync on hopsworks server
        if(settings.getHopsworksIp().equals(command.getHostId().getHostIp())) {
        
          Project projectId = command.getProjectId();
        
          String envStr = listCondaEnvironment(projName);
        
          Collection<PythonDep> pythonDeps = synchronizeDependencies(projectId,
              envStr, projectId.getPythonDepCollection());
        
          //Remove existing deps
          pythonDepsFacade.removePythonDepsForProject(projectId);
        
          //Insert all deps in current listing
          pythonDepsFacade.addPythonDepsForProject(projectId, pythonDeps);
        }
      
      }
    
      //an upgrade results in an unknown version installed, query local conda env to figure it out
      if(command.getOp().equals(CondaOp.UPGRADE)) {
      
        command.setVersion(getLocalLibraryVersion(command.getLib(), command.getVersion(), projName));
      
        if(settings.getHopsworksIp().equals(command.getHostId().getHostIp())) {
          Project projectId = command.getProjectId();
        
          Collection<PythonDep> pythonDeps = projectId.getPythonDepCollection();
          for(PythonDep pythonDep: pythonDeps) {
            if(pythonDep.getDependency().equals(command.getLib()) &&
                pythonDep.getVersion().equals(command.getVersion())) {
            
              String localVersion = getLocalLibraryVersion(command.getLib(), command.getVersion(), projName);
            
              if(!localVersion.equals(command.getVersion())) {
                Collection<PythonDep> deps = projectId.getPythonDepCollection();
              
                for(PythonDep dep: deps) {
                  if(dep.getDependency().equals(command.getLib())) {
                    PythonDep newDep = pythonDepsFacade.getDep(dep.getRepoUrl(), dep.getMachineType(),
                        command.getInstallType(), command.getLib(), localVersion, true, false);
                    deps.remove(dep);
                    deps.add(newDep);
                    projFacade.update(projectId);
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
  
  private void emailAlert(String subject, String body) {
    try {
      emailBean.sendEmails(settings.getAlertEmailAddrs(), subject, body);
    } catch (MessagingException ex) {
      logger.log(Level.SEVERE, ex.getMessage());
    }
  }

  @POST
  @Path("/alert")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response alert(@Context SecurityContext sc,
      @Context HttpServletRequest req,
      @Context HttpHeaders httpHeaders, String jsonString
  ) {
    // TODO: Alerts are stored in the database. Later, we should define reactions (Email, SMS, ...).
    Alert alert = new Alert();
    try {
      InputStream stream = new ByteArrayInputStream(jsonString.getBytes(StandardCharsets.UTF_8));
      JsonObject json = Json.createReader(stream).readObject();
      alert.setAlertTime(new Date());
      alert.setProvider(Alert.Provider.valueOf(json.getString("Provider")).toString());
      alert.setSeverity(Alert.Severity.valueOf(json.getString("Severity")).toString());
      alert.setAgentTime(json.getJsonNumber("Time").bigIntegerValue());
      alert.setMessage(json.getString("Message"));
      String hostname = json.getString("host-id");
      Hosts h = hostFacade.findByHostname(hostname);
      alert.setHost(h);
      alert.setPlugin(json.getString("Plugin"));
      if (json.containsKey("PluginInstance")) {
        alert.setPluginInstance(json.getString("PluginInstance"));
      }
      if (json.containsKey("Type")) {
        alert.setType(json.getString("Type"));
      }
      if (json.containsKey("TypeInstance")) {
        alert.setTypeInstance(json.getString("TypeInstance"));
      }
      if (json.containsKey("DataSource")) {
        alert.setDataSource(json.getString("DataSource"));
      }
      if (json.containsKey("CurrentValue")) {
        alert.setCurrentValue(Boolean.toString(json.getBoolean("CurrentValue")));
      }
      if (json.containsKey("WarningMin")) {
        alert.setWarningMin(json.getString("WarningMin"));
      }
      if (json.containsKey("WarningMax")) {
        alert.setWarningMax(json.getString("WarningMax"));
      }
      if (json.containsKey("FailureMin")) {
        alert.setFailureMin(json.getString("FailureMin"));
      }
      if (json.containsKey("FailureMax")) {
        alert.setFailureMax(json.getString("FailureMax"));
      }
      alertFacade.persistAlert(alert);

    } catch (Exception ex) {
      logger.log(Level.SEVERE, "Exception: {0}", ex);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }

    if (!settings.getAlertEmailAddrs().isEmpty()) {
      try {
        emailBean.sendEmails(settings.getAlertEmailAddrs(), UserAccountsEmailMessages.ALERT_SERVICE_DOWN, alert.
            toString());
      } catch (MessagingException ex) {
        Logger.getLogger(AgentResource.class.getName()).log(Level.SEVERE, null, ex);
      }
    }

    return Response.ok().build();
  }



  //SAMPLE OUTPUT
  /*
  # packages in environment at /srv/hops/anaconda/anaconda-2-5.0.1/envs/demo_tensorflow_admin000:
  #
  # Name                    Version                   Build  Channel
  absl-py                   0.1.10                    <pip>
  backports-abc             0.5                       <pip>
  backports.shutil-get-terminal-size 1.0.0                     <pip>
  backports.weakref         1.0.post1                 <pip>
  bleach                    2.1.2                     <pip>
  ca-certificates           2017.08.26           h1d4fec5_0
  certifi                   2018.1.18                py27_0

   */

  /**
   * List installed libraries in the anaconda environment for the project
   * @param project
   * @return
   */
  private String listCondaEnvironment(String project) {

    String prog = settings.getHopsworksDomainDir() + "/bin/list_environment.sh";
    ProcessBuilder pb = new ProcessBuilder(prog, project);
    StringBuilder sb = new StringBuilder();
    try {
      Process process = pb.start();
      BufferedReader br = new BufferedReader(new InputStreamReader(
              process.getInputStream(), Charset.forName("UTF8")));
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line + System.getProperty("line.separator"));
      }
      process.waitFor();
    } catch (IOException | InterruptedException ex) {
      logger.log(Level.SEVERE, "Problem listing conda environment: {0}", ex.
              toString());
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
   * @param project
   * @param condaListStr
   * @param currentlyInstalledPyDeps
   * @return
   * @throws AppException
   */
  private Collection<PythonDep> synchronizeDependencies(Project project, String condaListStr,
                                  Collection<PythonDep> currentlyInstalledPyDeps) throws AppException {

    Collection<PythonDep> deps = new ArrayList();

    String[] lines = condaListStr.split(System.getProperty("line.separator"));

    for(int i = 3; i < lines.length; i++) {

      String line = lines[i];

      String [] split = line.split(" +");

      String libraryName = split[0];
      String version = split[1];

      if(PythonDepsService.preInstalledLibraryNames.contains(libraryName)) {
        AnacondaRepo repo = pythonDepsFacade.getRepo(project, "PyPi",true);

        //Special case for tensorflow
        if(libraryName.equals("tensorflow")) {
          PythonDep tensorflowCPU = pythonDepsFacade.getDep(repo, PythonDepsFacade.MachineType.CPU,
                  PythonDepsFacade.CondaInstallType.PIP, libraryName, version, true, true);
          tensorflowCPU.setStatus(CondaStatus.SUCCESS);
          deps.add(tensorflowCPU);
          PythonDep tensorflowGPU = pythonDepsFacade.getDep(repo, PythonDepsFacade.MachineType.GPU,
                  PythonDepsFacade.CondaInstallType.PIP, libraryName + "-gpu", version, true, true);
          tensorflowGPU.setStatus(CondaStatus.SUCCESS);
          deps.add(tensorflowGPU);
          continue;
        }

        PythonDep pyDep = pythonDepsFacade.getDep(repo, PythonDepsFacade.MachineType.ALL,
                PythonDepsFacade.CondaInstallType.PIP, libraryName, version, true, true);
        pyDep.setStatus(CondaStatus.SUCCESS);
        deps.add(pyDep);
        continue;
      }

      if(PythonDepsService.providedLibraryNames.contains(libraryName)) {
        AnacondaRepo repo = pythonDepsFacade.getRepo(project, "PyPi",true);
        PythonDep pyDep = pythonDepsFacade.getDep(repo, PythonDepsFacade.MachineType.ALL,
                PythonDepsFacade.CondaInstallType.PIP, libraryName, version, true, false);
        pyDep.setStatus(CondaStatus.SUCCESS);
        deps.add(pyDep);
      } else {
        for(PythonDep pyDep: currentlyInstalledPyDeps) {
          if(libraryName.equals(pyDep.getDependency())) {
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

    for(int i = 3; i < lines.length; i++) {

      String line = lines[i];

      String [] split = line.split(" +");
      String localLib = split[0];

      if(localLib.equals(library)) {
        return split[1];
      }
    }
    return currentVersion;
  }
}
