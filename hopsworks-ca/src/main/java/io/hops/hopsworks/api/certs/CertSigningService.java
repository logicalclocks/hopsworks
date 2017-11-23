package io.hops.hopsworks.api.certs;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import io.hops.hopsworks.api.annotation.AllowCORS;
import io.hops.hopsworks.common.dao.host.Host;
import io.hops.hopsworks.common.dao.host.HostEJB;
import io.hops.hopsworks.common.dao.kafka.CsrDTO;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.cluster.ClusterCert;
import io.hops.hopsworks.common.dao.user.cluster.ClusterCertFacade;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.security.PKIUtils;
import io.hops.hopsworks.common.util.Settings;
import io.swagger.annotations.Api;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Objects;
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
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.StreamingOutput;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

@Path("/agentservice")
@Stateless
@Api(value = "Certificate Signing", description = "Sign certificates for hosts or clusters")
public class CertSigningService {

  final static Logger logger = Logger.getLogger(CertSigningService.class.
      getName());

  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private Settings settings;
  @EJB
  private HostEJB hostEJB;
  @EJB
  private UserFacade userBean;
  @EJB
  private ClusterCertFacade clusterCertFacade;

  @POST
  @Path("/register")
  @RolesAllowed({"AGENT"})
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response register(@Context HttpServletRequest req, String jsonString)
          throws AppException {
    logger.log(Level.INFO, "Request to sign host certificate: \n{0}", jsonString);
    JSONObject json = new JSONObject(jsonString);
    String pubAgentCert = "no certificate";
    String caPubCert = "no certificate";
    if (json.has("csr")) {
      String csr = json.getString("csr");
      try {
        
        pubAgentCert = PKIUtils.signCertificate(settings, csr, true);
        logger.info("Signed host certificate.");        
        caPubCert = Files.toString(new File(settings.getIntermediateCaDir()
            + "/certs/ca-chain.cert.pem"), Charsets.UTF_8);
      } catch (IOException | InterruptedException ex) {
        logger.log(Level.SEVERE, "Cert signing error: {0}", ex.getMessage());
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
        logger.log(Level.SEVERE, "Host storing error while Cert signing: {0}", ex.getMessage());
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
  public Response hopsworks(@Context HttpServletRequest req, @Context SecurityContext sc, String jsonString) throws
      AppException, IOException, InterruptedException {
    JSONObject json = new JSONObject(jsonString);
    String pubAgentCert = "no certificate";
    String caPubCert = "no certificate";
    String intermediateCaPubCert = "no certificate";
    Users user = userBean.findByEmail(sc.getUserPrincipal().getName());
    ClusterCert clusterCert;
    if (json.has("csr")) {
      String csr = json.getString("csr");
      clusterCert = checkCSR(user, csr);
      try {
        pubAgentCert = PKIUtils.signCertificate(settings, csr, true);
        caPubCert = Files.toString(new File(settings.getCertsDir() + "/certs/ca.cert.pem"), Charsets.UTF_8);
        intermediateCaPubCert = Files.toString(
            new File(settings.getIntermediateCaDir() + "/certs/intermediate.cert.pem"), Charsets.UTF_8);
        clusterCert.setSerialNumber(getSerialNumFromCert(pubAgentCert));
        clusterCertFacade.update(clusterCert);
      } catch (IOException | InterruptedException ex) {
        logger.log(Level.SEVERE, null, ex);
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ex.toString());
      }
    }

    CsrDTO dto = new CsrDTO(caPubCert, intermediateCaPubCert, pubAgentCert, settings.getHadoopVersionedDir());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(dto).build();
  }

  @GET
  @Path("/crl")
  public Response getCRL() throws FileNotFoundException {
    File certFile;
    try {
      certFile = File.createTempFile(System.getProperty("java.io.tmpdir"), ".pem");
      String crl = PKIUtils.createCRL(settings, true);
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
        status = PKIUtils.verifyCertificate(settings, cert, true);
      } catch (IOException | InterruptedException ex) {
        logger.log(Level.SEVERE, null, ex);
      }
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(status).build();
  }

  private ClusterCert checkCSR(Users user, String csr) throws IOException, InterruptedException {
    if (user == null || user.getEmail() == null || csr == null || csr.isEmpty()) {
      throw new IllegalArgumentException("User or csr not set.");
    }
    ClusterCert clusterCert;
    //subject=/C=se/CN=bbc.sics.se/ST=stockholm/L=kista/O=hopsworks/OU=hs/emailAddress=dela1@kth.se
    String subject = PKIUtils.getSubjectFromCSR(csr);
    HashMap<String, String> keyVal = getKeyValuesFromSubject(subject);
    String email = keyVal.get("emailAddress");
    String commonName = keyVal.get("CN");
    String organizationName = keyVal.get("O");
    String organizationalUnitName = keyVal.get("OU");
    if (email == null || email.isEmpty() || !email.equals(user.getEmail())) {
      throw new IllegalArgumentException("CSR email not set or does not match user.");
    }
    if (commonName == null || commonName.isEmpty()) {
      throw new IllegalArgumentException("CSR commonName not set.");
    }
    if (organizationName == null || organizationName.isEmpty()) {
      throw new IllegalArgumentException("CSR organizationName not set.");
    }
    if (organizationalUnitName == null || organizationalUnitName.isEmpty()) {
      throw new IllegalArgumentException("CSR organizationalUnitName not set.");
    }
    
    clusterCert = clusterCertFacade.getByOrgUnitNameAndOrgName(organizationName, organizationalUnitName);
    if (clusterCert == null) {
      throw new IllegalArgumentException(
          "No cluster registerd with the given organization name and organizational unit.");
    }
    if (clusterCert.getSerialNumber() != null && !clusterCert.getSerialNumber().isEmpty()) {
      throw new IllegalArgumentException("Cluster already have a signed certificate.");
    }
    if (!clusterCert.getCommonName().equals(commonName)) {
      throw new IllegalArgumentException("No cluster registerd with the given common name.");
    }
    if (!Objects.equals(clusterCert.getAgentId(), user)) {
      throw new IllegalArgumentException("Cluster not registerd for user.");
    }
    return clusterCert;
  }

  private HashMap<String, String> getKeyValuesFromSubject(String subject) {
    if (subject == null || subject.isEmpty()) {
      return null;
    }
    String[] parts = subject.split("/");
    String[] keyVal;
    HashMap<String, String> keyValStore = new HashMap<>();
    for (String part : parts) {
      keyVal = part.split("=");
      if (keyVal.length < 2) {
        continue;
      }
      keyValStore.put(keyVal[0], keyVal[1]);
    }
    return keyValStore;
  }

  private String getSerialNumFromCert(String pubAgentCert) throws IOException, InterruptedException {
    String serialNum = PKIUtils.getSerialNumberFromCert(pubAgentCert);
    String[] parts = serialNum.split("=");
    if (parts.length < 2) {
      throw new IllegalStateException("Failed to get serial number from cert.");
    }
    return parts[1];
  }
}
