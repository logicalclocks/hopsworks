package io.hops.hopsworks.api.admin;

import io.hops.hopsworks.api.admin.dto.VariablesRequest;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.util.JsonResponse;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.common.dao.host.Hosts;
import io.hops.hopsworks.common.dao.util.Variables;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.exception.EncryptionMasterPasswordException;
import io.hops.hopsworks.common.security.CertificatesMgmService;
import io.hops.hopsworks.common.util.Settings;
import io.swagger.annotations.Api;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
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
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/admin")
@RolesAllowed({"HOPS_ADMIN"})
@Api(value = "Admin")
@Produces(MediaType.APPLICATION_JSON)
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class SystemAdminService {
  
  private final Logger LOG = Logger.getLogger(SystemAdminService.class.getName());
  
  @EJB
  private CertificatesMgmService certificatesMgmService;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private Settings settings;
  @EJB
  private HostsFacade hostsFacade;
  
  /**
   * Admin endpoint that changes the master encryption password used to encrypt the certificates' password
   * stored in the database.
   * @param sc
   * @param request
   * @param oldPassword Current password
   * @param newPassword New password
   * @return
   * @throws AppException
   */
  @PUT
  @Path("/encryptionPass")
  public Response changeMasterEncryptionPassword(@Context SecurityContext sc, @Context HttpServletRequest request,
      @FormParam("oldPassword") String oldPassword, @FormParam("newPassword") String newPassword)
    throws AppException {
    LOG.log(Level.FINE, "Requested master encryption password change");
    try {
      String userEmail = sc.getUserPrincipal().getName();
      certificatesMgmService.checkPassword(oldPassword, userEmail);
      certificatesMgmService.resetMasterEncryptionPassword(newPassword, userEmail);
  
      JsonResponse response = noCacheResponse.buildJsonResponse(Response.Status.NO_CONTENT, ResponseMessages
          .MASTER_ENCRYPTION_PASSWORD_CHANGE);
  
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(response).build();
    } catch (EncryptionMasterPasswordException ex) {
      throw new AppException(Response.Status.FORBIDDEN.getStatusCode(), ex.getMessage());
    } catch (IOException ex) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Error while reading master " +
          "password file: " + ex.getMessage());
    }
  }
  
  @POST
  @Path("/variables/refresh")
  public Response refreshVariables(@Context SecurityContext sc, @Context HttpServletRequest request)
    throws AppException {
    LOG.log(Level.FINE, "Requested refreshing variables");
    settings.refreshCache();
    
    JsonResponse response = noCacheResponse.buildJsonResponse(Response.Status.NO_CONTENT, "Variables refreshed");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(response).build();
  }
  
  @POST
  @Consumes({MediaType.APPLICATION_JSON})
  @Path("/variables")
  public Response updateVariables(@Context SecurityContext sc, @Context HttpServletRequest request,
      VariablesRequest variablesRequest)
    throws AppException {
  
    List<Variables> variables = variablesRequest.getVariables();
    
    if (variables == null) {
      LOG.log(Level.WARNING, "Malformed request to update variables");
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Malformed request");
    }
    
    Map<String, String> updateVariablesMap = new HashMap<>(variablesRequest.getVariables().size());
    for (Variables var : variables) {
      updateVariablesMap.putIfAbsent(var.getId(), var.getValue());
    }
    
    try {
      settings.updateVariables(updateVariablesMap);
    } catch (Exception ex) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), ex.getCause().getMessage());
    }
    
    JsonResponse response = noCacheResponse.buildJsonResponse(Response.Status.NO_CONTENT, "Variables updated");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(response).build();
  }
  
  @GET
  @Path("/hosts")
  public Response getAllClusterNodes(@Context SecurityContext sc, @Context HttpServletRequest request)
      throws AppException {
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
  public Response updateClusterNode(@Context SecurityContext sc, @Context HttpServletRequest request, Hosts
      nodeToUpdate) throws AppException {
  
    Hosts storedNode = hostsFacade.findByHostname(nodeToUpdate.getHostname());
    if (storedNode == null) {
      LOG.log(Level.WARNING, "Tried to update node that does not exist");
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Tried to update node that does not exist");
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
      
      hostsFacade.storeHost(storedNode, true);
      JsonResponse response = noCacheResponse.buildJsonResponse(Response.Status.NO_CONTENT, "Node updated");
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NO_CONTENT).entity(response).build();
    }
  }
  
  @DELETE
  @Path("/hosts/{hostid}")
  public Response deleteNode(@Context SecurityContext sc, @Context HttpServletRequest request,
      @PathParam("hostid") String hostId) throws AppException {
    if (hostId != null) {
      boolean deleted = hostsFacade.removeByHostname(hostId);
      JsonResponse response;
      if (deleted) {
        response = noCacheResponse.buildJsonResponse(Response.Status.OK, "Node with ID " + hostId + " deleted");
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(response).build();
      } else {
        response = noCacheResponse.buildJsonResponse(Response.Status.NOT_FOUND, "Could not delete node " + hostId);
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_FOUND).entity(response).build();
      }
    }
    
    throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Host ID cannot be null");
  }
  
  @POST
  @Consumes({MediaType.APPLICATION_JSON})
  @Path("/hosts")
  public Response addNewClusterNode(@Context SecurityContext sc, @Context HttpServletRequest request, Hosts newNode)
    throws AppException {
    
    // Do some sanity check
    if (newNode.getHostname() == null || newNode.getHostname().isEmpty()
        || newNode.getHostname() == null || newNode.getHostname().isEmpty()) {
      LOG.log(Level.WARNING, "hostId or hostname of new node are empty");
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "hostId or hostname of new node are empty");
    }
    
    Hosts existingNode = hostsFacade.findByHostname(newNode.getHostname());
    if (existingNode != null) {
      LOG.log(Level.WARNING, "Tried to add Host with ID " + newNode.getHostname() + " but a host already exist with " +
          "the same ID");
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Host with the same ID already exist");
    }
    
    // Make sure we store what we want in the DB and not what the user wants to
    Hosts finalNode = new Hosts();
    finalNode.setHostname(newNode.getHostname());
    finalNode.setHostIp(newNode.getHostIp());
    hostsFacade.storeHost(finalNode, true);
  
    GenericEntity<Hosts> response = new GenericEntity<Hosts>(finalNode){};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.CREATED).entity(response).build();
  }
}
