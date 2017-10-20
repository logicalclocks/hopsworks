package io.hops.hopsworks.api.certs;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import io.hops.hopsworks.api.annotation.AllowCORS;
import io.hops.hopsworks.common.dao.host.Host;
import io.hops.hopsworks.common.dao.host.HostEJB;
import io.hops.hopsworks.common.dao.kafka.CsrDTO;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.util.PKIUtils;
import io.hops.hopsworks.common.util.Settings;
import io.swagger.annotations.Api;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

@Path("/agentservice")
@Stateless
@Api(value = "/agentservice", description = "Agent service")
public class CertSigningService {

  final static Logger logger = Logger.getLogger(CertSigningService.class.
          getName());

  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private Settings settings;
  @EJB
  private HostEJB hostEJB;

  @POST
  @Path("/register")
  @RolesAllowed({"AGENT"})
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response register(@Context HttpServletRequest req, String jsonString)
          throws AppException {
    JSONObject json = new JSONObject(jsonString);
    String pubAgentCert = "no certificate";
    String caPubCert = "no certificate";
    if (json.has("csr")) {
      String csr = json.getString("csr");
      try {
        pubAgentCert = PKIUtils.signCertificate(settings, csr, true);
        caPubCert = Files.toString(new File(settings.getIntermediateCaDir()
                + "/certs/ca-chain.cert.pem"), Charsets.UTF_8);
      } catch (IOException | InterruptedException ex) {
        Logger.getLogger(CertSigningService.class.getName()).log(Level.SEVERE,
                null,
                ex);
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                getStatusCode(), ex.toString());
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
        Logger.getLogger(CertSigningService.class.getName()).log(Level.SEVERE,
                null,
                ex);
      }
    }

    CsrDTO dto = new CsrDTO(caPubCert, pubAgentCert, settings.getHadoopVersionedDir());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            dto).build();
  }

  @POST
  @Path("/hopsworks")
  @AllowCORS
  @RolesAllowed({"AGENT", "CLUSTER_AGENT"})
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response hopsworks(@Context HttpServletRequest req, String jsonString) throws AppException {
    JSONObject json = new JSONObject(jsonString);
    String pubAgentCert = "no certificate";
    String caPubCert = "no certificate";
    if (json.has("csr")) {
      String csr = json.getString("csr");
      try {
        pubAgentCert = PKIUtils.signCertificate(settings, csr, false);
        caPubCert = Files.toString(new File(settings.getCaDir() + "/certs/ca.cert.pem"), Charsets.UTF_8);
      } catch (IOException | InterruptedException ex) {
        logger.log(Level.SEVERE,null,ex);
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ex.toString());
      }
    }

    CsrDTO dto = new CsrDTO(caPubCert, pubAgentCert, settings.getHadoopVersionedDir());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            dto).build();
  }

  @GET
  @Path("/crl")
  public Response getCRL() throws FileNotFoundException {
    File certFile;
    try {
      certFile = File.createTempFile(System.getProperty("java.io.tmpdir"), ".pem");
      String crl = PKIUtils.createCRL(settings.getCaDir(), settings.getHopsworksMasterPasswordSsl(), false);
      FileUtils.writeStringToFile(certFile, crl);
    } catch (IOException | InterruptedException ex) {
      logger.log(Level.SEVERE, null, ex);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(ex.getMessage()).build();
    }
    InputStream stream = new FileInputStream(certFile); 
    StreamingOutput sout = (OutputStream out) -> {
      try {
        int length;
        byte[] buffer = new byte[1024];
        while ((length = stream.read(buffer)) != -1) {
          out.write(buffer, 0, length);
        }
        out.flush();
      } finally {
        stream.close();
      }
    };
    Response.ResponseBuilder response = Response.ok(sout);
    response.header("Content-disposition", "attachment; filename=crl.pem");
    return response.build();
  }
  
  @POST
  @Path("/verifyCert")
  public Response verifyCert(String jsonString) {
    JSONObject json = new JSONObject(jsonString);
    String status = "Not available.";
    if (json.has("cert")) {
      String cert = json.getString("cert");
      try {
        status = PKIUtils.verifyCertificate(cert, settings.getCaDir(), settings.getHopsworksMasterPasswordSsl(), false);
      } catch (IOException | InterruptedException ex) {
        logger.log(Level.SEVERE, null, ex);
      }
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(status).build();
  }
  
//  @POST
//  @Path("/addUserToProject")
//  @Consumes(MediaType.APPLICATION_JSON)
//  @Produces(MediaType.APPLICATION_JSON)
//  public Response addUserToProject(@Context HttpServletRequest req,
//          UserCertCreationReqDTO userCert)
//          throws AppException {
//
//    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
//            entity(
//                    dto).build();
//  }

}
