package se.kth.kthfsdashboard.rest.resources;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.security.Key;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.Certificate;
import java.util.ArrayList;
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
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.openssl.PEMReader;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import se.kth.kthfsdashboard.alert.Alert;
import se.kth.kthfsdashboard.alert.AlertEJB;
import se.kth.kthfsdashboard.host.Host;
import se.kth.kthfsdashboard.host.HostEJB;
import se.kth.kthfsdashboard.role.Role;
import se.kth.kthfsdashboard.role.RoleEJB;
import se.kth.kthfsdashboard.struct.Status;

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

    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        
    }

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
        Host host;
        JSONObject json = new JSONObject();
        try {
            host = hostEJB.findHostByName(name);
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
        JSONObject json;
        List<Host> hosts = hostEJB.findHosts();
        if (hosts == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
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

    @PUT
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register(@Context HttpServletRequest req, String jsonStrig) {
        try {
            JSONObject json = new JSONObject(jsonStrig);
            String hostId = json.getString("host-id");
            Host host = hostEJB.findHostById(hostId);
            if (host == null) {
                logger.log(Level.INFO, "Could not register host with id {0}: unknown host id.", hostId);
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            if (host.isRegistered()) {
                logger.log(Level.INFO, "Could not register host with id {0}: already registered.", hostId);
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            
            String cert = signCertificate(json.getString("csr"));
            
            host.setRegistered(true);
            host.setLastHeartbeat((new Date()).getTime());
            host.setHostname(json.getString("hostname"));
            host.setPublicIp(json.getString("public-ip"));
            host.setPrivateIp(json.getString("private-ip"));
            host.setCores(json.getInt("cores"));

            hostEJB.storeHost(host, false);
            roleEjb.deleteRolesByHostId(hostId);
            
            logger.log(Level.INFO, "Host with id + {0} registered successfully.", hostId);
            return Response.ok(cert).build();
            
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Exception: {0}", ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PUT
    @Path("/heartbeat")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response heartbeat(@Context HttpServletRequest req, String jsonStrig) {
        JSONArray roles;
        try {
            JSONObject json = new JSONObject(jsonStrig);
            long agentTime = json.getLong("agent-time");
            String hostId = json.getString("host-id");
            Host host = hostEJB.findHostById(hostId);
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

            roles = json.getJSONArray("services");
            for (int i = 0; i < roles.length(); i++) {
                JSONObject s = roles.getJSONObject(i);
                Role role = new Role();
                role.setHostId(host.getHostId());
                role.setCluster(s.getString("cluster"));
                role.setService(s.getString("service"));
                role.setRole(s.getString("role"));
                role.setWebPort(s.has("web-port") ? s.getInt("web-port") : null);
                role.setPid(s.has("pid") ? s.getInt("pid") : 0);
                role.setStatus(Status.valueOf(s.getString("status")));
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

    @PUT
    @Path("/sign")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response sign(@Context HttpServletRequest req, String jsonStrig) {
        try {
            JSONObject json = new JSONObject(jsonStrig);
            String csr = json.getString("csr");
            String cert = signCertificate(csr);
            return Response.ok(cert).build();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Exception: ".concat(ex.getMessage()));
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Path("/alert")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response alert(@Context HttpServletRequest req, String jsonString) {

//       TODO: Alerts are stored in the database. Later, we should define reactions (Email, SMS, ...).
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
            alert.setType(json.getString("Type"));
            alert.setTypeInstance(json.getString("TypeInstance"));
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

    private String signCertificate(String csr) throws CryptoException, Exception {

        PEMReader reader = new PEMReader(new StringReader(csr));
        Object pemObject;
        try {
            pemObject = reader.readObject();
        } catch (IOException e) {
            logger.log(Level.INFO, "Could not read CSR from string: {0}", e);
            throw new CryptoException("Could not read CSR from string: " + e.getMessage());
        }
        if (pemObject instanceof PKCS10CertificationRequest) {
            PKCS10CertificationRequest certificationRequest = (PKCS10CertificationRequest) pemObject;
            try {
                if (!certificationRequest.verify()) {
                    logger.info("CSR signature is not correct.");
                    throw new CryptoException("CSR signature is not correct.");
                }
            } catch (Exception e) {
                logger.log(Level.INFO, "Cannot verify CSR signature: {0}", e);
                throw new CryptoException("Cannot verify CSR signature: " + e.getMessage());
            }

            File csrFile = File.createTempFile(System.getProperty("java.io.tmpdir"), ".csr");
            File certFile = File.createTempFile(System.getProperty("java.io.tmpdir"), ".cert");
            FileUtils.writeStringToFile(csrFile, csr);

            String keyFileName = "s1as.pem";
            String keystorePassword = "changeit";
            File keyFile = new File(keyFileName);
            if (!keyFile.exists()) {
                logger.log(Level.INFO, "Key/Cert file ({0}) does not exist. Exporting Key/Cert from keystore...", keyFileName);
                KeyStore ks = KeyStore.getInstance("jks");
                ks.load(new FileInputStream("keystore.jks"), keystorePassword.toCharArray());
                Certificate cert = ks.getCertificate("s1as");
                Key key = ks.getKey("s1as", keystorePassword.toCharArray());
                String certString = new String(Base64.encodeBase64(cert.getEncoded(), true));
                String keyString = new String(Base64.encodeBase64(key.getEncoded(), true));
                String content;
                content = "-----BEGIN CERTIFICATE-----\n";
                content += certString;
                content += "-----END CERTIFICATE-----\n";
                content += "-----BEGIN PRIVATE KEY-----\n";
                content += keyString;
                content += "-----END PRIVATE KEY-----\n";
                keyFile.createNewFile();
                FileUtils.writeStringToFile(keyFile, content);
            }
            logger.info("Signing CSR...");
            List<String> cmds = new ArrayList<String>();
            cmds.add("openssl");
            cmds.add("x509");
            cmds.add("-req");
            cmds.add("-CA");
            cmds.add(keyFile.getAbsolutePath());
            cmds.add("-CAkey");
            cmds.add(keyFile.getAbsolutePath());
            cmds.add("-in");
            cmds.add(csrFile.getAbsolutePath());
            cmds.add("-out");
            cmds.add(certFile.getAbsolutePath());
            cmds.add("-days");
            cmds.add("3650");
            cmds.add("-CAcreateserial");
            Process process = new ProcessBuilder(cmds).directory(new File("/usr/bin/")).redirectErrorStream(true).start();

            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.forName("UTF8")));
            String line;
            while ((line = br.readLine()) != null) {
                logger.info(line);
            }
            process.waitFor();
            if (process.exitValue() != 0) {
                throw new RuntimeException("Failed to sign CSR");
            }
            logger.info("Singned CSR");
            String agentCert = FileUtils.readFileToString(certFile);
            return agentCert;
        } else {
            throw new CryptoException("Not an instance of PKCS10CertificationRequest.");
        }

    }
}
