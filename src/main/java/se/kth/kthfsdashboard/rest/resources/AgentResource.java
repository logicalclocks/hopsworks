package se.kth.kthfsdashboard.rest.resources;

import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import se.kth.kthfsdashboard.alert.Alert;
import se.kth.kthfsdashboard.alert.AlertEJB;
import se.kth.kthfsdashboard.host.Host;
import se.kth.kthfsdashboard.host.HostEJB;
import se.kth.kthfsdashboard.role.Role;
import se.kth.kthfsdashboard.role.RoleEJB;
import se.kth.kthfsdashboard.struct.Status;
import se.kth.kthfsdashboard.utils.PKIUtils;

/**
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
        return "KTHFS Dashboard: Pong";
    }

    @GET
    @Path("load/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLoadAvg(@PathParam("name") String name) {
        JSONObject json = new JSONObject();
        try {
            Host host = hostEJB.findByHostname(name);
            json.put("hostname", host.getHostname());
            json.put("cores", host.getCores());
            json.put("load1", host.getLoad1());
            json.put("load5", host.getLoad5());
            json.put("load15", host.getLoad15());
        } catch (Exception ex) {
            // TODO - Should log all exceptions          
            logger.log(Level.SEVERE, "Exception: {0}", ex);
            if (ex.getMessage().equals("NoResultException")) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok(json).build();
    }

    @GET
    @Path("loads")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLoads() {
        JSONArray jsonArray = new JSONArray();
        List<Host> hosts = hostEJB.find();
        for (Host host : hosts) {
            try {
                JSONObject json = new JSONObject();
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

    @PUT
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register(@Context HttpServletRequest req, String jsonStrig) {
        try {
            JSONObject json = new JSONObject(jsonStrig);
            String hostId = json.getString("host-id");
            Host host = hostEJB.findByHostId(hostId);
            if (host == null) {
                logger.log(Level.INFO, "Could not register host with id {0}: unknown host id.", hostId);
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            if (host.isRegistered()) {
                logger.log(Level.INFO, "Did not register host with id {0}: already registered.", hostId);
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            String csr = json.getString("csr");
            String certificate = PKIUtils.signWithServerCertificate(csr);

            host.setRegistered(true);
            host.setLastHeartbeat((new Date()).getTime());
            host.setHostname(json.getString("hostname"));
            if (json.has("public-ip")) {
                host.setPublicIp(json.getString("public-ip"));
            }
            if (json.has("private-ip")) {
                host.setPrivateIp(json.getString("private-ip"));
            }
            host.setDiskCapacity(json.getLong("disk-capacity"));
            host.setMemoryCapacity(json.getLong("memory-capacity"));
            host.setCores(json.getInt("cores"));
            hostEJB.storeHost(host, false);
            roleEjb.deleteRolesByHostId(hostId);
            logger.log(Level.INFO, "Host with id {0} registered successfully.", hostId);
            return Response.ok(certificate).build();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Exception: {0}", ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PUT
    @Path("/heartbeat")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response heartbeat(@Context HttpServletRequest req, String jsonStrig) {
        try {
            JSONObject json = new JSONObject(jsonStrig);
            long agentTime = json.getLong("agent-time");
            String hostId = json.getString("host-id");
            Host host = hostEJB.findByHostId(hostId);
            if (host == null) {
                logger.log(Level.INFO, "Host with id {0} not found.", hostId);
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            if (!host.isRegistered()) {
                logger.log(Level.INFO, "Host with id {0} is not registered.", hostId);
                return Response.status(Response.Status.NOT_ACCEPTABLE).build();
            }
            host.setLastHeartbeat((new Date()).getTime());
            host.setLoad1(json.getDouble("load1"));
            host.setLoad5(json.getDouble("load5"));
            host.setLoad15(json.getDouble("load15"));
            host.setDiskUsed(json.getLong("disk-used"));
            host.setMemoryUsed(json.getLong("memory-used"));
            hostEJB.storeHost(host, false);

            JSONArray roles = json.getJSONArray("services");
            for (int i = 0; i < roles.length(); i++) {
                JSONObject s = roles.getJSONObject(i);
                Role role = new Role();
                role.setHostId(host.getHostId());
                role.setCluster(s.getString("cluster"));
                role.setService(s.getString("service"));
                if (s.has("role")) {
                    role.setRole(s.getString("role"));
                } else {
                    role.setRole("");
                }
                role.setWebPort(s.has("web-port") ? s.getInt("web-port") : null);
                role.setPid(s.has("pid") ? s.getInt("pid") : 0);
                if (s.has("status")) {
                    role.setStatus(Status.valueOf(s.getString("status")));
                } else {
                    role.setStatus(Status.None);
                }
                if (s.has("stop-time")) {
                    role.setUptime(s.getLong("stop-time") - s.getLong("start-time"));
                } else if (s.has("start-time")) {
                    role.setUptime(agentTime - s.getLong("start-time"));
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
            JSONObject json = new JSONObject(jsonString);
            Alert alert = new Alert();
            alert.setAlertTime(new Date());
            alert.setProvider(Alert.Provider.valueOf(json.getString("Provider")));
            alert.setSeverity(Alert.Severity.valueOf(json.getString("Severity")));
            alert.setAgentTime(json.getLong("Time"));
            alert.setMessage(json.getString("Message"));
            alert.setHostId(json.getString("Host"));
            alert.setPlugin(json.getString("Plugin"));
            if (json.has("PluginInstance")) {
                alert.setPluginInstance(json.getString("PluginInstance"));
            }
            if (json.has("Type")) {
                alert.setType(json.getString("Type"));
            }
            if (json.has("TypeInstance")) {
                alert.setTypeInstance(json.getString("TypeInstance"));
            }
            if (json.has("DataSource")) {
                alert.setDataSource(json.getString("DataSource"));
            }
            if (json.has("CurrentValue")) {
                alert.setCurrentValue(json.getString("CurrentValue"));
            }
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
