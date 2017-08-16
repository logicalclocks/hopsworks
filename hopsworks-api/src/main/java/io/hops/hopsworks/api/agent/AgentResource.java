package io.hops.hopsworks.api.agent;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.common.dao.alert.Alert;
import io.hops.hopsworks.common.dao.alert.AlertEJB;
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
import io.hops.hopsworks.common.dao.host.Host;
import io.hops.hopsworks.common.dao.host.HostEJB;
import io.hops.hopsworks.common.dao.role.Role;
import io.hops.hopsworks.common.dao.role.RoleEJB;
import io.hops.hopsworks.common.dao.host.Status;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.pythonDeps.BlockReport;
import io.hops.hopsworks.common.dao.pythonDeps.CondaCommands;
import io.hops.hopsworks.common.dao.pythonDeps.PythonDep;
import io.hops.hopsworks.common.dao.pythonDeps.PythonDepsFacade;
import io.hops.hopsworks.common.dao.pythonDeps.PythonDepsFacade.CondaOp;
import io.hops.hopsworks.common.dao.pythonDeps.PythonDepsFacade.CondaStatus;
import io.hops.hopsworks.common.util.Settings;
import io.swagger.annotations.Api;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
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
  private HostEJB hostFacade;
  @EJB
  private RoleEJB roleFacade;
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
  
  final static Logger logger = Logger.getLogger(AgentResource.class.getName());
  
  @GET
  @Path("ping")
  @Produces(MediaType.TEXT_PLAIN)
  public String ping() {
    return "Kmon: Pong";
  }

