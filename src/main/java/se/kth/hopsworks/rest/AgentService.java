/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hopsworks.rest;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import io.hops.kafka.CsrDTO;
import io.hops.kmon.host.Host;
import io.hops.kmon.host.HostEJB;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.json.JSONObject;
import se.kth.hopsworks.util.PKIUtils;
import se.kth.hopsworks.util.Settings;

/**
 *
 * @author jdowling
 */
@Path("/agentservice")
@Stateless
@RolesAllowed({"AGENT"})
public class AgentService {

  final static Logger logger = Logger.getLogger(AgentService.class.getName());

  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private Settings settings;
  @EJB
  private HostEJB hostEJB;
  
  @PUT
  @Path("/register")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response sign(@Context HttpServletRequest req, String jsonString) throws AppException {
    JSONObject json = new JSONObject(jsonString);
    String pubAgentCert = "no certificate";
    String caPubCert = "no certificate";
    if (json.has("csr")) {
      String csr = json.getString("csr");
      try {
        pubAgentCert = PKIUtils.signWithServerCertificate(csr, settings.getIntermediateCaDir(), settings.getHopsworksMasterPasswordSsl());
        caPubCert = Files.toString(new File(settings.getIntermediateCaDir() + "/certs/ca-chain.cert.pem"), Charsets.UTF_8);
      } catch (IOException | InterruptedException ex) {
        Logger.getLogger(AgentService.class.getName()).log(Level.SEVERE, null, ex);
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ex.toString());
      }
    }
    
    if (json.has("host-id") && json.has("agent-password")) {    
      String hostId = json.getString("host-id");
      Host host;
      try {
        host = hostEJB.findByHostId(hostId);
        String agentPassword = json.getString("agent-password");
        host.setAgentPassword(agentPassword);
        host.setRegistered(true);
        hostEJB.storeHost(host, true);
      } catch (Exception ex) {
        Logger.getLogger(AgentService.class.getName()).log(Level.SEVERE, null, ex);
      }
    }    
    
    CsrDTO dto = new CsrDTO(caPubCert, pubAgentCert);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            dto).build();
  }
}