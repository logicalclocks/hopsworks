/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 */
package io.hops.hopsworks.ca.api.certs;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import io.hops.hopsworks.ca.api.annotation.AllowCORS;
import io.hops.hopsworks.common.dao.kafka.CsrDTO;
import io.hops.hopsworks.common.exception.GenericException;
import io.hops.hopsworks.common.exception.HopsSecurityException;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.security.CAException;
import io.hops.hopsworks.common.security.CertificateType;
import io.hops.hopsworks.common.security.CertificatesMgmService;
import io.hops.hopsworks.common.security.DelaCSRCheckException;
import io.hops.hopsworks.common.security.DelaTrackerCertController;
import io.hops.hopsworks.common.security.OpensslOperations;
import io.hops.hopsworks.common.security.PKI;
import io.hops.hopsworks.common.security.ServiceCertificateRotationTimer;
import io.hops.hopsworks.common.util.Settings;
import io.swagger.annotations.Api;
import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.hops.hopsworks.common.security.CertificateType.APP;

@Path("/agentservice")
@Stateless
@Api(value = "Sign certificates for hosts or clusters")
public class CertSigningService {

  private static final Logger LOGGER = Logger.getLogger(CertSigningService.class.getName());

  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private Settings settings;
  @EJB
  private OpensslOperations opensslOperations;
  @EJB
  private DelaTrackerCertController delaTrackerCertController;
  @EJB
  private ServiceCertificateRotationTimer serviceCertificateRotationTimer;
  
  @POST
  @Path("/rotate")
  @RolesAllowed({"AGENT"})
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response keyRotate(@Context HttpServletRequest request, String jsonString) throws HopsSecurityException {
    JSONObject json = new JSONObject(jsonString);
    String hostId = json.getString("host-id");
    CsrDTO responseDto;
    if(!json.has("csr")){
      throw new IllegalArgumentException("Requested to sign CSR but no CSR provided");
    }
    String csr = json.getString("csr");
    String commandId = "-1";
    if (json.has("id")) {
      commandId = json.getString("id");
    }
    responseDto = signCSR(hostId, commandId, csr, true, CertificateType.HOST);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(responseDto).build();
  }
  
  @POST
  @Path("/sign/app")
  @RolesAllowed({"AGENT"})
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response signCSR(@Context HttpServletRequest request, String jsonString) throws HopsSecurityException {
    JSONObject json = new JSONObject(jsonString);
    CsrDTO response = signCSR("-1", "-1", json.getString("csr"), false, APP);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(response).build();
  }

  private CsrDTO signCSR(String hostId, String commandId, String csr,
                         boolean rotation, CertificateType certType) throws HopsSecurityException {
    try {
      // If there is a certificate already for that host, rename it to .TO_BE_REVOKED.COMMAND_ID
      // When AgentResource has received a successful response for the key rotation, revoke and delete it
      if (rotation) {
        File certFile = Paths.get(settings.getIntermediateCaDir(), "certs", hostId
            + CertificatesMgmService.CERTIFICATE_SUFFIX).toFile();
        if (certFile.exists()) {
          File destination = Paths.get(settings.getIntermediateCaDir(), "certs",
              hostId + serviceCertificateRotationTimer.getToBeRevokedSuffix(commandId)).toFile();
          try {
            FileUtils.moveFile(certFile, destination);
          } catch (FileExistsException ex) {
            FileUtils.deleteQuietly(destination);
            FileUtils.moveFile(certFile, destination);
          }
        }
      }
      String agentCert = opensslOperations.signCertificateRequest(csr, certType);
      File caCertFile = Paths.get(settings.getIntermediateCaDir(), "certs", "ca-chain.cert.pem").toFile();
      String caCert = Files.toString(caCertFile, Charset.defaultCharset());
      return new CsrDTO(caCert, agentCert, settings.getHadoopVersionedDir());
    } catch (IOException ex) {
      throw new HopsSecurityException(RESTCodes.SecurityErrorCode.CSR_ERROR, Level.SEVERE, "host: " + hostId,
        ex.getMessage(), ex);
    }
  }
  
  @POST
  @Path("/revoke")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response revokeCertificate(
    @Context HttpServletRequest request, String jsonString) throws HopsSecurityException {
    JSONObject json = new JSONObject(jsonString);
    String certificateID = json.getString("identifier");

    try {
      opensslOperations.revokeCertificate(certificateID, APP, true, true);
    } catch (CAException cae) {
      if (cae.getErrorCode() == RESTCodes.CAErrorCode.CERTNOTFOUND) {
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NO_CONTENT).entity("Certificate " +
          certificateID + " does not exist").build();
      }
      throw new HopsSecurityException(RESTCodes.SecurityErrorCode.CSR_ERROR, Level.SEVERE);
    } catch (IOException e) {
      throw new HopsSecurityException(RESTCodes.SecurityErrorCode.CSR_ERROR, Level.SEVERE, null, e.getMessage(), e);
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity("Certificate " + certificateID +
        "revoked").build();
  }
  
  @POST
  @Path("/hopsworks")
  @AllowCORS
  @RolesAllowed({"AGENT", "CLUSTER_AGENT"})
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response hopsworks(@Context HttpServletRequest req,
                            @Context SecurityContext sc, String jsonString) throws GenericException {
    JSONObject json = new JSONObject(jsonString);
    String pubAgentCert = "no certificate";
    String caPubCert = "no certificate";
    String intermediateCaPubCert = "no certificate";
    if (json.has("csr")) {
      String csr = json.getString("csr");
      try {
        pubAgentCert = delaTrackerCertController.signCsr(sc.getUserPrincipal().getName(), csr);

        caPubCert = Files.toString(new File(settings.getCertsDir() + "/certs/ca.cert.pem"), Charsets.UTF_8);
        intermediateCaPubCert = Files.toString(
            new File(settings.getIntermediateCaDir() + "/certs/intermediate.cert.pem"), Charsets.UTF_8);

      } catch (IOException | DelaCSRCheckException ex) {
        throw new GenericException(RESTCodes.GenericErrorCode.UNKNOWN_ERROR, Level.SEVERE, null, ex.getMessage(), ex);
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
      String crl = opensslOperations.createAndReadCRL(PKI.CAType.INTERMEDIATE);
      FileUtils.writeStringToFile(certFile, crl);
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
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
}
