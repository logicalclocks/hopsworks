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

package io.hops.hopsworks.api.admin;

import com.google.common.base.Strings;
import io.hops.hopsworks.api.admin.dto.VariablesRequest;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.jwt.ElasticJWTResponseDTO;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.util.RESTApiJsonResponse;
import io.hops.hopsworks.common.agent.AgentLivenessMonitor;
import io.hops.hopsworks.common.dao.host.Hosts;
import io.hops.hopsworks.common.dao.kafka.TopicDefaultValueDTO;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.util.Variables;
import io.hops.hopsworks.common.hosts.HostsController;
import io.hops.hopsworks.common.kafka.KafkaController;
import io.hops.hopsworks.common.util.RemoteCommandResult;
import io.hops.hopsworks.common.security.ServiceJWTKeepAlive;
import io.hops.hopsworks.exceptions.ElasticException;
import io.hops.hopsworks.exceptions.EncryptionMasterPasswordException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.jwt.exception.JWTException;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.common.security.CertificatesMgmService;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.SecurityContext;

@Path("/admin")
@Stateless
@JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN"})
@Api(value = "Admin")
@Produces(MediaType.APPLICATION_JSON)
@TransactionAttribute(TransactionAttributeType.NEVER)
public class SystemAdminService {
  
  private static final Logger LOGGER = Logger.getLogger(SystemAdminService.class.getName());
  
  @EJB
  private CertificatesMgmService certificatesMgmService;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private Settings settings;
  @EJB
  private HostsController hostsController;
  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private AgentLivenessMonitor agentLivenessMonitor;
  @EJB
  private ServiceJWTKeepAlive serviceJWTKeepAlive;
  @EJB
  private KafkaController kafkaController;
  
