package io.hops.hopsworks.api.iot;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.iot.IotGatewayConfiguration;
import io.hops.hopsworks.common.dao.iot.IotGatewayFacade;
import io.hops.hopsworks.common.dao.iot.IotGatewayState;
import io.hops.hopsworks.common.dao.iot.IotGateways;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.kafka.KafkaController;
import io.hops.hopsworks.exceptions.GatewayException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.jwt.exception.DuplicateSigningKeyException;
import io.hops.hopsworks.jwt.exception.SigningKeyNotFoundException;
import io.swagger.annotations.ApiOperation;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.logging.Logger;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class IotGatewayResource {
  private static final Logger LOGGER = Logger.getLogger(IotGatewayResource.class.getName());
  
  @EJB
  private IotGatewayBuilder iotGatewayBuilder;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private IotGatewayFacade iotGatewayFacade;
  @EJB
  private IotGatewayController iotGatewayController;
  @EJB
  private KafkaController kafkaController;
  @EJB
  private JWTHelper jWTHelper;
  
  private Project project;
  
  public IotGatewayResource setProject(Integer projectId) {
    this.project = projectFacade.find(projectId);
    return this;
  }
  
  @PostConstruct
  public void init() {
  
  }
  
  @ApiOperation(value = "Get list of currently connected IoT Gateways")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getGateways(
    @Context UriInfo uriInfo
  ) {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.GATEWAYS);
    IotGatewayDTO dto = iotGatewayBuilder.buildGateway(uriInfo, resourceRequest, project);
    return Response.ok().entity(dto).build();
  }
  
  @ApiOperation(value = "Get info about a specific IoT Gateway")
  @GET
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getGatewayById(
    @Context UriInfo uriInfo,
    @PathParam("id") Integer id
  ) throws GatewayException, URISyntaxException, IOException {
    IotGatewayDetails gateway = iotGatewayController.getGateway(project, id);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.GATEWAYS);
    IotGatewayDetailsDTO dto = iotGatewayBuilder.buildGatewayDetails(uriInfo, resourceRequest, gateway);
    return Response.ok().entity(dto).build();
  }
  
  @ApiOperation(value = "Register an IoT Gateway")
  @PUT
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_GATEWAY"})
  public Response registerGateway(
    IotGatewayConfiguration config,
    @Context UriInfo uriInfo,
    @Context SecurityContext sc
  ) throws URISyntaxException, IOException, NoSuchAlgorithmException, SigningKeyNotFoundException,
    DuplicateSigningKeyException {
    if (config == null) {
      throw new IllegalArgumentException("Gateway configuration was not provided.");
    }
    IotGateways gateway = iotGatewayController.putGateway(project, config);
    Users user = jWTHelper.getUserPrincipal(sc);
    String jwt = jWTHelper.createToken(user, "hopsworks@logicalclocks.com", null);
    iotGatewayController.sendJwtToIotGateway(config, project.getId(), jwt);
    IotGatewayDTO dto =
      iotGatewayBuilder.buildGateway(uriInfo, new ResourceRequest(ResourceRequest.Name.GATEWAYS), gateway);
    return Response.created(dto.getHref()).build();
  }
  
  @ApiOperation(value = "Unregister IoT Gateway")
  @DELETE
  @Path("{id}")
  @Produces(MediaType.TEXT_PLAIN)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_GATEWAY"})
  public Response unregisterGateway(
    @PathParam("id") Integer gatewayId,
    @Context UriInfo uriInfo
  ) {
    IotGateways gateway = iotGatewayFacade.findByProjectAndId(project, gatewayId);
    if (gateway == null) {
      String message = "Gateway with id " + gatewayId + " not found.";
      return Response
        .status(Response.Status.NOT_FOUND)
        .entity(message)
        .type(MediaType.TEXT_PLAIN)
        .build();
    }
    
    if (gateway.getState() == IotGatewayState.ACTIVE) {
      iotGatewayFacade.removeIotGateway(gateway);
    } else if (gateway.getState() == IotGatewayState.BLOCKED) {
      iotGatewayFacade.updateState(gateway.getId(), IotGatewayState.INACTIVE_BLOCKED);
    }
    
    return Response.ok().build();
  }
  
  @ApiOperation(value = "Start blocking an IoT Gateway")
  @POST
  @Path("{id}/blocked")
  @AllowedProjectRoles({AllowedProjectRoles.ANYONE})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response startBlockingGateway(
    @PathParam("id")
      Integer gatewayId
  ) {
    IotGateways gateway = iotGatewayFacade.findByProjectAndId(project, gatewayId);
    if (gateway == null) {
      return Response.status(Response.Status.NOT_FOUND).entity("Gateway " + gatewayId + " not found.").build();
    } else if (gateway.getState() == IotGatewayState.ACTIVE) {
      kafkaController.startBlockingIotGateway(gateway, project);
      iotGatewayFacade.updateState(gatewayId, IotGatewayState.BLOCKED);
      return Response.accepted().build();
    }
    return Response.status(Response.Status.NO_CONTENT).build();
  }
  
  @ApiOperation(value = "Stop blocking an IoT Gateway")
  @DELETE
  @Path("{id}/blocked")
  @AllowedProjectRoles({AllowedProjectRoles.ANYONE})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response stopBlockingGateway(
    @PathParam("id") Integer gatewayId,
    @Context SecurityContext sc
  ) {
    IotGateways gateway = iotGatewayFacade.findByProjectAndId(project, gatewayId);
    Users user = jWTHelper.getUserPrincipal(sc);
    if (gateway == null) {
      return Response.status(Response.Status.NOT_FOUND).entity("Gateway " + gatewayId + " not found.").build();
    } else if (gateway.getState() == IotGatewayState.ACTIVE) {
      return Response.accepted().build();
    } else {
      kafkaController.removeBlockingIotGateway(gateway, project, user);
      if (gateway.getState() == IotGatewayState.INACTIVE_BLOCKED) {
        iotGatewayFacade.removeIotGateway(gateway);
      } else if (gateway.getState() == IotGatewayState.BLOCKED) {
        iotGatewayFacade.updateState(gatewayId, IotGatewayState.ACTIVE);
      }
      return Response.accepted().build();
    }
  }
  
  @ApiOperation(value = "Get list of all IoT Nodes connected to an IoT Gateway")
  @GET
  @Path("{id}/nodes")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getNodesOfGateway(
    @Context UriInfo uriInfo,
    @PathParam("id") Integer gatewayId
  ) throws URISyntaxException, IOException {
    List<IotDevice> devices = iotGatewayController.getNodesOfGateway(gatewayId, project);
    LOGGER.info("Connected " + devices.size() + " devices");
    IotDeviceDTO dto = iotGatewayBuilder.buildDevice(uriInfo, project, devices, gatewayId);
    return Response.ok().entity(dto).build();
  }
  
  @ApiOperation(value = "Get info about node by its ID")
  @GET
  @Path("{gId}/nodes/{nId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getNodeById (
    @Context UriInfo uriInfo,
    @PathParam("gId") Integer gatewayId,
    @PathParam("nId") String nodeId)
    throws URISyntaxException, IOException {
    IotDevice device = iotGatewayController.getNodeById(gatewayId, nodeId, project);
    if (device == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    } else {
      IotDeviceDTO dto = iotGatewayBuilder.buildDevice(uriInfo, device, project);
      return Response.ok().entity(dto).build();
    }
  }
  
  @ApiOperation(value = "start blocking a node")
  @POST
  @Path("{gId}/nodes/{nId}/blocked")
  @AllowedProjectRoles({AllowedProjectRoles.ANYONE})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response startBlockingNodeById (
    @PathParam("gId") Integer gatewayId,
    @PathParam("nId") String nodeId
  ) throws URISyntaxException, IOException {
    iotGatewayController.actionBlockingNode(gatewayId, nodeId, project, true);
    return Response.ok().build();
  }
  
  @ApiOperation(value = "stop blocking a node")
  @DELETE
  @Path("{gId}/nodes/{nId}/blocked")
  @AllowedProjectRoles({AllowedProjectRoles.ANYONE})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response stopBlockingNodeById (
    @PathParam("gId") Integer gatewayId,
    @PathParam("nId") String nodeId
  ) throws URISyntaxException, IOException {
    iotGatewayController.actionBlockingNode(gatewayId, nodeId, project, false);
    return Response.ok().build();
  }
}
