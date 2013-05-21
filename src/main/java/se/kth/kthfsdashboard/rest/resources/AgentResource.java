package se.kth.kthfsdashboard.rest.resources;

import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import se.kth.kthfsdashboard.alert.Alert;
import se.kth.kthfsdashboard.alert.AlertEJB;
import se.kth.kthfsdashboard.host.Host;
import se.kth.kthfsdashboard.host.HostEJB;
import se.kth.kthfsdashboard.role.Role;
import se.kth.kthfsdashboard.role.RoleEJB;

/**
 * :
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@Path("/agent")
@Stateless
@RolesAllowed({"AGENT", "ADMIN"})
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
   public String getLog() {
      return "KTHFSDashboard: Pong";
   }

   @GET
   @Path("load/{name}")
   @Produces(MediaType.APPLICATION_JSON)
   public Response getLoadAvg(@PathParam("name") String name) {
      Host host = hostEJB.findHostByName(name);
      if (host == null) {
         return Response.status(Status.NOT_FOUND).build();
      }
      JSONObject json = new JSONObject();
      try {
         json.put("hostname", host.getHostname());
         json.put("cores", host.getCores());
         json.put("load1", host.getLoad1());
         json.put("load5", host.getLoad5());
         json.put("load15", host.getLoad15());
      } catch (Exception ex) {
         // TODO - Should log all exceptions 
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
      }
      return Response.ok(json).build();
   }

   @GET
   @Path("loads")
   @Produces(MediaType.APPLICATION_JSON)
   public Response getLoads() {
      JSONArray jsonArray = new JSONArray();
      JSONObject json;
      List<Host> hosts = hostEJB.findHosts();
      if (hosts == null) {
         return Response.status(Status.NOT_FOUND).build();
      }
      for (Host host : hosts) {
         try {
            json = new JSONObject();
            json.put("hostname", host.getHostname());
            json.put("cores", host.getCores());
            json.put("load1", host.getLoad1());
            json.put("load5", host.getLoad5());
            json.put("load15", host.getLoad15());
            jsonArray.put(json);
         } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
         }
      }
      return Response.ok(jsonArray).build();
   }

   @POST
   @Path("/heartbeat")
   @Consumes(MediaType.APPLICATION_JSON)
   public Response register(@Context HttpServletRequest req, String jsonStrig) {
      try {
         JSONObject json = new JSONObject(jsonStrig);

         Host host = new Host();
         host.setLastHeartbeat((new Date()).getTime());
         host.setHostname(json.getString("hostname"));
         host.setPublicIp(json.getString("public-ip"));
         host.setPrivateIp(json.getString("private-ip"));
         host.setCores(json.getInt("cores"));
         host.setRack(json.getString("rack"));
         hostEJB.storeHost(host, true);

      } catch (Exception ex) {
         logger.log(Level.SEVERE, "Exception: {0}", ex);
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
      }
      return Response.ok().build();
   }

   @PUT
   @Path("/heartbeat")
   @Consumes(MediaType.APPLICATION_JSON)
   public Response heartbeat(@Context HttpServletRequest req, String jsonStrig) {
      try {
         JSONObject json = new JSONObject(jsonStrig);

         long agentTime = json.getLong("agent-time");

         Host host = new Host();
         host.setLastHeartbeat((new Date()).getTime());
         host.setHostname(json.getString("hostname"));         
         host.setLoad1(json.getDouble("load1"));
         host.setLoad5(json.getDouble("load5"));
         host.setLoad15(json.getDouble("load15"));
         host.setDiskCapacity(json.getLong("disk-capacity"));
         host.setDiskUsed(json.getLong("disk-used"));
         host.setMemoryCapacity(json.getLong("memory-capacity"));
         host.setMemoryUsed(json.getLong("memory-used"));
         hostEJB.storeHost(host, false);

         JSONArray roles = json.getJSONArray("services");

         for (int i = 0; i < roles.length(); i++) {
            JSONObject s = roles.getJSONObject(i);
            Role role = new Role();
            role.setHostname(host.getHostname());
            role.setCluster(s.getString("cluster"));
            role.setServiceGroup(s.getString("service"));
            role.setRole(s.getString("role"));
            role.setWebPort(s.has("web-port") ? s.getInt("web-port") : null);
            role.setPid(s.has("pid") ? s.getInt("pid") : 0);
            role.setStatus(Role.Status.valueOf(s.getString("status")));
            if (s.has("stop-time")) {
               role.setUptime(s.getLong("stop-time") - s.getLong("start-time"));
            } else if (s.has("start-time")) {
               role.setUptime(agentTime - s.getLong("start-time"));
            }
            roleEjb.store(role);
         }
      } catch (Exception ex) {
         logger.log(Level.SEVERE, "Exception: {0}", ex);
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
      }
      return Response.ok().build();
   }

   @POST
   @Path("/alert")
   @Consumes(MediaType.APPLICATION_JSON)
   public Response alert(@Context HttpServletRequest req, String jsonStrig) {

//       TODO: Alerts are stored in the database. Later, we should define reactions (Email, SMS, ...).
      try {
         JSONObject json = new JSONObject(jsonStrig);

         Alert alert = new Alert();
         alert.setAlertTime(new Date());

         alert.setAgentTime(json.getLong("Time"));
         alert.setMessage(json.getString("Message"));
         alert.setHostname(json.getString("Host"));
         alert.setSeverity(Alert.Severity.valueOf(json.getString("Severity")));

         alert.setPlugin(json.getString("Plugin"));
         if (json.has("PluginInstance")) {
            alert.setPluginInstance(json.getString("PluginInstance"));
         }

         alert.setType(json.getString("Type"));
         alert.setTypeInstance(json.getString("TypeInstance"));

         alert.setDataSource(json.getString("DataSource"));
         alert.setCurrentValue(json.getString("CurrentValue"));
         if (json.has("WarningMin")) {
            alert.setWarningMin(json.getString("WarningMin"));
         }
         if (json.has("WarningMax")) {
            alert.setWarningMax(json.getString("WarningMax"));
         }
         if (json.has("FailureMin")) {
            alert.setFailureMin(json.getString("FailureMin"));
         }
         if (json.has("FailureMax")) {
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