  /**
   * Admin endpoint that changes the master encryption password used to encrypt the certificates' password
   * stored in the database.
   * @param sc
   * @param oldPassword Current password
   * @param newPassword New password
   * @return
   * @throws HopsSecurityException
   */
  @PUT
  @Path("/encryptionPass")
  public Response changeMasterEncryptionPassword(@Context SecurityContext sc,
    @FormParam("oldPassword") String oldPassword, @FormParam("newPassword") String newPassword)
    throws HopsSecurityException {
    LOGGER.log(Level.FINE, "Requested master encryption password change");
    try {
      Users user = jWTHelper.getUserPrincipal(sc);
      certificatesMgmService.checkPassword(oldPassword, user.getEmail());
      Integer operationId = certificatesMgmService.initUpdateOperation();
      certificatesMgmService.resetMasterEncryptionPassword(operationId, newPassword, user.getEmail());
      
      RESTApiJsonResponse response = noCacheResponse.buildJsonResponse(Response.Status.CREATED,
          String.valueOf(operationId));
      
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.CREATED).entity(response).build();
    } catch (EncryptionMasterPasswordException ex) {
      throw new HopsSecurityException(RESTCodes.SecurityErrorCode.CERT_ACCESS_DENIED, Level.SEVERE, null,
        ex.getMessage(), ex);
    } catch (IOException ex) {
      throw new HopsSecurityException(RESTCodes.SecurityErrorCode.MASTER_ENCRYPTION_PASSWORD_ACCESS_ERROR,
        Level.SEVERE, null, ex.getMessage(), ex);
    }
  }
  
  @GET
  @Path("/encryptionPass/{opId}")
  public Response getUpdatePasswordStatus(@PathParam("opId") Integer operationId, @Context SecurityContext sc) {
    CertificatesMgmService.UPDATE_STATUS status = certificatesMgmService.getOperationStatus(operationId);
    switch (status) {
      case OK:
        return noCacheResponse.getNoCacheCORSResponseBuilder(Response.Status.OK).build();
      case FAILED:
        return noCacheResponse.getNoCacheCORSResponseBuilder(Response.Status.INTERNAL_SERVER_ERROR).build();
      case WORKING:
        return noCacheResponse.getNoCacheCORSResponseBuilder(Response.Status.FOUND).build();
      default:
        return noCacheResponse.getNoCacheCORSResponseBuilder(Response.Status.NOT_FOUND).build();
    }
  }
  
  @POST
  @Path("/variables/refresh")
  public Response refreshVariables(@Context SecurityContext sc) {
    LOGGER.log(Level.FINE, "Requested refreshing variables");
    settings.refreshCache();
    
    RESTApiJsonResponse response = noCacheResponse.buildJsonResponse(Response.Status.NO_CONTENT, "Variables refreshed");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(response).build();
  }
  
  @POST
  @Consumes({MediaType.APPLICATION_JSON})
  @Path("/variables")
  public Response updateVariables(VariablesRequest variablesRequest, @Context SecurityContext sc) {
  
    List<Variables> variables = variablesRequest.getVariables();
    
    if (variables == null) {
      throw new IllegalArgumentException("variablesRequest was not provided or was incomplete.");
    }
    
    Map<String, String> updateVariablesMap = new HashMap<>(variablesRequest.getVariables().size());
    for (Variables var : variables) {
      updateVariablesMap.putIfAbsent(var.getId(), var.getValue());
    }
    
    settings.updateVariables(updateVariablesMap);
    
    RESTApiJsonResponse response = noCacheResponse.buildJsonResponse(Response.Status.NO_CONTENT, "Variables updated");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(response).build();
  }
  
  @POST
  @Path("/rotate")
  public Response serviceKeyRotate(@Context SecurityContext sc) {
    certificatesMgmService.issueServiceKeyRotationCommand();
    RESTApiJsonResponse
      response = noCacheResponse.buildJsonResponse(Response.Status.NO_CONTENT, "Key rotation commands " +
        "issued");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NO_CONTENT).entity(response).build();
  }
  
  @POST
  @Path("/kagent/{hostname}")
  public Response startAgent(@PathParam("hostname") String hostname, @Context SecurityContext sc)
    throws ServiceException {
    if (Strings.isNullOrEmpty(hostname)) {
      throw new IllegalArgumentException("Hostname should not be null or empty");
    }
    Hosts host = hostsController.findByHostname(hostname);
    RemoteCommandResult result = agentLivenessMonitor.start(host);
    
    if (result.getExitCode() == 0) {
      return Response.ok().build();
    }
    
    String responseMessage = "Exit code: " + result.getExitCode() + " Reason: " + result.getStdout();
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NO_CONTENT).entity(responseMessage).build();
  }
  
  @DELETE
  @Path("/kagent/{hostname}")
  public Response stopAgent(@PathParam("hostname") String hostname, @Context SecurityContext sc)
    throws ServiceException {
    if (Strings.isNullOrEmpty(hostname)) {
      throw new IllegalArgumentException("Hostname should not be null or empty");
    }
    Hosts host = hostsController.findByHostname(hostname);
    RemoteCommandResult result = agentLivenessMonitor.stop(host);
    
    if (result.getExitCode() == 0) {
      return Response.ok().build();
    }
  
    String responseMessage = "Exit code: " + result.getExitCode() + " Reason: " + result.getStdout();
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NO_CONTENT).entity(responseMessage).build();
  }
  
  @PUT
  @Path("/kagent/{hostname}")
  public Response restartAgent(@PathParam("hostname") String hostname, @Context SecurityContext sc)
    throws ServiceException {
    if (Strings.isNullOrEmpty(hostname)) {
      throw new IllegalArgumentException("Hostname should not be null or empty");
    }
    Hosts host = hostsController.findByHostname(hostname);
    RemoteCommandResult result = agentLivenessMonitor.restart(host);
    if (result.getExitCode() == 0) {
      return Response.ok().build();
    }
    String responseMessage = "Exit code: " + result.getExitCode() + " Reason: " + result.getStdout();
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NO_CONTENT).entity(responseMessage).build();
  }
  
  @PUT
  @Path("/servicetoken")
  public Response renewServiceJWT(@Context SecurityContext sc) throws JWTException {
    serviceJWTKeepAlive.forceRenewServiceToken();
    return Response.noContent().build();
  }
  
  @ApiOperation(value = "Get kafka system settings")
  @GET
  @Path("/kafka/settings")
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @Produces(MediaType.APPLICATION_JSON)
  public Response getKafkaSettings(@Context SecurityContext sc) throws KafkaException {
    TopicDefaultValueDTO values = kafkaController.topicDefaultValues();
    
    return Response.ok().entity(values).build();
  }
  
  @GET
  @Path("/elastic/admintoken")
  public Response getElasticAdminToken(@Context SecurityContext sc) throws ElasticException {
    ElasticJWTResponseDTO responseDTO = jWTHelper.createTokenForELKAsAdmin();
    return Response.ok().entity(responseDTO).build();
  }
}