//    @GET
//    @Path("load/{name}")
//    @Produces(MediaType.APPLICATION_JSON)
//    public Response getLoadAvg(@PathParam("name") String name) {
//        JSONObject json = new JSONObject();
//        try {
//            Host host = hostEJB.findByHostname(name);
//            json.put("hostname", host.getHostname());
//            json.put("cores", host.getCores());
//            json.put("load1", host.getLoad1());
//            json.put("load5", host.getLoad5());
//            json.put("load15", host.getLoad15());
//        } catch (Exception ex) {
//            // TODO - Should log all exceptions          
//            logger.log(Level.SEVERE, "Exception: {0}", ex);
//            if (ex.getMessage().equals("NoResultException")) {
//                return Response.status(Response.Status.NOT_FOUND).build();
//            }
//            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
//        }
//        return Response.ok(json).build();
//    }
//    @GET
//    @Path("loads")
//    @Produces(MediaType.APPLICATION_JSON)
//    public Response getLoads() {
//        JSONArray jsonArray = new JSONArray();
//        List<Host> hosts = hostEJB.find();
//        for (Host host : hosts) {
//            try {
//                JSONObject json = new JSONObject();
//                json.put("hostname", host.getHostname());
//                json.put("cores", host.getCores());
//                json.put("load1", host.getLoad1());
//                json.put("load5", host.getLoad5());
//                json.put("load15", host.getLoad15());
//                jsonArray.put(json);
//            } catch (Exception ex) {
//                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
//            }
//        }
//        return Response.ok(jsonArray).build();
//    }
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
    
    try {
      
      InputStream stream = new ByteArrayInputStream(jsonHb.getBytes(
              StandardCharsets.UTF_8));
      JsonObject json = Json.createReader(stream).readObject();
      long agentTime = json.getJsonNumber("agent-time").longValue();
      String hostId = json.getString("host-id");
      Host host = hostFacade.findByHostId(hostId);
      if (host == null) {
        logger.log(Level.WARNING, "Host with id {0} not found.", hostId);
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      if (!host.isRegistered()) {
        logger.log(Level.WARNING, "Host with id {0} is not registered.", hostId);
        return Response.status(Response.Status.NOT_ACCEPTABLE).build();
      }
      host.setLastHeartbeat((new Date()).getTime());
      host.setLoad1(json.getJsonNumber("load1").doubleValue());
      host.setLoad5(json.getJsonNumber("load5").doubleValue());
      host.setLoad15(json.getJsonNumber("load15").doubleValue());
      host.setDiskUsed(json.getJsonNumber("disk-used").longValue());
      host.setMemoryUsed(json.getJsonNumber("memory-used").longValue());
      host.setPrivateIp(json.getString("private-ip"));
      host.setDiskCapacity(json.getJsonNumber("disk-capacity").longValue());
      host.setMemoryCapacity(json.getJsonNumber("memory-capacity").longValue());
      host.setCores(json.getInt("cores"));
      hostFacade.storeHost(host, false);
      
      JsonArray roles = json.getJsonArray("services");
      for (int i = 0; i < roles.size(); i++) {
        JsonObject s = roles.getJsonObject(i);
        
        if (!s.containsKey("cluster") || !s.containsKey("service") || !s.
                containsKey("role")) {
          logger.warning("Badly formed JSON object describing a service.");
          continue;
        }
        String cluster = s.getString("cluster");
        String roleName = s.getString("role");
        String service = s.getString("service");
        Role role = null;
        try {
          roleFacade.find(hostId, cluster, service, roleName);
        } catch (Exception ex) {
          logger.warning("Problem finding a role, transaction timing out? "
                  + ex.toString());
        }

        if (role == null) {
          role = new Role();
          role.setHostId(hostId);
          role.setCluster(cluster);
          role.setService(service);
          role.setRole(roleName);
          role.setStartTime(agentTime);
        }
        
        String webPort = s.containsKey("web-port") ? s.getString("web-port")
                : "0";
        String pid = s.containsKey("pid") ? s.getString("pid") : "-1";
        try {
          role.setWebPort(Integer.parseInt(webPort));
          role.setPid(Integer.parseInt(pid));
        } catch (NumberFormatException ex) {
          logger.log(Level.WARNING,
                  "Invalid webport or pid - not a number for: {0}", role);
          continue;
        }
        if (s.containsKey("status") && role.getStatus() != null) {
          if (!role.getStatus().equals(Status.Started) && Status.valueOf(s.
                  getString("status")).equals(Status.Started)) {
            role.setStartTime(agentTime);
          }
          role.setStatus(Status.valueOf(s.getString("status")));
        } else {
          role.setStatus(Status.None);
        }

        Long startTime = role.getStartTime();
        Status status = Status.valueOf(s.getString("status"));
        if (status.equals(Status.Started)) {
          role.setStopTime(agentTime);
        }
        Long stopTime = role.getStopTime();
        
        if ( startTime != null && stopTime != null) {
          role.setUptime(stopTime - startTime);
        } else {
          role.setUptime(0);          
        }
        
        roleFacade.store(role);
      }
      
      if (json.containsKey("conda-ops")) {
        JsonArray condaOps = json.getJsonArray("conda-ops");
        for (int j = 0; j < condaOps.size(); j++) {
          JsonObject entry = condaOps.getJsonObject(j);
          
          String projName = entry.getString("proj");
          String op = entry.getString("op");
          PythonDepsFacade.CondaOp opType = PythonDepsFacade.CondaOp.valueOf(
                  op.toUpperCase());
          String channelurl = entry.getString("channelurl");
          String lib = entry.containsKey("lib") ? entry.getString("lib") : "";
          String version = entry.containsKey("version") ? entry.getString(
                  "version") : "";
          String arg = entry.containsKey("arg") ? entry.getString("arg") : "";
          String status = entry.getString("status");
          PythonDepsFacade.CondaStatus agentStatus
                  = PythonDepsFacade.CondaStatus.valueOf(status.toUpperCase());
          int commmandId = Integer.parseInt(entry.getString("id"));
          
          CondaCommands command = pythonDepsFacade.
                  findCondaCommand(commmandId);
          // If the command object does not exist, then the project
          // has probably been removed. We needed to send a compensating action if
          // this action was successful.
          if (command != null) {
            if (agentStatus == PythonDepsFacade.CondaStatus.INSTALLED) {
              // remove command from the DB
              pythonDepsFacade.
                      updateCondaComamandStatus(commmandId, agentStatus, arg,
                              projName, opType, lib, version);
            } else {
              pythonDepsFacade.
                      updateCondaComamandStatus(commmandId, agentStatus, arg,
                              projName, opType, lib, version);
            }
          }
        }
      }
      
      List<CondaCommands> differenceList = new ArrayList<>();
      
      if (json.containsKey("block-report")) {
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
            String libVersion = libObj.getString("version");
            br.addLib(libName, libUrl, libVersion);
          }
        }

        // get all the projects and send them down and all the dependencies
        // for all the projects and send them down, too.
        List<Project> allProjs = projFacade.findAll();
        // For each project, verify all its libs are in the blockreport list
        // Any extra blocks reported need to be removed. Any missing need to
        // be added
        for (Project project : allProjs) {
          
          Collection<CondaCommands> allCcs = project.
                  getCondaCommandsCollection();
          logger.log(Level.INFO, "AnacondaReport: {0}", project.getName());
          
          if ((!mapReports.containsKey(project.getName()))
                  && (project.getName().compareToIgnoreCase(settings.
                          getAnacondaEnv())) != 0) {
            // project not a conda environment
            // check if a conda-command exists for creating the project and is valid.

            boolean noExistingCommandInDB = true;
            for (CondaCommands command : allCcs) {
              if (command.getOp() == CondaOp.CREATE && command.getProj().
                      compareTo(project.getName()) == 0) {
                noExistingCommandInDB = false; // command already exists
              }
            }
            if (noExistingCommandInDB) {
              CondaCommands cc = new CondaCommands(host, settings.
                      getSparkUser(), CondaOp.CREATE, CondaStatus.ONGOING,
                      project, "", "", "", null, "");
              // commandId == '-1' implies this is a block report command that
              // doesn't need to be acknowledged by the agent (no need to send as a
              // reponse a command-status). No need to persist this command to the DB either.
              cc.setId(-1);
              // Need to create env on node
              differenceList.add(cc);
            }
            
          } else { // This project exists as a conda env
            BlockReport br = mapReports.get(project.getName());
            for (PythonDep lib : project.getPythonDepCollection()) {
              BlockReport.Lib blockLib = br.getLib(lib.getDependency());
              if (blockLib == null || blockLib.compareTo(lib) != 0) {
                CondaCommands cc = new CondaCommands(host, settings.
                        getSparkUser(),
                        CondaOp.INSTALL, CondaStatus.ONGOING, project,
                        lib.getDependency(),
                        lib.getRepoUrl().getUrl(), lib.getVersion(),
                        Date.from(Instant.now()), "");
                cc.setId(-1);
                differenceList.add(cc);
              }
              // we mark the library as checked by deleting it from the incoming br
              if (blockLib != null) {
                br.removeLib(blockLib.getLib());
              }
            }
            // remove any extra libraries in the conda-env, not in the project
            // get removed from the conda env.
            for (BlockReport.Lib blockLib : br.getLibs()) {
              CondaCommands cc
                      = new CondaCommands(host, settings.getSparkUser(),
                              CondaOp.UNINSTALL, CondaStatus.ONGOING, project,
                              blockLib.getLib(),
                              blockLib.getChannelUrl(), blockLib.getVersion(),
                              null, "");
              cc.setId(-1);
              differenceList.add(cc);
            }
            mapReports.remove(project.getName());
          }
        }
        // All the conda environments that weren't in the project list, remove them.
        for (BlockReport br : mapReports.values()) {
          // Don't delete our default environment

          logger.log(Level.INFO, "BlockReport: {0} - {1}", new Object[]{br.
            getProject(), br.getLibs().size()});
          
          if (br.getProject().compareToIgnoreCase(settings.getAnacondaEnv())
                  == 0) {
            continue;
          }
          CondaCommands cc = new CondaCommands();
          cc.setId(-1);
          cc.setHostId(host);
          cc.setUser(settings.getSparkUser());
          cc.setProj(br.getProject());
          cc.setOp(PythonDepsFacade.CondaOp.REMOVE);
          differenceList.add(cc);
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
      
    } catch (Exception ex) {
      logger.log(Level.SEVERE, ex.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }

    GenericEntity<Collection<CondaCommands>> commandsForKagent
            = new GenericEntity<Collection<CondaCommands>>(commands) { };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            commandsForKagent).build();
  }
  
  @POST
  @Path("/alert")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response alert(@Context SecurityContext sc,
          @Context HttpServletRequest req,
          @Context HttpHeaders httpHeaders, String jsonString
  ) {
    // TODO: Alerts are stored in the database. Later, we should define reactions (Email, SMS, ...).
    try {
      InputStream stream = new ByteArrayInputStream(jsonString.getBytes(
              StandardCharsets.UTF_8));
      JsonObject json = Json.createReader(stream).readObject();
      Alert alert = new Alert();
      alert.setAlertTime(new Date());
      alert.setProvider(Alert.Provider.valueOf(json.getString("Provider")).
              toString());
      alert.setSeverity(Alert.Severity.valueOf(json.getString("Severity")).
              toString());
      alert.setAgentTime(json.getJsonNumber("Time").bigIntegerValue());
      alert.setMessage(json.getString("Message"));
      alert.setHostid(json.getString("host-id"));
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
        alert.setCurrentValue(Boolean.
                toString(json.getBoolean("CurrentValue")));
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
    return Response.ok().build();
  }
}
