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
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.util.RESTApiJsonResponse;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.host.Hosts;
import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.util.Variables;
import io.hops.hopsworks.common.exception.EncryptionMasterPasswordException;
import io.hops.hopsworks.common.exception.HopsSecurityException;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.exception.ServiceException;
import io.hops.hopsworks.common.security.CertificatesMgmService;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.Api;

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
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
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
  private HostsFacade hostsFacade;
  @EJB
  private JWTHelper jWTHelper;
  
  /**
   * Admin endpoint that changes the master encryption password used to encrypt the certificates' password
   * stored in the database.
   * @param sc
   * @param oldPassword Current password
   * @param newPassword New password
   * @return
   * @throws io.hops.hopsworks.common.exception.HopsSecurityException
   */
  @PUT
  @Path("/encryptionPass")
  public Response changeMasterEncryptionPassword(@Context SecurityContext sc,
      @FormParam("oldPassword") String oldPassword, @FormParam("newPassword") String newPassword) throws
      HopsSecurityException {
    LOGGER.log(Level.FINE, "Requested master encryption password change");
    try {
      Users user = jWTHelper.getUserPrincipal(sc);
      certificatesMgmService.checkPassword(oldPassword, user.getEmail());
      certificatesMgmService.resetMasterEncryptionPassword(newPassword, user.getEmail());
  
      RESTApiJsonResponse response = noCacheResponse.buildJsonResponse(Response.Status.NO_CONTENT, ResponseMessages
          .MASTER_ENCRYPTION_PASSWORD_CHANGE);
  
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(response).build();
    } catch (EncryptionMasterPasswordException ex) {
      throw new HopsSecurityException(RESTCodes.SecurityErrorCode.CERT_ACCESS_DENIED, Level.SEVERE, null,
        ex.getMessage(), ex);
    } catch (IOException ex) {
      throw new HopsSecurityException(RESTCodes.SecurityErrorCode.MASTER_ENCRYPTION_PASSWORD_ACCESS_ERROR,
        Level.SEVERE, null, ex.getMessage(), ex);
    }
  }
  
  @POST
  @Path("/variables/refresh")
  public Response refreshVariables() {
    LOGGER.log(Level.FINE, "Requested refreshing variables");
    settings.refreshCache();
    
    RESTApiJsonResponse response = noCacheResponse.buildJsonResponse(Response.Status.NO_CONTENT, "Variables refreshed");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(response).build();
  }
  
  @POST
  @Consumes({MediaType.APPLICATION_JSON})
  @Path("/variables")
  public Response updateVariables(VariablesRequest variablesRequest) {
  
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
  
  @GET
  @Path("/hosts")
  public Response getAllClusterNodes() {
    List<Hosts> allNodes = hostsFacade.find();
    
    List<Hosts> responseList = new ArrayList<>(allNodes.size());
    // Send only hostID and hostname
    for (Hosts host : allNodes) {
      Hosts node = new Hosts();
      node.setHostname(host.getHostname());
      node.setHostIp(host.getHostIp());
      node.setRegistered(host.isRegistered());
      responseList.add(node);
    }
    
    GenericEntity<List<Hosts>> response = new GenericEntity<List<Hosts>>(responseList){};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(response).build();
  }
  
  @PUT
  @Consumes({MediaType.APPLICATION_JSON})
  @Path("/hosts")
  public Response updateClusterNode(Hosts nodeToUpdate) throws ServiceException {
  
    Hosts storedNode = hostsFacade.findByHostname(nodeToUpdate.getHostname());
    if (storedNode == null) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.HOST_NOT_FOUND, Level.WARNING);
    } else {
      if (nodeToUpdate.getHostIp() != null && !nodeToUpdate.getHostIp().isEmpty()) {
        storedNode.setHostIp(nodeToUpdate.getHostIp());
      }
    
      if (nodeToUpdate.getPublicIp() != null && !nodeToUpdate.getPublicIp().isEmpty()) {
        storedNode.setPublicIp(nodeToUpdate.getPublicIp());
      }
      
      if (nodeToUpdate.getPrivateIp() != null && !nodeToUpdate.getPrivateIp().isEmpty()) {
        storedNode.setPrivateIp(nodeToUpdate.getPrivateIp());
      }
      
      if (nodeToUpdate.getAgentPassword() != null && !nodeToUpdate.getAgentPassword().isEmpty()) {
        storedNode.setAgentPassword(nodeToUpdate.getAgentPassword());
      }

      if (nodeToUpdate.getCondaEnabled() != null) {
        storedNode.setCondaEnabled(nodeToUpdate.getCondaEnabled());
      }

      hostsFacade.storeHost(storedNode);
      RESTApiJsonResponse response = noCacheResponse.buildJsonResponse(Response.Status.NO_CONTENT, "Node updated");
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NO_CONTENT).entity(response).build();
    }
  }
  
  @DELETE
  @Path("/hosts/{hostid}")
  public Response deleteNode(@PathParam("hostid") String hostId) {
    if (hostId == null) {
      throw new IllegalArgumentException("hostId was not provided.");
    }
    boolean deleted = hostsFacade.removeByHostname(hostId);
    RESTApiJsonResponse response;
    if (deleted) {
      response = noCacheResponse.buildJsonResponse(Response.Status.OK, "Node with ID " + hostId + " deleted");
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(response).build();
    } else {
      response = noCacheResponse.buildJsonResponse(Response.Status.NOT_FOUND, "Could not delete node " + hostId);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_FOUND).entity(response).build();
    }
  }
  
  
  @POST
  @Consumes({MediaType.APPLICATION_JSON})
  @Path("/hosts")
  public Response addNewClusterNode(Hosts newNode)
    throws ServiceException {
    
    // Do some sanity check
    if (Strings.isNullOrEmpty(newNode.getHostname())) {
      throw new IllegalArgumentException("hostId or hostname of new node are empty");
    }
    
    Hosts existingNode = hostsFacade.findByHostname(newNode.getHostname());
    if (existingNode != null) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.HOST_EXISTS,  Level.WARNING, "Host with the same ID " +
        "already exist");
    }
    
    // Make sure we store what we want in the DB and not what the user wants to
    Hosts finalNode = new Hosts();
    finalNode.setHostname(newNode.getHostname());
    finalNode.setHostIp(newNode.getHostIp());
    hostsFacade.storeHost(finalNode);
  
    GenericEntity<Hosts> response = new GenericEntity<Hosts>(finalNode){};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.CREATED).entity(response).build();
  }
  
  @POST
  @Path("/rotate")
  public Response serviceKeyRotate() {
    certificatesMgmService.issueServiceKeyRotationCommand();
    RESTApiJsonResponse
      response = noCacheResponse.buildJsonResponse(Response.Status.NO_CONTENT, "Key rotation commands " +
        "issued");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NO_CONTENT).entity(response).build();
  }
}
