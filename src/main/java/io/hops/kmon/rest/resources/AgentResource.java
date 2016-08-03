package io.hops.kmon.rest.resources;

import io.hops.kmon.alert.Alert;
import io.hops.kmon.alert.AlertEJB;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import io.hops.kmon.host.Host;
import io.hops.kmon.host.HostEJB;
import io.hops.kmon.role.Role;
import io.hops.kmon.role.RoleEJB;
import io.hops.kmon.struct.Status;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.POST;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@Path("/agentresource")
@Stateless
@RolesAllowed({"AGENT"})
public class AgentResource {

  @EJB
  private HostEJB hostEJB;
  @EJB
  private RoleEJB roleEjb;
  @EJB
  private AlertEJB alertEJB;

  final static Logger logger = Logger.getLogger(AgentResource.class.getName());

  @GET
  @Path("ping")
  @Produces(MediaType.TEXT_PLAIN)
  public String ping() {
    return "KTHFS Dashboard: Pong";
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
//    @PUT
//    @Path("/register")
//    @Consumes(MediaType.APPLICATION_JSON)
//    public Response register(@Context HttpServletRequest req, String jsonString) {
//        try {
//            JSONObject json = new JSONObject(jsonString);
//            String hostId = json.getString("host-id");
//            Host host = hostEJB.findByHostId(hostId);
//            boolean toRegister = false;
//            if (host == null) {
//                logger.log(Level.INFO, "Registering host with id {0}: unknown host id.", hostId);
//                host = new Host();
//                host.setHostId(hostId);
//                toRegister = true;
//                if (json.has("hostname") == false) {
//                    host.setHostname("vagrant");
//                } else {
//                    host.setHostname(json.getString("hostname"));                    
//                }
//            } else {
//                if (host.isRegistered()) {
//                    logger.log(Level.INFO, "Re-registering host with id {0}: already registered.", hostId);
////                    return Response.status(Response.Status.NOT_FOUND).build();
//                }
//                host.setHostname(json.getString("hostname"));
//            }
//            String certificate = "no certificate";
//            if (json.has("csr")) {
//                String csr = json.getString("csr");
//                certificate = PKIUtils.signWithServerCertificate(csr);
//            }
//
//            host.setRegistered(true);
//            host.setLastHeartbeat((new Date()).getTime());
//            if (json.has("public-ip")) {
//                host.setPublicIp(json.getString("public-ip"));
//            }
//            if (json.has("private-ip")) {
//                host.setPrivateIp(json.getString("private-ip"));
//            }
//            if (json.has("disk-capacity")) {
//                host.setDiskCapacity(json.getLong("disk-capacity"));
//            }
//            if (json.has("memory-capacity")) {
//                host.setMemoryCapacity(json.getLong("memory-capacity"));
//            }
//            if (json.has("cores")) {
//                host.setCores(json.getInt("cores"));
//            }
//            hostEJB.storeHost(host, toRegister);
//            roleEjb.deleteRolesByHostId(hostId);
//            logger.log(Level.INFO, "Host with id {0} registered successfully.", hostId);
//            return Response.ok(certificate).build();
//        } catch (Exception ex) {
//            logger.log(Level.SEVERE, "Exception: {0}", ex);
//            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
//        }
//    }
  @PUT
  @Path("/heartbeat")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response heartbeat(@Context HttpServletRequest req, String jsonHb) {
    try {

      InputStream stream = new ByteArrayInputStream(jsonHb.getBytes(StandardCharsets.UTF_8));
      JsonObject json = Json.createReader(stream).readObject();
      long agentTime = json.getJsonNumber("agent-time").longValue();
      String hostId = json.getString("host-id");
      Host host = hostEJB.findByHostId(hostId);
      if (host == null) {
        logger.log(Level.INFO, "Host with id {0} not found.", hostId);
        return Response.status(Response.Status.NOT_FOUND).build();
      }
//            if (!host.isRegistered()) {
//                logger.log(Level.INFO, "Host with id {0} is not registered.", hostId);
//                return Response.status(Response.Status.NOT_ACCEPTABLE).build();
//            }
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
      hostEJB.storeHost(host, false);

      JsonArray roles = json.getJsonArray("services");
      for (int i = 0; i < roles.size(); i++) {
        JsonObject s = roles.getJsonObject(i);
        Role role = new Role();
        role.setHostId(host.getHostId());
        role.setCluster(s.getString("cluster"));
        role.setService(s.getString("service"));
        if (s.containsKey("role")) {
          role.setRole(s.getString("role"));
        } else {
          role.setRole("");
        }
        String webPort = s.containsKey("web-port") ? s.getString("web-port") : "0";
        role.setWebPort(Integer.parseInt(webPort));
        String pid = s.containsKey("pid") ? s.getString("pid") : "-1";
        role.setPid(Integer.parseInt(pid));
        if (s.containsKey("status")) {
          role.setStatus(Status.valueOf(s.getString("status")));
        } else {
          role.setStatus(Status.None);
        }
        String startTime = null;
        if (s.containsKey("start-time")) {
          startTime = s.getString("start-time");
        }
        String stopTime = null;
        if (s.containsKey("stop-time")) {
          stopTime = s.getString("stop-time");
        }
        if (stopTime != null) {
          role.setUptime(Long.parseLong(stopTime) - Long.parseLong(startTime));
        } else if (startTime != null) {
          role.setUptime(agentTime - Long.parseLong(startTime));
        }
        roleEjb.store(role);
      }
    } catch (Exception ex) {
      logger.log(Level.SEVERE, "Exception: ".concat(ex.getMessage()));
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
    return Response.ok().build();
  }

  @POST
  @Path("/alert")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response alert(@Context HttpServletRequest req, String jsonString) {
    // TODO: Alerts are stored in the database. Later, we should define reactions (Email, SMS, ...).
    try {
      InputStream stream = new ByteArrayInputStream(jsonString.getBytes(StandardCharsets.UTF_8));
      JsonObject json = Json.createReader(stream).readObject();
      Alert alert = new Alert();
      alert.setAlertTime(new Date());
      alert.setProvider(Alert.Provider.valueOf(json.getString("Provider")));
      alert.setSeverity(Alert.Severity.valueOf(json.getString("Severity")));
      alert.setAgentTime(json.getJsonNumber("Time").longValue());
      alert.setMessage(json.getString("Message"));
      alert.setHostId(json.getString("Host"));
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
        alert.setCurrentValue(json.getString("CurrentValue"));
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
      alertEJB.persistAlert(alert);

    } catch (Exception ex) {
      logger.log(Level.SEVERE, "Exception: {0}", ex);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
    return Response.ok().build();
  }
}
